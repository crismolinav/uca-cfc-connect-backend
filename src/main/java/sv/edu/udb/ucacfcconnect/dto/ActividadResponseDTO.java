package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(name = "ActividadResponse", description = "Actividad incluida en la agenda institucional")
public record ActividadResponseDTO(
        Long idActividad,
        String titulo,
        String tipo,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer cupo,
        Long idDiplomado
) {
}
