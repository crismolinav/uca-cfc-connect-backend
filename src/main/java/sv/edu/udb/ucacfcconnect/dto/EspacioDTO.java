package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EspacioDTO(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 80) String tipo,
        @NotNull @Positive Integer capacidad,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal precio,
        @Size(max = 255) String equipamiento,
        @NotNull @DecimalMin("0.5") @DecimalMax("24.0") @Digits(integer = 2, fraction = 2) BigDecimal duracionMaximaHoras,
        @NotNull Boolean disponible
) {}
