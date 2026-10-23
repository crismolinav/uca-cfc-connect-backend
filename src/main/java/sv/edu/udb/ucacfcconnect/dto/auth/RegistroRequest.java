package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El primer nombre es obligatorio")
        @Size(max = 60, message = "El primer nombre no puede superar los 60 caracteres")
        @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} '’-]*", message = "Escribe un primer nombre válido")
        String primerNombre,
        @Size(max = 60, message = "El segundo nombre no puede superar los 60 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{M} '’-]*", message = "Escribe un segundo nombre válido")
        String segundoNombre,
        @NotBlank(message = "El primer apellido es obligatorio")
        @Size(max = 60, message = "El primer apellido no puede superar los 60 caracteres")
        @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} '’-]*", message = "Escribe un primer apellido válido")
        String primerApellido,
        @NotBlank(message = "El segundo apellido es obligatorio")
        @Size(max = 60, message = "El segundo apellido no puede superar los 60 caracteres")
        @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} '’-]*", message = "Escribe un segundo apellido válido")
        String segundoApellido,

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
        primerNombre = limpiar(primerNombre);
        segundoNombre = limpiar(segundoNombre);
        primerApellido = limpiar(primerApellido);
        segundoApellido = limpiar(segundoApellido);
        dui = dui == null ? null : dui.trim();
        correo = correo == null ? null : correo.trim();
    }

    public String nombre() {
        return java.util.stream.Stream.of(primerNombre, segundoNombre, primerApellido, segundoApellido)
                .filter(s -> s != null && !s.isBlank()).collect(java.util.stream.Collectors.joining(" "));
    }

    private static String limpiar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
}
