package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.ucacfcconnect.dto.ResenaCursoRequest;
import sv.edu.udb.ucacfcconnect.dto.ResenaResumenDTO;
import sv.edu.udb.ucacfcconnect.service.ResenaCursoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos/{idCurso}/resenas")
public class ResenaCursoController {
    private final ResenaCursoService servicio;
    public ResenaCursoController(ResenaCursoService servicio) { this.servicio = servicio; }

    @GetMapping
    public List<ResenaResumenDTO> listar(@PathVariable @Positive Long idCurso) {
        return servicio.listar(idCurso);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResenaResumenDTO crear(@PathVariable @Positive Long idCurso, @AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ResenaCursoRequest solicitud) {
        return servicio.crear(idCurso, Long.valueOf(jwt.getSubject()), solicitud);
    }
}
