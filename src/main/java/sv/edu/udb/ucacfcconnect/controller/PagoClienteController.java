package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.ucacfcconnect.dto.PagoClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.PagoInscripcionRequest;
import sv.edu.udb.ucacfcconnect.service.PagoClienteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cliente/pagos")
public class PagoClienteController {
    private final PagoClienteService pagos;

    public PagoClienteController(PagoClienteService pagos) { this.pagos = pagos; }

    @GetMapping
    public List<PagoClienteDTO> listar(@AuthenticationPrincipal Jwt jwt) {
        return pagos.listar(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoClienteDTO registrar(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody PagoInscripcionRequest request) {
        return pagos.registrar(Long.valueOf(jwt.getSubject()), request);
    }
}
