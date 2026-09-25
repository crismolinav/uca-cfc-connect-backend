package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;

public record DetalleCotizacionResponseDTO(
        Long idDetalle, String tipo, Long idReferencia, String nombre, String descripcion,
        Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal
) {}
