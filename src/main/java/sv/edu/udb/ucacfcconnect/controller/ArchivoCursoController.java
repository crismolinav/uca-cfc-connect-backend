package sv.edu.udb.ucacfcconnect.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.service.ArchivoCursoService;
import sv.edu.udb.ucacfcconnect.service.CursoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class ArchivoCursoController {
    private final ArchivoCursoService archivoService;
    private final CursoService cursoService;

    public ArchivoCursoController(ArchivoCursoService archivoService, CursoService cursoService) {
        this.archivoService = archivoService;
        this.cursoService = cursoService;
    }

    @GetMapping("/archivos/cursos/{idCurso}/portadas/{nombreArchivo:.+}")
    public ResponseEntity<Resource> portada(@PathVariable Long idCurso, @PathVariable String nombreArchivo) {
        return servir(archivoService.obtener(idCurso, "portadas", nombreArchivo), true);
    }

    @GetMapping("/archivos/cursos/{idCurso}/recursos/{nombreArchivo:.+}")
    public ResponseEntity<Resource> recurso(@PathVariable Long idCurso, @PathVariable String nombreArchivo,
                                             @AuthenticationPrincipal Jwt jwt) {
        validarAcceso(idCurso, jwt);
        return servir(archivoService.obtener(idCurso, "recursos", nombreArchivo), false);
    }

    /** Atiende enlaces guardados antes de separar portadas y materiales académicos. */
    @GetMapping("/archivos/cursos/{idCurso}/{nombreArchivo:.+}")
    public ResponseEntity<Resource> archivoAnterior(@PathVariable Long idCurso, @PathVariable String nombreArchivo,
                                                     @AuthenticationPrincipal Jwt jwt) {
        String ruta = "/archivos/cursos/" + idCurso + "/" + nombreArchivo;
        boolean esPortada = ruta.equals(cursoService.obtenerPorId(idCurso).imagenUrl());
        if (!esPortada) validarAcceso(idCurso, jwt);
        return servir(archivoService.obtenerAnterior(idCurso, nombreArchivo), esPortada);
    }

    private void validarAcceso(Long idCurso, Jwt jwt) {
        if (jwt == null) throw new AccessDeniedException("Debes iniciar sesión para acceder al material del curso");
        Long usuarioId = Long.valueOf(jwt.getSubject());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = tieneRol(auth, "ROLE_ADMIN");
        boolean docente = tieneRol(auth, "ROLE_DOCENTE");
        if (admin || docente) {
            cursoService.validarPermisoEdicion(idCurso, usuarioId);
            return;
        }
        if (!cursoService.clienteTieneAccesoConfirmado(usuarioId, idCurso)) {
            throw new AccessDeniedException("El material estará disponible cuando recepción confirme tu pago");
        }
    }

    private boolean tieneRol(Authentication auth, String rol) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> rol.equals(authority.getAuthority()));
    }

    private ResponseEntity<Resource> servir(Path path, boolean publico) {
        MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String detectado = Files.probeContentType(path);
            if (detectado != null) tipo = MediaType.parseMediaType(detectado);
        } catch (IOException ignored) {
            // El tipo genérico permite descargar archivos cuyo MIME no se pudo detectar.
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(publico ? CacheControl.noCache() : CacheControl.noStore())
                .body(new FileSystemResource(path));
    }
}
