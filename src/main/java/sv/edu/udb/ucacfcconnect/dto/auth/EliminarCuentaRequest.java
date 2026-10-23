package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EliminarCuentaRequest(@NotBlank String confirmacion) {}
