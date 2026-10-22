package sv.edu.udb.ucacfcconnect.dto;

import java.time.LocalDate;

public record ResenaResumenDTO(
        String autor,
        String curso,
        Integer calificacion,
        String comentario,
        LocalDate fecha
) {}
