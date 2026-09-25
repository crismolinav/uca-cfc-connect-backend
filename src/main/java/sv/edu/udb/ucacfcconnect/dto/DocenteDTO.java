package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "DocenteRequest", description = "Datos para registrar o actualizar un docente")
public record DocenteDTO(
        @NotBlank(message = "El nombre del docente es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        @Schema(example = "Ana Martínez")
        String nombre,

        @NotBlank(message = "La especialidad es obligatoria")
        @Size(min = 3, max = 120, message = "La especialidad debe tener entre 3 y 120 caracteres")
        @Schema(example = "Analítica de datos")
        String especialidad,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        @Schema(example = "ana.martinez@uca.edu.sv")
        String correo,

        @Pattern(
                regexp = "^$|^[+0-9() .-]{7,20}$",
                message = "El teléfono debe contener entre 7 y 20 caracteres válidos"
        )
        @Schema(example = "+503 2222-0000")
        String telefono
) {
}
