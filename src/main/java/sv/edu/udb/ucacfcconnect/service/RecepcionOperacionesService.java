package sv.edu.udb.ucacfcconnect.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
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
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteResumen;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

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
    private final ParticipanteRegistroService registroParticipantes;

    public RecepcionOperacionesService(ClienteRepository clientes, ParticipanteRepository participantes,
                                        InscripcionRepository inscripciones, CursoRepository cursos,
                                        DiplomadoRepository diplomados, ServicioCateringRepository servicios,
                                        SolicitudCateringRepository catering, CotizacionRepository cotizaciones,
                                        DetalleCotizacionRepository detalles, EspacioRepository espacios,
                                        AlquilerRepository alquileres, ActividadRepository actividades,
                                        PagoRepository pagos, ParticipanteRegistroService registroParticipantes) {
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
        this.registroParticipantes = registroParticipantes;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> catalogos() {
        return item("clientes", clientes.findAll(Sort.by("nombre")).stream()
                        .map(ClienteResumen::from).toList(),
                "participantes", participantes.findAll(Sort.by("apellido", "nombre")).stream()
                        .map(this::participanteView).toList(),
                "participantesInscribibles", participantes.findAll(Sort.by("apellido", "nombre")).stream()
                        .filter(this::puedeInscribirse).map(this::participanteView).toList(),
                "cursos", cursos.findAll(Sort.by("titulo")).stream().filter(c -> Boolean.TRUE.equals(c.getActivo()))
                        .map(c -> item("id", c.getIdCurso(), "nombre", c.getTitulo(), "precio", c.getCosto(),
                                "fechaInicio", c.getFechaInicio(), "fechaFin", c.getFechaFin(),
                                "horario", c.getHorario())).toList(),
                "diplomados", diplomados.findAll(Sort.by("nombre")).stream().filter(Diplomado::isActivo)
                        .map(d -> item("id", d.getId(), "nombre", d.getNombre(), "precio", d.getCosto(),
                                "fechaInicio", d.getFechaInicio(), "fechaFin", d.getFechaFin(),
                                "sesiones", actividades.findByDiplomado_IdOrderByFechaAscHoraInicioAsc(d.getId())
                                        .stream().map(this::actividadView).toList())).toList(),
                "servicios", servicios.findAll(Sort.by("nombre")).stream().filter(ServicioCatering::isActivo)
                        .map(this::servicioView).toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> participantes() {
        return participantes.findAll(Sort.by("apellido", "nombre")).stream().map(this::participanteView).toList();
    }

    @Transactional
    public Map<String, Object> registrarParticipante(ParticipanteRequest request) {
        return participanteView(registroParticipantes.asegurar(clientePorCarnet(request.carnet())));
    }

    private Cliente clientePorCarnet(String carnet) {
        return clientes.findByCarnetIgnoreCase(carnet.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe un cliente con ese carnet"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buscarCarnet(String carnet) {
        Cliente cliente = clientePorCarnet(carnet);
        Participante participante = participantes.findFirstByClienteIdOrderByIdAsc(cliente.getId()).orElse(null);
        return item("clienteId", cliente.getId(), "carnet", cliente.getCarnet(), "nombre", cliente.getNombre(),
                "correo", cliente.getCorreo(), "esParticipante", participante != null,
                "puedeInscribirse", participante != null && puedeInscribirse(participante));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> inscripciones() {
        return inscripciones.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(this::inscripcionView).toList();
    }

    @Transactional
    public Map<String, Object> inscribir(InscripcionRequest request) {
        Cliente encontrado = clientePorCarnet(request.carnet());
        Cliente cliente = clientes.bloquear(encontrado.getId()).orElseThrow(() -> notFound("cliente"));
        Participante participante = participantes.findFirstByClienteIdOrderByIdAsc(cliente.getId())
                .orElseThrow(() -> conflicto("El carnet existe, pero el cliente aún no es participante. Regístralo en Participantes"));
        if (!puedeInscribirse(participante)) {
            throw conflicto("La cuenta del cliente está inactiva. Solicita su reactivación antes de inscribirlo");
        }
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

    @Transactional(readOnly = true)
    public Map<String, Object> cotizacion(Long id) {
        return cotizacionView(cotizaciones.buscarPorId(id).orElseThrow(() -> notFound("cotización")));
    }

    @Transactional
    public Map<String, Object> cotizar(CotizacionRequest request) {
        Cliente cliente = cliente(request.clienteId());
        DetalleCotizacion detalle = new DetalleCotizacion();
        BigDecimal precio;
        String descripcion;
        boolean programacionPropia = true;
        switch (request.conceptoTipo()) {
            case "ESPACIO" -> {
                Espacio espacio = espacios.findById(request.conceptoId()).orElseThrow(() -> notFound("espacio"));
                if (!espacio.isDisponible()) throw conflicto("El espacio seleccionado no está disponible");
                detalle.setEspacio(espacio);
                precio = espacio.getPrecio();
                descripcion = espacio.getNombre();
            }
            case "CATERING" -> {
                ServicioCatering servicio = servicios.findById(request.conceptoId())
                        .orElseThrow(() -> notFound("servicio de catering"));
                if (!servicio.isActivo()) throw conflicto("El servicio de catering está inactivo");
                detalle.setServicioCatering(servicio);
                precio = servicio.getPrecioBase();
                descripcion = servicio.getNombre();
            }
            case "CURSO" -> {
                Curso curso = cursos.findById(request.conceptoId()).orElseThrow(() -> notFound("curso"));
                if (!Boolean.TRUE.equals(curso.getActivo())) throw conflicto("El curso no está activo");
                detalle.setCurso(curso);
                precio = curso.getCosto();
                descripcion = programacionCurso(curso);
                programacionPropia = false;
            }
            case "DIPLOMADO" -> {
                Diplomado diplomado = diplomados.findById(request.conceptoId())
                        .orElseThrow(() -> notFound("diplomado"));
                if (!diplomado.isActivo()) throw conflicto("El diplomado no está activo");
                detalle.setDiplomado(diplomado);
                precio = diplomado.getCosto();
                descripcion = programacionDiplomado(diplomado);
                programacionPropia = false;
            }
            default -> throw invalido("Selecciona un concepto válido para la cotización");
        }
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setFecha(LocalDate.now(ZONA));
        cotizacion.setEstado("PENDIENTE");
        cotizacion.setObservaciones(opcional(request.observaciones()));
        detalle.setDescripcion(programacionPropia ? descripcionCotizacion(request) : descripcion);
        detalle.setCantidad(request.cantidad());
        detalle.setPrecioUnitario(precio);
        BigDecimal total = precio.multiply(BigDecimal.valueOf(request.cantidad()));
        validarDinero(total, false);
        detalle.setSubtotal(total);
        cotizacion.setMontoEstimado(total);
        cotizaciones.save(cotizacion);
        detalle.setCotizacion(cotizacion);
        detalles.save(detalle);
        cotizacion.getDetalles().add(detalle);
        return cotizacionView(cotizacion);
    }

    private String descripcionCotizacion(CotizacionRequest request) {
        boolean tieneFecha = request.fechaServicio() != null;
        boolean tieneInicio = request.horaInicio() != null;
        boolean tieneFin = request.horaFin() != null;
        if (!tieneFecha && !tieneInicio && !tieneFin) {
            throw invalido("Indica la fecha, la hora de inicio y la hora de fin del servicio");
        }
        if (!tieneFecha || !tieneInicio || !tieneFin) {
            throw invalido("Indica la fecha, la hora de inicio y la hora de fin del servicio");
        }
        if (request.fechaServicio().isBefore(LocalDate.now(ZONA))) {
            throw invalido("La fecha del servicio debe ser hoy o una fecha futura");
        }
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw invalido("La hora final del servicio debe ser posterior a la inicial");
        }
        return "Fecha: " + request.fechaServicio() + " · Horario: "
                + request.horaInicio() + "–" + request.horaFin();
    }

    private String programacionCurso(Curso curso) {
        String fechas = curso.getFechaInicio() == null ? "Fechas por confirmar" : "Del " + curso.getFechaInicio()
                + (curso.getFechaFin() == null ? "" : " al " + curso.getFechaFin());
        return fechas + " · Horario: " + (curso.getHorario() == null || curso.getHorario().isBlank()
                ? "por confirmar" : curso.getHorario());
    }

    private String programacionDiplomado(Diplomado diplomado) {
        List<Actividad> sesiones = actividades.findByDiplomado_IdOrderByFechaAscHoraInicioAsc(diplomado.getId());
        if (!sesiones.isEmpty()) {
            return "Sesiones: " + sesiones.stream().map(s -> s.getFecha() + " " + s.getHoraInicio()
                    + "–" + s.getHoraFin()).reduce((a, b) -> a + "; " + b).orElse("");
        }
        if (diplomado.getFechaInicio() == null) return "Programación por confirmar";
        return "Del " + diplomado.getFechaInicio()
                + (diplomado.getFechaFin() == null ? "" : " al " + diplomado.getFechaFin())
                + " · Sesiones por confirmar";
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
        return pagos.pendientesRecepcion().stream().map(this::pagoView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> historialPagos(LocalDate desde, LocalDate hasta, String estado,
                                               String metodo, Long clienteId, String q, int page, int size) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw invalido("La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (page < 0 || size < 1 || size > 100 || (clienteId != null && clienteId < 1)
                || (q != null && q.length() > 120)) {
            throw invalido("Los filtros del historial no son válidos");
        }
        estado = opcional(estado);
        metodo = opcional(metodo);
        if (estado != null) estado = estado.toUpperCase(Locale.ROOT);
        if (metodo != null) metodo = metodo.toUpperCase(Locale.ROOT);
        if (estado != null && !List.of("PENDIENTE", "CONFIRMADO", "RECHAZADO", "CANCELADO").contains(estado)) {
            throw invalido("Selecciona un estado de pago válido");
        }
        if (metodo != null && !List.of("EFECTIVO", "TRANSFERENCIA", "TARJETA").contains(metodo)) {
            throw invalido("Selecciona un método de pago válido");
        }
        var resultado = pagos.historialRecepcion(desde, hasta, estado, metodo, clienteId,
                q == null ? "" : q.trim(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha", "id")));
        return item("content", resultado.getContent().stream().map(this::pagoView).toList(),
                "totalElements", resultado.getTotalElements(), "totalPages", resultado.getTotalPages(),
                "number", resultado.getNumber(), "size", resultado.getSize());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> inscripcionesPendientesPago(Long clienteId) {
        return inscripciones.pendientesDePago(clienteId).stream().map(this::inscripcionView).toList();
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
                Inscripcion inscripcion = inscripciones.bloquear(id).orElseThrow(() -> notFound("inscripción"));
                verificarCliente(cliente, inscripcion.getParticipante().getCliente().getId());
                if (!"PENDIENTE".equals(inscripcion.getEstado())) {
                    throw conflicto("Solo se puede registrar el pago de una inscripción pendiente");
                }
                if (pagos.existsByInscripcionIdAndEstadoIn(id, List.of("PENDIENTE", "CONFIRMADO"))) {
                    throw conflicto("Esta inscripción ya tiene un pago pendiente o confirmado");
                }
                if (inscripcion.getTotal().compareTo(request.monto()) != 0) {
                    throw invalido("El monto debe coincidir con el total de la inscripción");
                }
                pago.setInscripcion(inscripcion);
                pago.setMonto(inscripcion.getTotal());
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
        Pago pago = pagos.bloquear(id).orElseThrow(() -> notFound("pago"));
        if (!"PENDIENTE".equals(pago.getEstado())) throw conflicto("El pago ya fue confirmado");
        if (pago.getInscripcion() != null) {
            Inscripcion inscripcion = inscripciones.bloquear(pago.getInscripcion().getId())
                    .orElseThrow(() -> notFound("inscripción"));
            if (!"PENDIENTE".equals(inscripcion.getEstado())) {
                throw conflicto("La inscripción ya está confirmada o fue cancelada");
            }
            if (pago.getMonto().compareTo(inscripcion.getTotal()) != 0) {
                throw invalido("El monto del pago no coincide con el total de la inscripción");
            }
            inscripcion.setEstado("CONFIRMADA");
        }
        pago.setEstado("CONFIRMADO");
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

    private boolean puedeInscribirse(Participante participante) {
        Usuario usuario = participante.getCliente().getUsuario();
        return usuario == null || (usuario.isActivo() && "CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre()));
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
                "telefono", p.getTelefono(), "clienteId", p.getCliente().getId(), "cliente", p.getCliente().getNombre(),
                "carnet", p.getCliente().getCarnet());
    }
    private Map<String, Object> inscripcionView(Inscripcion i) {
        return item("id", i.getId(), "participanteId", i.getParticipante().getId(),
                "participante", i.getParticipante().getNombre() + " " + i.getParticipante().getApellido(),
                "clienteId", i.getParticipante().getCliente().getId(), "carnet", i.getParticipante().getCliente().getCarnet(),
                "programa", i.getCurso() != null ? i.getCurso().getTitulo() : i.getDiplomado().getNombre(),
                "programaTipo", i.getCurso() != null ? "CURSO" : "DIPLOMADO",
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
                "observaciones", c.getObservaciones(), "detalles",
                c.getDetalles().stream().map(this::detalleCotizacionView).toList());
    }
    private Map<String, Object> detalleCotizacionView(DetalleCotizacion detalle) {
        String tipo;
        String nombre;
        String programacion = detalle.getDescripcion();
        if (detalle.getCurso() != null) {
            tipo = "CURSO";
            nombre = detalle.getCurso().getTitulo();
            programacion = programacionCurso(detalle.getCurso());
        } else if (detalle.getDiplomado() != null) {
            tipo = "DIPLOMADO";
            nombre = detalle.getDiplomado().getNombre();
            programacion = programacionDiplomado(detalle.getDiplomado());
        } else if (detalle.getEspacio() != null) {
            tipo = "ESPACIO";
            nombre = detalle.getEspacio().getNombre();
            if (programacion == null || programacion.isBlank() || programacion.equals(nombre)) {
                programacion = "Fecha y horario no registrados en esta cotización anterior";
            }
        } else if (detalle.getServicioCatering() != null) {
            tipo = "CATERING";
            nombre = detalle.getServicioCatering().getNombre();
            if (programacion == null || programacion.isBlank() || programacion.equals(nombre)) {
                programacion = "Fecha y horario no registrados en esta cotización anterior";
            }
        } else {
            tipo = "SERVICIO";
            nombre = detalle.getDescripcion();
        }
        return item("id", detalle.getId(), "tipo", tipo, "nombre", nombre,
                "cantidad", detalle.getCantidad(), "precioUnitario", detalle.getPrecioUnitario(),
                "subtotal", detalle.getSubtotal(), "descripcion", programacion);
    }
    private Map<String, Object> actividadView(Actividad a) {
        return item("id", a.getId(), "titulo", a.getTitulo(), "tipo", a.getTipo(), "fecha", a.getFecha(),
                "horaInicio", a.getHoraInicio(), "horaFin", a.getHoraFin(), "cupo", a.getCupo());
    }
    private Map<String, Object> pagoView(Pago p) {
        return item("id", p.getId(), "clienteId", p.getCliente().getId(), "cliente", p.getCliente().getNombre(),
                "carnet", p.getCliente().getCarnet(), "concepto", conceptoPago(p),
                "monto", p.getMonto(), "fecha", p.getFecha(), "metodo", p.getMetodo(),
                "referencia", p.getReferencia(), "estado", p.getEstado());
    }

    private String conceptoPago(Pago pago) {
        if (pago.getInscripcion() != null) {
            Inscripcion i = pago.getInscripcion();
            return i.getCurso() != null ? "Curso: " + i.getCurso().getTitulo()
                    : i.getDiplomado() != null ? "Diplomado: " + i.getDiplomado().getNombre() : "Inscripción";
        }
        if (pago.getAlquiler() != null) return "Reserva: " + pago.getAlquiler().getEspacio().getNombre();
        if (pago.getSolicitudCatering() != null) return "Catering: " + pago.getSolicitudCatering().getServicioCatering().getNombre();
        if (pago.getCotizacion() != null) return "Cotización #" + pago.getCotizacion().getId();
        return "Sin concepto asociado";
    }

    private static Map<String, Object> item(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }
}
