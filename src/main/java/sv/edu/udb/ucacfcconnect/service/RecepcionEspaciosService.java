package sv.edu.udb.ucacfcconnect.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.AlquilerRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.EspacioRequest;
import sv.edu.udb.ucacfcconnect.entity.Alquiler;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Espacio;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.AlquilerRepository;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.EspacioRepository;
import sv.edu.udb.ucacfcconnect.repository.PagoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RecepcionEspaciosService {
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private final EspacioRepository espacios;
    private final AlquilerRepository alquileres;
    private final ClienteRepository clientes;
    private final PagoRepository pagos;

    public RecepcionEspaciosService(EspacioRepository espacios, AlquilerRepository alquileres,
                                     ClienteRepository clientes, PagoRepository pagos) {
        this.espacios = espacios;
        this.alquileres = alquileres;
        this.clientes = clientes;
        this.pagos = pagos;
    }

    @Transactional(readOnly = true)
    public List<EspacioView> espacios() {
        return espacios.findAll(Sort.by("nombre")).stream().map(EspacioView::from).toList();
    }

    @Transactional
    public EspacioView guardar(Long id, EspacioRequest request) {
        validarPrecio(request.precio());
        Espacio espacio = id == null ? new Espacio() : espacios.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el espacio"));
        espacio.setNombre(request.nombre().trim());
        espacio.setTipo(request.tipo().trim());
        espacio.setCapacidad(request.capacidad());
        espacio.setPrecio(request.precio());
        espacio.setEquipamiento(opcional(request.equipamiento()));
        espacio.setDisponible(request.disponible());
        return EspacioView.from(espacios.save(espacio));
    }

    @Transactional(readOnly = true)
    public List<AlquilerView> alquileres(LocalDate fecha) {
        List<Alquiler> resultado = fecha == null
                ? alquileres.findAll(Sort.by(Sort.Direction.DESC, "fecha", "horaInicio"))
                : alquileres.findByFechaOrderByHoraInicioAsc(fecha);
        return resultado.stream().map(AlquilerView::from).toList();
    }

    @Transactional
    public AlquilerView reservar(AlquilerRequest request) {
        validarHorario(request.fecha(), request.horaInicio(), request.horaFin());
        Cliente cliente = clientes.findById(request.clienteId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el cliente"));
        // La fila del espacio serializa reservas simultáneas del mismo lugar.
        Espacio espacio = espacios.bloquear(request.espacioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el espacio"));
        if (!espacio.isDisponible()) {
            throw new ApiException(HttpStatus.CONFLICT, "El espacio no está habilitado para reservas");
        }
        if (alquileres.existeCruce(espacio.getId(), request.fecha(), request.horaInicio(), request.horaFin())) {
            throw new ApiException(HttpStatus.CONFLICT, "El espacio ya está reservado en ese horario");
        }
        Alquiler alquiler = new Alquiler();
        alquiler.setCliente(cliente);
        alquiler.setEspacio(espacio);
        alquiler.setFecha(request.fecha());
        alquiler.setHoraInicio(request.horaInicio());
        alquiler.setHoraFin(request.horaFin());
        alquiler.setMotivo(opcional(request.motivo()));
        alquiler.setEstado("RESERVADO");
        return AlquilerView.from(alquileres.save(alquiler));
    }

    @Transactional
    public AlquilerView cancelar(Long id) {
        Alquiler alquiler = alquileres.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró la reserva"));
        if ("CANCELADO".equals(alquiler.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "La reserva ya está cancelada");
        }
        if (pagos.existsByAlquilerIdAndEstado(id, "CONFIRMADO")) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Esta reserva tiene un pago confirmado y requiere gestión administrativa");
        }
        alquiler.setEstado("CANCELADO");
        return AlquilerView.from(alquiler);
    }

    private void validarHorario(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        if (fecha == null || inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La hora final debe ser posterior a la inicial");
        }
        if (inicio.getSecond() != 0 || fin.getSecond() != 0
                || inicio.getMinute() % 10 != 0 || fin.getMinute() % 10 != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las reservas deben usar intervalos de 10 minutos");
        }
        LocalDate hoy = LocalDate.now(ZONA);
        if (fecha.isBefore(hoy) || (fecha.equals(hoy) && !inicio.isAfter(LocalTime.now(ZONA)))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La reserva debe comenzar en una fecha y hora futuras");
        }
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0 || precio.scale() > 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El precio debe ser positivo y tener hasta dos decimales");
        }
    }

    private String opcional(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    public record EspacioView(Long id, String nombre, String tipo, Integer capacidad, BigDecimal precio,
                              boolean disponible, String equipamiento) {
        public static EspacioView from(Espacio espacio) {
            return new EspacioView(espacio.getId(), espacio.getNombre(), espacio.getTipo(),
                    espacio.getCapacidad(), espacio.getPrecio(), espacio.isDisponible(), espacio.getEquipamiento());
        }
    }

    public record AlquilerView(Long id, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                               String estado, String motivo, Long espacioId, String espacio,
                               Long clienteId, String cliente) {
        public static AlquilerView from(Alquiler alquiler) {
            return new AlquilerView(alquiler.getId(), alquiler.getFecha(), alquiler.getHoraInicio(),
                    alquiler.getHoraFin(), alquiler.getEstado(), alquiler.getMotivo(),
                    alquiler.getEspacio().getId(), alquiler.getEspacio().getNombre(),
                    alquiler.getCliente().getId(), alquiler.getCliente().getNombre());
        }
    }
}
