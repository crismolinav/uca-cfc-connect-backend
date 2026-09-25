package sv.edu.udb.ucacfcconnect.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.service.AuthService;
import sv.edu.udb.ucacfcconnect.service.JwtService;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuthService authService;
    private final JwtService jwtService;
    private final String redirectUri;
    private final AuthCookieService authCookies;

    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            JwtService jwtService,
            AuthCookieService authCookies,
            @Value("${app.oauth2.success-redirect-uri:http://localhost:3000/auth/callback}") String redirectUri
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.authCookies = authCookies;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new ServletException("El proveedor no devolvió un usuario OpenID Connect");
        }

        Usuario usuario = authService.obtenerUsuarioGoogle(oidcUser.getSubject());
        String token = jwtService.generarToken(usuario);
        authCookies.crear(response, token, true);
        String destino = redirectUri + "#success";

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, destino);
    }
}
