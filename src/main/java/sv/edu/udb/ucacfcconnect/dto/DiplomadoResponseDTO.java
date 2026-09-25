package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "DiplomadoResponse", description = "Representación completa de un diplomado")
public record DiplomadoResponseDTO(
        Long idDiplomado,
        String nombre,
        String descripcion,
        Integer duracionHoras,
        BigDecimal costo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo,
        Long idCategoria,
        String categoria,
        Long idModalidad,
        String modalidad
) {
}
