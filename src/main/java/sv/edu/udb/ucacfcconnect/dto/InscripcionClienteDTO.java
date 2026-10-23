package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InscripcionClienteDTO(
        Long idInscripcion,
        Long idCurso,
        String curso,
        LocalDate fecha,
        String estado,
        BigDecimal total,
        boolean resenaEnviada,
        String imagenUrl,
        List<Integer> modulosCompletados,
        int totalModulos,
        int progresoPorcentaje,
        boolean finalizado,
        LocalDate fechaFinalizacion
) {}
