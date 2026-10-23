package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DetalleCotizacionDTO(
        @Positive Long idCurso,
        @Positive Long idEspacio,
        @Positive Long idDiplomado,
        @Positive Long idServicioCatering,
        @NotNull @Positive Integer cantidad,
        @Size(max = 255) String descripcion
) {
    public DetalleCotizacionDTO(Long idCurso, Long idEspacio, Integer cantidad, String descripcion) {
        this(idCurso, idEspacio, null, null, cantidad, descripcion);
    }

    @AssertTrue(message = "Debe seleccionar exactamente un servicio cotizable")
    public boolean isReferenciaValida() {
        int referencias = 0;
        if (idCurso != null) referencias++;
        if (idEspacio != null) referencias++;
        if (idDiplomado != null) referencias++;
        if (idServicioCatering != null) referencias++;
        return referencias == 1;
    }
}
