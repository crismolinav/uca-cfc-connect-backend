package sv.edu.udb.ucacfcconnect.dto;

import java.time.LocalDate;
import java.util.List;

public record CertificadoCursoDTO(
        String estudiante,
        String curso,
        List<String> docentes,
        LocalDate fechaFinalizacion,
        String codigo
) {}
