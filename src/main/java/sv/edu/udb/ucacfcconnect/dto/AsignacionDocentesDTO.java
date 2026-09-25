package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "AsignacionDocentesRequest", description = "Docentes que impartirán un curso o diplomado")
public record AsignacionDocentesDTO(
        @NotNull(message = "Debe enviar la lista de docentes")
        @Size(max = 50, message = "No se pueden asignar más de 50 docentes")
        List<@NotNull(message = "El identificador del docente es obligatorio")
                @Positive(message = "El identificador del docente debe ser mayor que cero") Long> idsDocentes
) {
}
