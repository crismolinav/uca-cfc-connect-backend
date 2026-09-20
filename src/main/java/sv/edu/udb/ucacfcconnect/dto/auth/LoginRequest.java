package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        Boolean recordar
) {
    public LoginRequest {
        correo = correo == null ? null : correo.trim();
    }
}
