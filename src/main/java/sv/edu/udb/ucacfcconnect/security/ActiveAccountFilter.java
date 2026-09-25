package sv.edu.udb.ucacfcconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sv.edu.udb.ucacfcconnect.dto.auth.ErrorResponse;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    private final AuthCookieService authCookies;
    private final ObjectMapper objectMapper;

    public ActiveAccountFilter(
            UsuarioRepository usuarios,
            AuthCookieService authCookies,
            ObjectMapper objectMapper
    ) {
        this.usuarios = usuarios;
        this.authCookies = authCookies;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().equals("/api/v1/auth/logout");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
            Long usuarioId;
            try {
                usuarioId = Long.valueOf(jwt.getToken().getSubject());
            } catch (NumberFormatException exception) {
                rechazar(request, response);
                return;
            }
            if (!usuarios.existsByIdAndActivoTrue(usuarioId)) {
                rechazar(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        authCookies.eliminar(response);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                Instant.now(), HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(),
                "La cuenta está desactivada", request.getRequestURI()));
    }
}
