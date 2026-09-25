package sv.edu.udb.ucacfcconnect.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecepcionistaRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 120) String correo,
        @NotBlank @Size(min = 10, max = 72) String password
) {}
