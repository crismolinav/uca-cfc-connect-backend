package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DetalleCotizacionDTO(
        @Positive Long idCurso,
        @Positive Long idEspacio,
        @NotNull @Positive Integer cantidad,
        @Size(max = 255) String descripcion
) {
    @AssertTrue(message = "Debe seleccionar exactamente un curso o un espacio")
    public boolean isReferenciaValida() {
        return (idCurso == null) != (idEspacio == null);
    }
}
