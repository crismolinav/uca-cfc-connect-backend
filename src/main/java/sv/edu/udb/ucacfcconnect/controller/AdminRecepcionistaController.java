package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.dto.admin.RecepcionistaRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.RecepcionistaResponse;
import sv.edu.udb.ucacfcconnect.service.AdminRecepcionistaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/recepcionistas")
public class AdminRecepcionistaController {
    private final AdminRecepcionistaService service;

    public AdminRecepcionistaController(AdminRecepcionistaService service) { this.service = service; }

    @GetMapping
    public List<RecepcionistaResponse> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecepcionistaResponse crear(@Valid @RequestBody RecepcionistaRequest request) {
        return service.crear(request);
    }

    @PatchMapping("/{id}/estado")
    public RecepcionistaResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.activo());
    }

    public record EstadoRequest(boolean activo) {}
}
