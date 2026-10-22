package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PagoInscripcionRequest(
        @Positive long idInscripcion,
        @NotBlank String metodo,
        @Size(max = 100) String referencia
) {}
