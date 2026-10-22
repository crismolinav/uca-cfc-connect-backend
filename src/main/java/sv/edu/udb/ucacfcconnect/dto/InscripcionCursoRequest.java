package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InscripcionCursoRequest(@NotNull @Positive Long idCurso) {}
