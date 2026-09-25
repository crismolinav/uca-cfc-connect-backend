package sv.edu.udb.ucacfcconnect.dto.recepcion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public final class RecepcionRequests {
    private RecepcionRequests() {}

    public record EspacioRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 80) String tipo,
            @NotNull @Positive Integer capacidad,
            @NotNull @DecimalMin("0.00") BigDecimal precio,
            @Size(max = 255) String equipamiento,
            boolean disponible
    ) {}

    public record AlquilerRequest(
            @NotNull Long clienteId,
            @NotNull Long espacioId,
            @NotNull @FutureOrPresent LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @Size(max = 255) String motivo
    ) {}

    public record ParticipanteRequest(
            @NotNull Long clienteId,
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @Email @Size(max = 120) String correo,
            @Size(max = 20) String telefono
    ) {}

    public record InscripcionRequest(
            @NotNull Long participanteId,
            String programaTipo,
            @NotNull Long programaId
    ) {}

    public record ServicioRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 80) String tipoServicio,
            @NotNull @DecimalMin("0.00") BigDecimal precioBase,
            @Size(max = 255) String descripcion
    ) {}

    public record CateringRequest(
            @NotNull Long clienteId,
            @NotNull Long servicioId,
            @NotNull @FutureOrPresent LocalDate fecha,
            @NotNull LocalTime hora,
            @NotBlank @Size(max = 150) String lugar,
            @NotNull @Positive Integer numeroAsistentes,
            @NotBlank @Size(max = 255) String menu
    ) {}

    public record CotizacionRequest(
            @NotNull Long clienteId,
            @NotBlank String conceptoTipo,
            @NotNull Long conceptoId,
            @NotNull @Positive Integer cantidad,
            @Size(max = 255) String observaciones
    ) {}

    public record ActividadRequest(
            @NotBlank @Size(max = 150) String titulo,
            @NotNull @FutureOrPresent LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @Positive Integer cupo,
            @NotBlank String vinculacionTipo,
            @NotNull Long vinculacionId
    ) {}

    public record PagoRequest(
            @NotNull Long clienteId,
            @NotNull @DecimalMin("0.01") BigDecimal monto,
            @NotBlank String metodo,
            @Size(max = 100) String referencia,
            @NotBlank String conceptoTipo,
            @NotNull Long conceptoId
    ) {}
}
