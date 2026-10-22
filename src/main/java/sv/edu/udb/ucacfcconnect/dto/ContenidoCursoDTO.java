package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.Size;

public record ContenidoCursoDTO(
        @Size(max = 5000) String loQueAprenderas,
        @Size(max = 50000) String contenidoPrograma,
        @Size(max = 5000) String requisitos,
        @Size(max = 5000) String dirigidoA,
        @Size(min = 10, max = 5000) String descripcion
) {}
