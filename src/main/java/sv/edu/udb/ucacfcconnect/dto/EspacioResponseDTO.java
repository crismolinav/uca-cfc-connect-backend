package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;

public record EspacioResponseDTO(
        Long idEspacio, String nombre, String tipo, Integer capacidad,
        BigDecimal precio, boolean disponible, String equipamiento, BigDecimal duracionMaximaHoras
) {}
