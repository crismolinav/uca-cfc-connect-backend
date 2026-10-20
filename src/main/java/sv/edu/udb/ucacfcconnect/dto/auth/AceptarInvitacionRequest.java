package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AceptarInvitacionRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = "^[0-9]{8}-[0-9]$", message = "Escribe el DUI con el formato 12345678-9") String dui,
        @NotBlank @Size(min = 8, max = 72) String password,
        @AssertTrue(message = "Debe aceptar los términos y políticas") boolean aceptaTerminos
) {}
