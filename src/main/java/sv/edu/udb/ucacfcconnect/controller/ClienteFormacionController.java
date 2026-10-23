package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.ucacfcconnect.dto.InscripcionClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.CertificadoCursoDTO;
import sv.edu.udb.ucacfcconnect.dto.InscripcionCursoRequest;
import sv.edu.udb.ucacfcconnect.dto.ProgresoModuloRequest;
import sv.edu.udb.ucacfcconnect.service.ClienteFormacionService;
import sv.edu.udb.ucacfcconnect.service.CertificadoPdfService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cliente")
public class ClienteFormacionController {
    private final ClienteFormacionService formacion;
    private final CertificadoPdfService certificados;

    public ClienteFormacionController(ClienteFormacionService formacion, CertificadoPdfService certificados) {
        this.formacion = formacion;
        this.certificados = certificados;
    }

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

    @PostMapping("/cursos/{idCurso}/progreso")
    public InscripcionClienteDTO completarModulo(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long idCurso,
                                                  @Valid @RequestBody ProgresoModuloRequest request) {
        return formacion.completarModulo(Long.valueOf(jwt.getSubject()), idCurso, request.numeroModulo());
    }

    @GetMapping("/cursos/{idCurso}/certificado")
    public ResponseEntity<byte[]> certificado(@AuthenticationPrincipal Jwt jwt, @PathVariable Long idCurso) {
        CertificadoCursoDTO datos = formacion.certificado(Long.valueOf(jwt.getSubject()), idCurso);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certificados.nombreArchivo(datos) + "\"")
                .cacheControl(CacheControl.noStore())
                .body(certificados.generar(datos));
    }

}
