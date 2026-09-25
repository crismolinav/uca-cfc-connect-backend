package sv.edu.udb.ucacfcconnect.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ActividadRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.CateringRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.CotizacionRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.InscripcionRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.PagoRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ParticipanteRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ServicioRequest;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecepcionOperacionesService {
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private final ClienteRepository clientes;
    private final ParticipanteRepository participantes;
    private final InscripcionRepository inscripciones;
    private final CursoRepository cursos;
    private final DiplomadoRepository diplomados;
    private final ServicioCateringRepository servicios;
    private final SolicitudCateringRepository catering;
    private final CotizacionRepository cotizaciones;
    private final DetalleCotizacionRepository detalles;
    private final EspacioRepository espacios;
    private final AlquilerRepository alquileres;
    private final ActividadRepository actividades;
    private final PagoRepository pagos;

    public RecepcionOperacionesService(ClienteRepository clientes, ParticipanteRepository participantes,
                                        InscripcionRepository inscripciones, CursoRepository cursos,
                                        DiplomadoRepository diplomados, ServicioCateringRepository servicios,
                                        SolicitudCateringRepository catering, CotizacionRepository cotizaciones,
                                        DetalleCotizacionRepository detalles, EspacioRepository espacios,
                                        AlquilerRepository alquileres, ActividadRepository actividades,
                                        PagoRepository pagos) {
        this.clientes = clientes;
        this.participantes = participantes;
        this.inscripciones = inscripciones;
        this.cursos = cursos;
        this.diplomados = diplomados;
        this.servicios = servicios;
        this.catering = catering;
        this.cotizaciones = cotizaciones;
        this.detalles = detalles;
        this.espacios = espacios;
        this.alquileres = alquileres;
        this.actividades = actividades;
        this.pagos = pagos;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> catalogos() {
        return item("clientes", clientes.findAll(Sort.by("nombre")).stream()
                        .map(c -> item("id", c.getId(), "nombre", c.getNombre(), "duiNit", c.getDuiNit())).toList(),
                "participantes", participantes.findAll(Sort.by("apellido", "nombre")).stream()
                        .map(this::participanteView).toList(),
                "cursos", cursos.findAll(Sort.by("titulo")).stream().filter(c -> Boolean.TRUE.equals(c.getActivo()))
                        .map(c -> item("id", c.getIdCurso(), "nombre", c.getTitulo(), "precio", c.getCosto())).toList(),
                "diplomados", diplomados.findAll(Sort.by("nombre")).stream().filter(Diplomado::isActivo)
                        .map(d -> item("id", d.getId(), "nombre", d.getNombre(), "precio", d.getCosto())).toList(),
                "servicios", servicios.findAll(Sort.by("nombre")).stream().map(this::servicioView).toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> participantes() {
        return participantes.findAll(Sort.by("apellido", "nombre")).stream().map(this::participanteView).toList();
    }

    @Transactional
    public Map<String, Object> registrarParticipante(ParticipanteRequest request) {
        Participante participante = new Participante();
        participante.setCliente(cliente(request.clienteId()));
        participante.setNombre(request.nombre().trim());
        participante.setApellido(request.apellido().trim());
        participante.setCorreo(opcional(request.correo()));
        participante.setTelefono(opcional(request.telefono()));
        return participanteView(participantes.save(participante));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> inscripciones() {
        return inscripciones.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(this::inscripcionView).toList();
    }

    @Transactional
    public Map<String, Object> inscribir(InscripcionRequest request) {
        Participante participante = participantes.findById(request.participanteId())
                .orElseThrow(() -> notFound("participante"));
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setParticipante(participante);
        if ("CURSO".equals(request.programaTipo())) {
            // Serializa inscripciones al mismo curso antes de comprobar el cupo.
            Curso curso = cursos.bloquear(request.programaId()).orElseThrow(() -> notFound("curso"));
            if (!Boolean.TRUE.equals(curso.getActivo())) throw conflicto("El curso no está activo");
            if (inscripciones.countByCursoIdCursoAndEstadoNot(curso.getIdCurso(), "CANCELADA")
                    >= curso.getCupoMaximo()) {
                throw conflicto("El curso ya alcanzó su cupo máximo");
            }
            if (inscripciones.existsByParticipanteIdAndCursoIdCurso(participante.getId(), curso.getIdCurso())) {
                throw conflicto("El participante ya está inscrito en ese curso");
            }
            inscripcion.setCurso(curso);
            inscripcion.setTotal(curso.getCosto());
        } else if ("DIPLOMADO".equals(request.programaTipo())) {
            Diplomado diplomado = diplomados.findById(request.programaId())
                    .orElseThrow(() -> notFound("diplomado"));
            if (!diplomado.isActivo()) throw conflicto("El diplomado no está activo");
            if (inscripciones.existsByParticipanteIdAndDiplomadoId(participante.getId(), diplomado.getId())) {
                throw conflicto("El participante ya está inscrito en ese diplomado");
            }
            inscripcion.setDiplomado(diplomado);
            inscripcion.setTotal(diplomado.getCosto());
        } else {
            throw invalido("Selecciona curso o diplomado");
        }
        inscripcion.setFecha(LocalDate.now(ZONA));
        inscripcion.setEstado("PENDIENTE");
        return inscripcionView(inscripciones.save(inscripcion));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> servicios() {
        return servicios.findAll(Sort.by("nombre")).stream().map(this::servicioView).toList();
    }

    @Transactional
    public Map<String, Object> registrarServicio(ServicioRequest request) {
        validarDinero(request.precioBase(), false);
        ServicioCatering servicio = new ServicioCatering();
        servicio.setNombre(request.nombre().trim());
        servicio.setTipoServicio(request.tipoServicio().trim());
        servicio.setPrecioBase(request.precioBase());
        servicio.setDescripcion(opcional(request.descripcion()));
        servicio.setActivo(true);
        return servicioView(servicios.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> solicitudesCatering() {
        return catering.findAll(Sort.by(Sort.Direction.DESC, "fecha")).stream().map(this::cateringView).toList();
    }

    @Transactional
    public Map<String, Object> solicitarCatering(CateringRequest request) {
        ServicioCatering servicio = servicios.findById(request.servicioId())
                .orElseThrow(() -> notFound("servicio de catering"));
        if (!servicio.isActivo()) throw conflicto("El servicio de catering está inactivo");
        if (request.fecha().equals(LocalDate.now(ZONA)) && !request.hora().isAfter(LocalTime.now(ZONA))) {
            throw invalido("La hora del servicio debe ser futura");
        }
        SolicitudCatering solicitud = new SolicitudCatering();
        solicitud.setCliente(cliente(request.clienteId()));
        solicitud.setServicioCatering(servicio);
        solicitud.setFecha(request.fecha());
        solicitud.setHora(request.hora());
        solicitud.setLugar(request.lugar().trim());
        solicitud.setNumeroAsistentes(request.numeroAsistentes());
        solicitud.setMenu(request.menu().trim());
        solicitud.setEstado("PENDIENTE");
        return cateringView(catering.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> cotizaciones() {
        return cotizaciones.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(this::cotizacionView).toList();
    }

    @Transactional
    public Map<String, Object> cotizar(CotizacionRequest request) {
        Cliente cliente = cliente(request.clienteId());
        DetalleCotizacion detalle = new DetalleCotizacion();
        BigDecimal precio;
        String descripcion;
        switch (request.conceptoTipo()) {
            case "ESPACIO" -> {
                Espacio espacio = espacios.findById(request.conceptoId()).orElseThrow(() -> notFound("espacio"));
                detalle.setEspacio(espacio);
                precio = espacio.getPrecio();
                descripcion = espacio.getNombre();
            }
            case "CATERING" -> {
                ServicioCatering servicio = servicios.findById(request.conceptoId())
                        .orElseThrow(() -> notFound("servicio de catering"));
                detalle.setServicioCatering(servicio);
                precio = servicio.getPrecioBase();
                descripcion = servicio.getNombre();
            }
            case "CURSO" -> {
                Curso curso = cursos.findById(request.conceptoId()).orElseThrow(() -> notFound("curso"));
                detalle.setCurso(curso);
                precio = curso.getCosto();
                descripcion = curso.getTitulo();
            }
            case "DIPLOMADO" -> {
                Diplomado diplomado = diplomados.findById(request.conceptoId())
                        .orElseThrow(() -> notFound("diplomado"));
                detalle.setDiplomado(diplomado);
                precio = diplomado.getCosto();
                descripcion = diplomado.getNombre();
            }
            default -> throw invalido("Selecciona un concepto válido para la cotización");
        }
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setFecha(LocalDate.now(ZONA));
        cotizacion.setEstado("PENDIENTE");
        cotizacion.setObservaciones(opcional(request.observaciones()));
        detalle.setDescripcion(descripcion);
        detalle.setCantidad(request.cantidad());
        detalle.setPrecioUnitario(precio);
        BigDecimal total = precio.multiply(BigDecimal.valueOf(request.cantidad()));
        validarDinero(total, false);
        detalle.setSubtotal(total);
        cotizacion.setMontoEstimado(total);
        cotizaciones.save(cotizacion);
        detalle.setCotizacion(cotizacion);
        detalles.save(detalle);
        return cotizacionView(cotizacion);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> actividades() {
        return actividades.findAll(Sort.by(Sort.Direction.DESC, "fecha")).stream().map(this::actividadView).toList();
    }

    @Transactional
    public Map<String, Object> programarActividad(ActividadRequest request) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw invalido("La hora final debe ser posterior a la inicial");
        }
        Actividad actividad = new Actividad();
        actividad.setTitulo(request.titulo().trim());
        actividad.setFecha(request.fecha());
        actividad.setHoraInicio(request.horaInicio());
        actividad.setHoraFin(request.horaFin());
        actividad.setCupo(request.cupo());
        switch (request.vinculacionTipo()) {
            case "CURSO" -> actividad.setCurso(cursos.findById(request.vinculacionId())
                    .orElseThrow(() -> notFound("curso")));
            case "DIPLOMADO" -> actividad.setDiplomado(diplomados.findById(request.vinculacionId())
                    .orElseThrow(() -> notFound("diplomado")));
            case "ALQUILER" -> {
                Alquiler alquiler = alquileres.findById(request.vinculacionId())
                        .orElseThrow(() -> notFound("reserva"));
                if ("CANCELADO".equals(alquiler.getEstado()) || !alquiler.getFecha().equals(request.fecha())
                        || request.horaInicio().isBefore(alquiler.getHoraInicio())
                        || request.horaFin().isAfter(alquiler.getHoraFin())) {
                    throw invalido("La actividad debe quedar dentro del horario de una reserva activa");
                }
                actividad.setAlquiler(alquiler);
            }
            case "CATERING" -> {
                SolicitudCatering solicitud = catering.findById(request.vinculacionId())
                        .orElseThrow(() -> notFound("solicitud de catering"));
                if (!solicitud.getFecha().equals(request.fecha())) {
                    throw invalido("La actividad debe ser el mismo día de la solicitud de catering");
                }
                actividad.setSolicitudCatering(solicitud);
            }
            default -> throw invalido("Selecciona una vinculación válida para la actividad");
        }
        actividad.setTipo(request.vinculacionTipo());
        return actividadView(actividades.save(actividad));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pagos() {
        return pagos.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(this::pagoView).toList();
    }

    @Transactional
    public Map<String, Object> registrarPago(PagoRequest request) {
        validarDinero(request.monto(), true);
        if (!List.of("EFECTIVO", "TRANSFERENCIA", "TARJETA").contains(request.metodo())) {
            throw invalido("Selecciona un método de pago válido");
        }
        Cliente cliente = cliente(request.clienteId());
        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setMonto(request.monto());
        pago.setMetodo(request.metodo());
        pago.setReferencia(opcional(request.referencia()));
        pago.setFecha(LocalDate.now(ZONA));
        pago.setEstado("PENDIENTE");
        Long id = request.conceptoId();
        switch (request.conceptoTipo()) {
            case "INSCRIPCION" -> {
                Inscripcion inscripcion = inscripciones.findById(id).orElseThrow(() -> notFound("inscripción"));
                verificarCliente(cliente, inscripcion.getParticipante().getCliente().getId());
                pago.setInscripcion(inscripcion);
            }
            case "ALQUILER" -> {
                Alquiler alquiler = alquileres.findById(id).orElseThrow(() -> notFound("reserva"));
                verificarCliente(cliente, alquiler.getCliente().getId());
                if ("CANCELADO".equals(alquiler.getEstado())) throw conflicto("La reserva está cancelada");
                pago.setAlquiler(alquiler);
            }
            case "CATERING" -> {
                SolicitudCatering solicitud = catering.findById(id)
                        .orElseThrow(() -> notFound("solicitud de catering"));
                verificarCliente(cliente, solicitud.getCliente().getId());
                pago.setSolicitudCatering(solicitud);
            }
            case "COTIZACION" -> {
                Cotizacion cotizacion = cotizaciones.findById(id).orElseThrow(() -> notFound("cotización"));
                verificarCliente(cliente, cotizacion.getCliente().getId());
                pago.setCotizacion(cotizacion);
            }
            default -> throw invalido("Selecciona un concepto válido para el pago");
        }
        return pagoView(pagos.save(pago));
    }

    @Transactional
    public Map<String, Object> confirmarPago(Long id) {
        Pago pago = pagos.findById(id).orElseThrow(() -> notFound("pago"));
        if (!"PENDIENTE".equals(pago.getEstado())) throw conflicto("El pago ya fue confirmado");
        pago.setEstado("CONFIRMADO");
        if (pago.getInscripcion() != null) pago.getInscripcion().setEstado("CONFIRMADA");
        return pagoView(pago);
    }

    private void verificarCliente(Cliente cliente, Long propietarioId) {
        if (!cliente.getId().equals(propietarioId)) {
            throw invalido("El concepto del pago no pertenece al cliente seleccionado");
        }
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> notFound("cliente"));
    }

    private void validarDinero(BigDecimal valor, boolean mayorQueCero) {
        if (valor == null || (mayorQueCero ? valor.signum() <= 0 : valor.signum() < 0)
                || valor.scale() > 2 || valor.precision() - valor.scale() > 8) {
            throw invalido("El monto debe ser válido y tener hasta dos decimales");
        }
    }

    private String opcional(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private ApiException notFound(String recurso) { return new ApiException(HttpStatus.NOT_FOUND, "No se encontró el " + recurso); }
    private ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
    private ApiException invalido(String mensaje) { return new ApiException(HttpStatus.BAD_REQUEST, mensaje); }

    private Map<String, Object> participanteView(Participante p) {
        return item("id", p.getId(), "nombre", p.getNombre(), "apellido", p.getApellido(), "correo", p.getCorreo(),
                "telefono", p.getTelefono(), "clienteId", p.getCliente().getId(), "cliente", p.getCliente().getNombre());
    }
    private Map<String, Object> inscripcionView(Inscripcion i) {
        return item("id", i.getId(), "participanteId", i.getParticipante().getId(),
                "participante", i.getParticipante().getNombre() + " " + i.getParticipante().getApellido(),
                "clienteId", i.getParticipante().getCliente().getId(),
                "programa", i.getCurso() != null ? i.getCurso().getTitulo() : i.getDiplomado().getNombre(),
                "total", i.getTotal(), "fecha", i.getFecha(), "estado", i.getEstado());
    }
    private Map<String, Object> servicioView(ServicioCatering s) {
        return item("id", s.getId(), "nombre", s.getNombre(), "tipoServicio", s.getTipoServicio(),
                "precioBase", s.getPrecioBase(), "descripcion", s.getDescripcion(), "activo", s.isActivo());
    }
    private Map<String, Object> cateringView(SolicitudCatering s) {
        return item("id", s.getId(), "clienteId", s.getCliente().getId(), "cliente", s.getCliente().getNombre(),
                "servicio", s.getServicioCatering().getNombre(), "servicioId", s.getServicioCatering().getId(),
                "fecha", s.getFecha(), "hora", s.getHora(), "lugar", s.getLugar(),
                "numeroAsistentes", s.getNumeroAsistentes(), "menu", s.getMenu(), "estado", s.getEstado());
    }
    private Map<String, Object> cotizacionView(Cotizacion c) {
        return item("id", c.getId(), "clienteId", c.getCliente().getId(), "cliente", c.getCliente().getNombre(),
                "fecha", c.getFecha(), "montoEstimado", c.getMontoEstimado(), "estado", c.getEstado(),
                "observaciones", c.getObservaciones());
    }
    private Map<String, Object> actividadView(Actividad a) {
        return item("id", a.getId(), "titulo", a.getTitulo(), "tipo", a.getTipo(), "fecha", a.getFecha(),
                "horaInicio", a.getHoraInicio(), "horaFin", a.getHoraFin(), "cupo", a.getCupo());
    }
    private Map<String, Object> pagoView(Pago p) {
        return item("id", p.getId(), "clienteId", p.getCliente().getId(), "cliente", p.getCliente().getNombre(),
                "monto", p.getMonto(), "fecha", p.getFecha(), "metodo", p.getMetodo(),
                "referencia", p.getReferencia(), "estado", p.getEstado());
    }

    private static Map<String, Object> item(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }
}
