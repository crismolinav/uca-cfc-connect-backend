package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El DUI es obligatorio")
        @Pattern(regexp = "^[0-9]{8}-[0-9]$", message = "Escribe el DUI con el formato 12345678-9")
        String dui,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @AssertTrue(message = "Debe aceptar los términos y políticas")
        boolean aceptaTerminos
) {
    public RegistroRequest {
        nombre = nombre == null ? null : nombre.trim();
        dui = dui == null ? null : dui.trim();
        correo = correo == null ? null : correo.trim();
    }
}
