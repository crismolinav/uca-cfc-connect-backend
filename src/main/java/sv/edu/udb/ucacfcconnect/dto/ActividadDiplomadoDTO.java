package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(name = "ActividadDiplomadoRequest", description = "Sesión programada de un diplomado")
public record ActividadDiplomadoDTO(
        @NotBlank(message = "El título de la sesión es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,

        @NotNull(message = "La fecha de la sesión es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @Positive(message = "El cupo debe ser mayor que cero")
        Integer cupo
) {
}
