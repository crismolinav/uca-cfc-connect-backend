package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceptarInvitacionDocenteRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password,
        @AssertTrue(message = "Debe aceptar los términos y políticas") boolean aceptaTerminos
) {}
