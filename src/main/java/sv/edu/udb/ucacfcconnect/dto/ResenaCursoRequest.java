package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResenaCursoRequest(
        @Positive long idInscripcion,
        @Min(1) @Max(5) int calificacionCurso,
        @NotBlank @Size(max = 2000) String comentarioCurso,
        @Positive Long idDocente,
        @Min(1) @Max(5) Integer calificacionDocente,
        @Size(max = 2000) String comentarioDocente
) {}
