package sv.edu.udb.ucacfcconnect.dto.admin;

import jakarta.validation.constraints.NotNull;

public record EstadoClienteRequest(
        @NotNull(message = "Debes indicar si la cuenta estará activa") Boolean activo
) {}
