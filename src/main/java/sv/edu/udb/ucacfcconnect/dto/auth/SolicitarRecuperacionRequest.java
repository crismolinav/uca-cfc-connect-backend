package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacionRequest(
        @NotBlank(message = "Escribe tu correo electrónico")
        @Email(message = "Escribe un correo electrónico válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String correo
) {}
