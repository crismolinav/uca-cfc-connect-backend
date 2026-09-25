package sv.edu.udb.ucacfcconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import sv.edu.udb.ucacfcconnect.entity.Usuario;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer:uca-cfc-connect}") String issuer,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        String autoridad = normalizarAutoridad(usuario.getRol().getNombre());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expirationSeconds, ChronoUnit.SECONDS))
                .subject(usuario.getId().toString())
                .claim("email", usuario.getCorreo())
                .claim("nombre", usuario.getNombre())
                .claim("role", autoridad)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String normalizarAutoridad(String rol) {
        return rol.startsWith("ROLE_") ? rol : "ROLE_" + rol.toUpperCase();
    }
}
