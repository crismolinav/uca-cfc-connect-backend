package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CatalogoResponse", description = "Elemento de un catálogo académico")
public record CatalogoResponseDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "Presencial") String nombre,
        String descripcion
) {
}
