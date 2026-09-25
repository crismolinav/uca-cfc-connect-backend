package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CotizacionDTO(
        @NotNull @Positive Long idCliente,
        @Size(max = 255) String observaciones,
        @NotEmpty @Size(max = 50) List<@Valid DetalleCotizacionDTO> detalles
) {}
