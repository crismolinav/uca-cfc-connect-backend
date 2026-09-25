package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "CursoResponse", description = "Representación completa de un curso")
public record CursoResponseDTO(
        @Schema(example = "10") Long idCurso,
        @Schema(example = "Excel avanzado para negocios") String titulo,
        String descripcion,
        @Schema(example = "32") Integer duracionHoras,
        @Schema(example = "25") Integer cupoMaximo,
        @Schema(example = "125.00") BigDecimal costo,
        @Schema(example = "2026-10-05") LocalDate fechaInicio,
        @Schema(example = "2026-11-05") LocalDate fechaFin,
        @Schema(example = "Lunes y miércoles, 18:00-20:00") String horario,
        @Schema(example = "true") Boolean activo,
        @Schema(example = "1") Long idCategoria,
        @Schema(example = "Tecnología") String categoria,
        @Schema(example = "1") Long idModalidad,
        @Schema(example = "Presencial") String modalidad,
        List<DocenteResumenDTO> docentes
) {
}
