package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DocenteResponse", description = "Información administrativa de un docente")
public record DocenteResponseDTO(
        Long idDocente,
        String nombre,
        String especialidad,
        String correo,
        String telefono
) {
}
