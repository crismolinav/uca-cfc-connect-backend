package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "EstadoDiplomadoRequest", description = "Nuevo estado de publicación de un diplomado")
public record EstadoDiplomadoDTO(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
