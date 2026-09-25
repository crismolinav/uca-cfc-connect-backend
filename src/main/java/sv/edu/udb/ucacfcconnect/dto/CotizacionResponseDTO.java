package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CotizacionResponseDTO(
        Long idCotizacion, LocalDate fecha, String estado, BigDecimal montoEstimado,
        String observaciones, Long idCliente, String nombreCliente,
        List<DetalleCotizacionResponseDTO> detalles
) {}
