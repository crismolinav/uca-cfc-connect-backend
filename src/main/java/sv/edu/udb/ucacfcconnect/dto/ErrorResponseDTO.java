package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(name = "ApiError", description = "Respuesta estándar para errores de la API")
public record ErrorResponseDTO(
        Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "La solicitud contiene datos inválidos") String message,
        @Schema(example = "/api/v1/cursos") String path,
        Map<String, String> fieldErrors
) {
}
