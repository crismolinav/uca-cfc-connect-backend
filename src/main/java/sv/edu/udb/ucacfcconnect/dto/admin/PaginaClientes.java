package sv.edu.udb.ucacfcconnect.dto.admin;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaClientes(
        List<ClienteResumen> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
    public static PaginaClientes from(Page<ClienteResumen> page) {
        return new PaginaClientes(page.getContent(), page.getTotalElements(),
                page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
