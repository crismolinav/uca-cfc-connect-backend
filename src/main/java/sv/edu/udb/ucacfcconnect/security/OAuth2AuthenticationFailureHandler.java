package sv.edu.udb.ucacfcconnect.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final String redirectUri;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.failure-redirect-uri:http://localhost:3000/login}") String redirectUri
    ) {
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        getRedirectStrategy().sendRedirect(
                request, response, redirectUri + "#error=google_authentication_failed");
    }
}
