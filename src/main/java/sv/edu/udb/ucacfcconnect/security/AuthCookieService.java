package sv.edu.udb.ucacfcconnect.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class AuthCookieService {
    public static final String DEFAULT_COOKIE_NAME = "uca_cfc_session";

    private final String cookieName;
    private final boolean secure;
    private final Duration expiration;

    public AuthCookieService(
            @Value("${app.auth.cookie-name:" + DEFAULT_COOKIE_NAME + "}") String cookieName,
            @Value("${app.auth.cookie-secure:false}") boolean secure,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.expiration = Duration.ofSeconds(expirationSeconds);
    }

    public void crear(HttpServletResponse response, String token, boolean persistente) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/");
        if (persistente) cookie.maxAge(expiration);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    public void eliminar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build().toString());
    }

    public String resolver(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
