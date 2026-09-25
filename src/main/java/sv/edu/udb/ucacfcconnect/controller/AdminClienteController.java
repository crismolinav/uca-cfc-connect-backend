package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteResumen;
import sv.edu.udb.ucacfcconnect.dto.admin.EstadoClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.PaginaClientes;
import sv.edu.udb.ucacfcconnect.service.AdminClienteService;

@RestController
@RequestMapping("/api/v1/admin/clientes")
public class AdminClienteController {
    private final AdminClienteService clientes;

    public AdminClienteController(AdminClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public PaginaClientes listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return clientes.listar(q, page, size);
    }

    @GetMapping("/{id}")
    public ClienteResumen detalle(@PathVariable Long id) {
        return clientes.detalle(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResumen> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientes.crear(request));
    }

    @PutMapping("/{id}")
    public ClienteResumen actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clientes.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ClienteResumen cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoClienteRequest request) {
        return clientes.cambiarEstado(id, request);
    }
}
