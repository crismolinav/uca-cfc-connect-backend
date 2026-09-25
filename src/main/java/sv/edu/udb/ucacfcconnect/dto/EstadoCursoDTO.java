package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "EstadoCursoRequest", description = "Nuevo estado de un curso")
public record EstadoCursoDTO(
        @NotNull(message = "El estado activo es obligatorio")
        @Schema(example = "false")
        Boolean activo
) {
}
