package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record AlquilerDTO(
        @NotNull @Positive Long idCliente,
        @NotNull @Positive Long idEspacio,
        @NotNull @FutureOrPresent LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @Size(max = 255) String motivo
) {}
