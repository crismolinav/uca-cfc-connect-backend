package sv.edu.udb.ucacfcconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.*;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;
import sv.edu.udb.ucacfcconnect.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class CotizacionService {
    private static final String PENDIENTE = "PENDIENTE";
    private static final Set<String> ESTADOS = Set.of(PENDIENTE, "EN_PROCESO", "APROBADA", "RECHAZADA");
    private static final Map<String, String> ORDEN = Map.of(
            "idCotizacion", "id", "fecha", "fecha", "estado", "estado",
            "montoEstimado", "montoEstimado", "cliente", "cliente.nombre"
    );

    private final CotizacionRepository cotizaciones;
    private final DetalleCotizacionRepository detalles;
    private final ClienteRepository clientes;
    private final CursoRepository cursos;
    private final EspacioRepository espacios;
    private final DiplomadoRepository diplomados;
    private final ServicioCateringRepository serviciosCatering;

    public CotizacionService(CotizacionRepository cotizaciones, DetalleCotizacionRepository detalles,
                             ClienteRepository clientes, CursoRepository cursos, EspacioRepository espacios,
                             DiplomadoRepository diplomados, ServicioCateringRepository serviciosCatering) {
        this.cotizaciones = cotizaciones;
        this.detalles = detalles;
        this.clientes = clientes;
        this.cursos = cursos;
        this.espacios = espacios;
        this.diplomados = diplomados;
        this.serviciosCatering = serviciosCatering;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CotizacionResponseDTO> listar(String texto, String estado, Long idCliente,
                                                    int pagina, int tamano, String ordenarPor,
                                                    String direccion) {
        String propiedad = ORDEN.get(ordenarPor);
        if (propiedad == null) throw new SolicitudInvalidaException("Campo de ordenamiento no permitido");
        String estadoNormalizado = estado == null || estado.isBlank() ? null : validarEstado(estado);
        Sort.Direction sentido;
        try {
            sentido = Sort.Direction.fromString(direccion);
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("La dirección debe ser 'asc' o 'desc'");
        }
        Page<CotizacionResponseDTO> paginaResultado = cotizaciones.buscar(
                normalizar(texto), estadoNormalizado, idCliente,
                PageRequest.of(pagina, tamano, Sort.by(sentido, propiedad))
        ).map(this::aRespuesta);
        return PaginaDTO.desde(paginaResultado);
    }

    @Transactional(readOnly = true)
    public List<CotizacionCatalogoItemDTO> listarCatalogos() {
        List<CotizacionCatalogoItemDTO> catalogo = new ArrayList<>();
        cursos.findAll(Sort.by("titulo")).stream()
                .filter(curso -> Boolean.TRUE.equals(curso.getActivo()))
                .map(curso -> new CotizacionCatalogoItemDTO(
                        curso.getIdCurso(), "CURSO", curso.getTitulo(), curso.getCosto()))
                .forEach(catalogo::add);
        diplomados.findAll(Sort.by("nombre")).stream()
                .filter(Diplomado::isActivo)
                .map(diplomado -> new CotizacionCatalogoItemDTO(
                        diplomado.getId(), "DIPLOMADO", diplomado.getNombre(), diplomado.getCosto()))
                .forEach(catalogo::add);
        espacios.findAll(Sort.by("nombre")).stream()
                .filter(Espacio::isDisponible)
                .map(espacio -> new CotizacionCatalogoItemDTO(
                        espacio.getId(), "ESPACIO", espacio.getNombre(), espacio.getPrecio()))
                .forEach(catalogo::add);
        serviciosCatering.findAll(Sort.by("nombre")).stream()
                .filter(ServicioCatering::isActivo)
                .map(servicio -> new CotizacionCatalogoItemDTO(
                        servicio.getId(), "CATERING", servicio.getNombre(), servicio.getPrecioBase()))
                .forEach(catalogo::add);
        return catalogo;
    }

    @Transactional(readOnly = true)
    public CotizacionResponseDTO obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public CotizacionResponseDTO crear(CotizacionDTO dto) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setFecha(LocalDate.now());
        cotizacion.setEstado(PENDIENTE);
        cotizacion.setCliente(buscarCliente(dto.idCliente()));
        cotizacion.setObservaciones(normalizar(dto.observaciones()));
        cotizaciones.save(cotizacion);
        cotizacion.setDetalles(crearDetalles(cotizacion, dto.detalles()));
        cotizacion.setMontoEstimado(sumar(cotizacion.getDetalles()));
        cotizaciones.save(cotizacion);
        return aRespuesta(cotizacion);
    }

    @Transactional
    public CotizacionResponseDTO actualizar(Long id, CotizacionDTO dto) {
        Cotizacion cotizacion = buscar(id);
        if (!PENDIENTE.equals(cotizacion.getEstado())) {
            throw new ConflictException("Solo se puede editar una cotización pendiente");
        }
        cotizacion.setCliente(buscarCliente(dto.idCliente()));
        cotizacion.setObservaciones(normalizar(dto.observaciones()));
        detalles.deleteByCotizacion_Id(id);
        detalles.flush();
        cotizacion.setDetalles(crearDetalles(cotizacion, dto.detalles()));
        cotizacion.setMontoEstimado(sumar(cotizacion.getDetalles()));
        return aRespuesta(cotizaciones.save(cotizacion));
    }

    @Transactional
    public CotizacionResponseDTO cambiarEstado(Long id, String estado) {
        Cotizacion cotizacion = buscar(id);
        String nuevoEstado = validarEstado(estado);
        if ("APROBADA".equals(nuevoEstado) && cotizacion.getDetalles().isEmpty()) {
            throw new ConflictException("No se puede aprobar una cotización sin servicios");
        }
        cotizacion.setEstado(nuevoEstado);
        return aRespuesta(cotizaciones.save(cotizacion));
    }

    @Transactional
    public void eliminar(Long id) {
        Cotizacion cotizacion = buscar(id);
        try {
            detalles.deleteByCotizacion_Id(id);
            cotizaciones.delete(cotizacion);
            cotizaciones.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se puede eliminar la cotización porque posee pagos relacionados");
        }
    }

    private List<DetalleCotizacion> crearDetalles(Cotizacion cotizacion, List<DetalleCotizacionDTO> solicitudes) {
        return solicitudes.stream().map(dto -> {
            DetalleCotizacion detalle = new DetalleCotizacion();
            detalle.setCotizacion(cotizacion);
            detalle.setCantidad(dto.cantidad());
            detalle.setDescripcion(normalizar(dto.descripcion()));
            if (dto.idCurso() != null) {
                Curso curso = cursos.findById(dto.idCurso())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Curso", dto.idCurso()));
                if (!Boolean.TRUE.equals(curso.getActivo())) {
                    throw new ConflictException("El curso seleccionado no está activo");
                }
                detalle.setCurso(curso);
                detalle.setPrecioUnitario(curso.getCosto());
            } else if (dto.idEspacio() != null) {
                Espacio espacio = espacios.findById(dto.idEspacio())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Espacio", dto.idEspacio()));
                if (!espacio.isDisponible()) throw new ConflictException("El espacio seleccionado no está disponible");
                detalle.setEspacio(espacio);
                detalle.setPrecioUnitario(espacio.getPrecio());
            } else if (dto.idDiplomado() != null) {
                Diplomado diplomado = diplomados.findById(dto.idDiplomado())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Diplomado", dto.idDiplomado()));
                if (!diplomado.isActivo()) {
                    throw new ConflictException("El diplomado seleccionado no está activo");
                }
                detalle.setDiplomado(diplomado);
                detalle.setPrecioUnitario(diplomado.getCosto());
            } else {
                ServicioCatering servicio = serviciosCatering.findById(dto.idServicioCatering())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Servicio de catering", dto.idServicioCatering()));
                if (!servicio.isActivo()) {
                    throw new ConflictException("El servicio de catering seleccionado no está activo");
                }
                detalle.setServicioCatering(servicio);
                detalle.setPrecioUnitario(servicio.getPrecioBase());
            }
            detalle.setSubtotal(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            return detalles.save(detalle);
        }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private Cotizacion buscar(Long id) {
        return cotizaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización", id));
    }

    private Cliente buscarCliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private String validarEstado(String estado) {
        String normalizado = estado.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!ESTADOS.contains(normalizado)) {
            throw new SolicitudInvalidaException("Estado inválido. Use: " + String.join(", ", ESTADOS));
        }
        return normalizado;
    }

    private BigDecimal sumar(List<DetalleCotizacion> items) {
        return items.stream().map(DetalleCotizacion::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private CotizacionResponseDTO aRespuesta(Cotizacion c) {
        return new CotizacionResponseDTO(c.getId(), c.getFecha(), c.getEstado(), c.getMontoEstimado(),
                c.getObservaciones(), c.getCliente().getId(), c.getCliente().getNombre(),
                c.getDetalles().stream().map(this::aRespuesta).toList());
    }

    private DetalleCotizacionResponseDTO aRespuesta(DetalleCotizacion d) {
        String tipo;
        Long idReferencia;
        String nombre;
        if (d.getCurso() != null) {
            tipo = "CURSO";
            idReferencia = d.getCurso().getIdCurso();
            nombre = d.getCurso().getTitulo();
        } else if (d.getDiplomado() != null) {
            tipo = "DIPLOMADO";
            idReferencia = d.getDiplomado().getId();
            nombre = d.getDiplomado().getNombre();
        } else if (d.getEspacio() != null) {
            tipo = "ESPACIO";
            idReferencia = d.getEspacio().getId();
            nombre = d.getEspacio().getNombre();
        } else if (d.getServicioCatering() != null) {
            tipo = "CATERING";
            idReferencia = d.getServicioCatering().getId();
            nombre = d.getServicioCatering().getNombre();
        } else {
            throw new SolicitudInvalidaException("El detalle de cotización no tiene un servicio asociado");
        }
        return new DetalleCotizacionResponseDTO(d.getId(), tipo, idReferencia, nombre,
                d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
