package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarNombreRequest(
        @NotBlank(message = "Escribe tu nombre")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {
    public ActualizarNombreRequest {
        nombre = nombre == null ? null : nombre.trim();
    }
}
