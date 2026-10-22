package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InscripcionClienteDTO(
        Long idInscripcion,
        Long idCurso,
        String curso,
        LocalDate fecha,
        String estado,
        BigDecimal total
) {}
