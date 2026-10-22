package sv.edu.udb.ucacfcconnect.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.ucacfcconnect.dto.auth.AceptarInvitacionDocenteRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.AuthResponse;
import sv.edu.udb.ucacfcconnect.security.AuthCookieService;
import sv.edu.udb.ucacfcconnect.service.InvitacionDocenteService;

@RestController
public class DocenteAccesoController {
    private final InvitacionDocenteService invitaciones;
    private final AuthCookieService cookies;
    public DocenteAccesoController(InvitacionDocenteService invitaciones, AuthCookieService cookies) {
        this.invitaciones = invitaciones; this.cookies = cookies;
    }

    @PostMapping("/api/v1/docentes/{id}/invitacion")
    public InvitacionDocenteService.Invitacion invitar(@PathVariable Long id) { return invitaciones.invitar(id); }

    @GetMapping("/api/v1/auth/invitacion-docente")
    public InvitacionDocenteService.Datos consultar(@RequestParam String token) { return invitaciones.consultar(token); }

    @PostMapping("/api/v1/auth/aceptar-invitacion-docente")
    public ResponseEntity<AuthResponse> aceptar(@Valid @RequestBody AceptarInvitacionDocenteRequest solicitud,
                                                 HttpServletResponse response) {
        var sesion = invitaciones.aceptar(solicitud);
        cookies.crear(response, sesion.token(), false);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(sesion.expiraEnSegundos(), sesion.usuario()));
    }
}
