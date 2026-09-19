package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DuiRequest(
        @NotBlank(message = "El DUI es obligatorio")
        @Pattern(regexp = "^[0-9]{8}-[0-9]$", message = "Escribe el DUI con el formato 12345678-9")
        String dui
) {
    public DuiRequest {
        dui = dui == null ? null : dui.trim();
    }
}
