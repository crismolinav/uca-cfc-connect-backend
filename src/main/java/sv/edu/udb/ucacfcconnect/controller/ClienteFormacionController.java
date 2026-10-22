package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.ucacfcconnect.dto.InscripcionClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.InscripcionCursoRequest;
import sv.edu.udb.ucacfcconnect.service.ClienteFormacionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cliente")
public class ClienteFormacionController {
    private final ClienteFormacionService formacion;

    public ClienteFormacionController(ClienteFormacionService formacion) { this.formacion = formacion; }

    @GetMapping("/inscripciones")
    public List<InscripcionClienteDTO> inscripciones(@AuthenticationPrincipal Jwt jwt) {
        return formacion.inscripciones(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/inscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionClienteDTO inscribir(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody InscripcionCursoRequest request) {
        return formacion.inscribir(Long.valueOf(jwt.getSubject()), request);
    }

}
