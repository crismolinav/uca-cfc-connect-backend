package sv.edu.udb.ucacfcconnect.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AlquilerResponseDTO(
        Long idAlquiler, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
        String motivo, String estado, Long idCliente, String nombreCliente,
        Long idEspacio, String nombreEspacio
) {}
