package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

public record InscripcionCursoRequest(@Positive Long idCurso, @Positive Long idDiplomado) {
    public InscripcionCursoRequest(Long idCurso) { this(idCurso, null); }

    @AssertTrue(message = "Indica un curso o un diplomado")
    public boolean isProgramaValido() {
        return (idCurso != null) != (idDiplomado != null);
    }
}
