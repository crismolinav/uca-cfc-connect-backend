package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DocenteResumen", description = "Docente asignado a una oferta académica")
public record DocenteResumenDTO(
        Long idDocente,
        String nombre,
        String especialidad
) {
}
