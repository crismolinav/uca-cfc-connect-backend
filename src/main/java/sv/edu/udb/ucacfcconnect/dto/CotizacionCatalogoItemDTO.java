package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;

public record CotizacionCatalogoItemDTO(
        Long idReferencia,
        String tipo,
        String nombre,
        BigDecimal precio
) {}
