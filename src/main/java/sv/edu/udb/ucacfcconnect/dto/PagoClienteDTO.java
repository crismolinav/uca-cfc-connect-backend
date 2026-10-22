package sv.edu.udb.ucacfcconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoClienteDTO(
        Long idPago,
        Long idInscripcion,
        String curso,
        BigDecimal monto,
        LocalDate fecha,
        String metodo,
        String referencia,
        String estado
) {}
