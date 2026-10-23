package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RestablecerPasswordRequest(
        @NotBlank(message = "El enlace de recuperación es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$", message = "El enlace de recuperación no es válido")
        String token,
        @NotBlank(message = "Escribe una contraseña nueva")
        @Size(min = 8, max = 72, message = "Usa una contraseña de entre 8 y 72 caracteres")
        String password
) {}
