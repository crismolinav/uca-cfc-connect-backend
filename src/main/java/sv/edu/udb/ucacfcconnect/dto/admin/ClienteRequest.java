package sv.edu.udb.ucacfcconnect.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,
        @Size(max = 20, message = "El DUI o NIT no puede superar los 20 caracteres")
        String duiNit,
        @Size(max = 150, message = "La empresa no puede superar los 150 caracteres")
        String empresa,
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String correo,
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion
) {}
