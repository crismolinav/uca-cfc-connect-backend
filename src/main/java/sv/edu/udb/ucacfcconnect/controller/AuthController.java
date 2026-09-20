package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.dto.auth.AuthResponse;
import sv.edu.udb.ucacfcconnect.dto.auth.DuiRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.LoginRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.RegistroRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.UsuarioResponse;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.security.AuthCookieService;
import sv.edu.udb.ucacfcconnect.service.AuthService;
import sv.edu.udb.ucacfcconnect.service.AuthSession;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final AuthCookieService authCookies;

    public AuthController(
            AuthService authService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            AuthCookieService authCookies
    ) {
        this.authService = authService;
        this.clientRegistrations = clientRegistrations;
        this.authCookies = authCookies;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(
            @Valid @RequestBody RegistroRequest request,
            HttpServletResponse response
    ) {
        AuthSession sesion = authService.registrar(request);
        authCookies.crear(response, sesion.token(), false);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(sesion.expiraEnSegundos(), sesion.usuario()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthSession sesion = authService.login(request);
        authCookies.crear(response, sesion.token(), Boolean.TRUE.equals(request.recordar()));
        return new AuthResponse(sesion.expiraEnSegundos(), sesion.usuario());
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookies.eliminar(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/google")
    public ResponseEntity<Void> iniciarGoogle() {
        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        if (registrations == null || registrations.findByRegistrationId("google") == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El acceso con Google todavía no tiene credenciales configuradas");
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }

    @GetMapping("/me")
    public UsuarioResponse usuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.from(authService.obtenerUsuarioPorId(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/me/dui")
    public UsuarioResponse completarDui(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DuiRequest request) {
        return UsuarioResponse.from(authService.completarDui(Long.valueOf(jwt.getSubject()), request));
    }
}
