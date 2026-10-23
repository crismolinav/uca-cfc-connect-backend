package sv.edu.udb.ucacfcconnect.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarNombreRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} '\u2019-]*") String primerNombre,
        @Size(max = 60) @Pattern(regexp = "[\\p{L}\\p{M} '\u2019-]*") String segundoNombre,
        @NotBlank @Size(max = 60) @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} '\u2019-]*") String primerApellido,
        @NotBlank @Size(max = 60) @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} '\u2019-]*") String segundoApellido
) {
    public ActualizarNombreRequest {
        primerNombre = primerNombre == null ? null : primerNombre.trim();
        segundoNombre = segundoNombre == null ? null : segundoNombre.trim();
        primerApellido = primerApellido == null ? null : primerApellido.trim();
        segundoApellido = segundoApellido == null ? null : segundoApellido.trim();
    }
}
