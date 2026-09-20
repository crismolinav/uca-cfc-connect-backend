package sv.edu.udb.ucacfcconnect.dto.auth;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta
) {}
