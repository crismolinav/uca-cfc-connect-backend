package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "DiplomadoRequest", description = "Datos requeridos para crear o actualizar un diplomado")
public record DiplomadoDTO(
        @NotBlank(message = "El nombre del diplomado es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        @Schema(example = "Diplomado en analítica de datos")
        String nombre,

        @NotBlank(message = "La descripción no puede estar vacía")
        @Size(min = 10, max = 5000, message = "La descripción debe tener entre 10 y 5000 caracteres")
        String descripcion,

        @NotNull(message = "La duración en horas es obligatoria")
        @Positive(message = "La duración debe ser mayor que cero")
        @Schema(example = "40")
        Integer duracionHoras,

        @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El costo debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El costo debe tener como máximo 8 enteros y 2 decimales")
        @Schema(example = "350.00")
        BigDecimal costo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        @NotNull(message = "Debe seleccionar una categoría")
        @Positive(message = "El identificador de categoría debe ser mayor que cero")
        Long idCategoria,

        @NotNull(message = "Debe seleccionar una modalidad")
        @Positive(message = "El identificador de modalidad debe ser mayor que cero")
        Long idModalidad
) {
}
