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

    public CotizacionService(CotizacionRepository cotizaciones, DetalleCotizacionRepository detalles,
                             ClienteRepository clientes, CursoRepository cursos, EspacioRepository espacios) {
        this.cotizaciones = cotizaciones;
        this.detalles = detalles;
        this.clientes = clientes;
        this.cursos = cursos;
        this.espacios = espacios;
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
        ).map(this::aRespuestaResumen);
        return PaginaDTO.desde(paginaResultado);
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
        cotizacion.setEstado(validarEstado(estado));
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
            } else {
                Espacio espacio = espacios.findById(dto.idEspacio())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Espacio", dto.idEspacio()));
                if (!espacio.isDisponible()) throw new ConflictException("El espacio seleccionado no está disponible");
                detalle.setEspacio(espacio);
                detalle.setPrecioUnitario(espacio.getPrecio());
            }
            detalle.setSubtotal(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            return detalles.save(detalle);
        }).toList();
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

    private CotizacionResponseDTO aRespuestaResumen(Cotizacion c) {
        return new CotizacionResponseDTO(c.getId(), c.getFecha(), c.getEstado(), c.getMontoEstimado(),
                c.getObservaciones(), c.getCliente().getId(), c.getCliente().getNombre(), List.of());
    }

    private CotizacionResponseDTO aRespuesta(Cotizacion c) {
        return new CotizacionResponseDTO(c.getId(), c.getFecha(), c.getEstado(), c.getMontoEstimado(),
                c.getObservaciones(), c.getCliente().getId(), c.getCliente().getNombre(),
                c.getDetalles().stream().map(this::aRespuesta).toList());
    }

    private DetalleCotizacionResponseDTO aRespuesta(DetalleCotizacion d) {
        boolean curso = d.getCurso() != null;
        return new DetalleCotizacionResponseDTO(d.getId(), curso ? "CURSO" : "ESPACIO",
                curso ? d.getCurso().getIdCurso() : d.getEspacio().getId(),
                curso ? d.getCurso().getTitulo() : d.getEspacio().getNombre(), d.getDescripcion(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
