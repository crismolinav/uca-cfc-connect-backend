package sv.edu.udb.ucacfcconnect.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.dto.CursoResponseDTO;
import sv.edu.udb.ucacfcconnect.service.DocenteCursoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/docente/cursos")
public class DocentePortalController {
    private final DocenteCursoService cursos;
    public DocentePortalController(DocenteCursoService cursos) { this.cursos = cursos; }

    @GetMapping
    public List<CursoResponseDTO> listar(@AuthenticationPrincipal Jwt jwt) {
        return cursos.listar(Long.valueOf(jwt.getSubject()));
    }
}
