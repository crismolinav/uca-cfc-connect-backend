package sv.edu.udb.ucacfcconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "Pagina", description = "Respuesta paginada")
public record PaginaDTO<T>(
        List<T> contenido,
        @Schema(example = "0") int paginaActual,
        @Schema(example = "20") int tamanoPagina,
        @Schema(example = "53") long totalElementos,
        @Schema(example = "3") int totalPaginas,
        boolean primera,
        boolean ultima
) {
    public static <T> PaginaDTO<T> desde(Page<T> pagina) {
        return new PaginaDTO<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}
