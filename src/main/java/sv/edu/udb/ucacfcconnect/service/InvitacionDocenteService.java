package sv.edu.udb.ucacfcconnect.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.auth.AceptarInvitacionDocenteRequest;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.DocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class InvitacionDocenteService {
    private final DocenteRepository docentes;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtService jwtService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String issuer, delivery, baseUrl, mailUsername;

    public InvitacionDocenteService(DocenteRepository docentes, UsuarioRepository usuarios, RolRepository roles,
            PasswordEncoder passwords, JwtEncoder encoder, JwtDecoder decoder, JwtService jwtService,
            ObjectProvider<JavaMailSender> mailSender, @Value("${app.jwt.issuer:uca-cfc-connect}") String issuer,
            @Value("${app.invitation.delivery:local}") String delivery,
            @Value("${app.invitation.base-url:http://localhost:8080}") String baseUrl,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.docentes = docentes; this.usuarios = usuarios; this.roles = roles; this.passwords = passwords;
        this.encoder = encoder; this.decoder = decoder; this.jwtService = jwtService; this.mailSender = mailSender;
        this.issuer = issuer; this.delivery = delivery; this.baseUrl = baseUrl; this.mailUsername = mailUsername;
    }

    @Transactional(readOnly = true)
    public Invitacion invitar(Long id) {
        var docente = docentes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el docente"));
        if (docente.getUsuario() != null) throw new ApiException(HttpStatus.CONFLICT, "Este docente ya tiene acceso");
        String correo = normalizar(docente.getCorreo());
        if (correo == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Registra un correo antes de invitar al docente");
        if (usuarios.existsByCorreoIgnoreCase(correo)) throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya pertenece a una cuenta");
        Instant ahora = Instant.now();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(issuer).issuedAt(ahora).expiresAt(ahora.plus(24, ChronoUnit.HOURS))
                        .subject("docente:" + id).claim("purpose", "registro-docente").claim("email", correo).build())).getTokenValue();
        String enlace = baseUrl.replaceAll("/+$", "") + "/auth/invitacion-docente.html#token=" + token;
        if ("smtp".equalsIgnoreCase(delivery)) {
            if (mailUsername.isBlank() || mailSender.getIfAvailable() == null)
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Configura el correo para enviar invitaciones");
            SimpleMailMessage mensaje = new SimpleMailMessage(); mensaje.setFrom(mailUsername); mensaje.setTo(correo);
            mensaje.setSubject("Acceso docente a UCA CFC Connect");
            mensaje.setText("Hola " + docente.getNombre() + ",\n\nCrea tu acceso docente con este enlace, vigente durante 24 horas:\n" + enlace);
            try { mailSender.getObject().send(mensaje); }
            catch (MailException ex) { throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo enviar la invitación"); }
            return new Invitacion("Enviamos la invitación a " + correo, null);
        }
        if (!"local".equalsIgnoreCase(delivery)) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El modo de envío de invitaciones no es válido");
        return new Invitacion("Enlace local para " + correo, enlace);
    }

    @Transactional(readOnly = true)
    public Datos consultar(String token) {
        var docente = docenteValido(token);
        return new Datos(docente.getNombre(), docente.getCorreo());
    }

    @Transactional
    public AuthSession aceptar(AceptarInvitacionDocenteRequest request) {
        var docente = docenteValido(request.token());
        String correo = normalizar(docente.getCorreo());
        if (usuarios.existsByCorreoIgnoreCase(correo)) throw new ApiException(HttpStatus.CONFLICT, "Este correo ya tiene una cuenta");
        Rol rol = roles.findByNombreIgnoreCase("DOCENTE").orElseGet(() -> {
            Rol nuevo = new Rol(); nuevo.setNombre("DOCENTE"); nuevo.setDescripcion("Docente asignado a cursos"); return roles.save(nuevo);
        });
        Usuario usuario = new Usuario(); usuario.setNombre(docente.getNombre()); usuario.setCorreo(correo);
        usuario.setPassword(passwords.encode(request.password())); usuario.setActivo(true);
        usuario.setEmailVerificado("smtp".equalsIgnoreCase(delivery)); usuario.setRol(rol);
        usuario = usuarios.saveAndFlush(usuario); docente.setUsuario(usuario); usuario.setDocente(docente);
        return new AuthSession(jwtService.generarToken(usuario), jwtService.getExpirationSeconds(),
                sv.edu.udb.ucacfcconnect.dto.auth.UsuarioResponse.from(usuario));
    }

    private sv.edu.udb.ucacfcconnect.entity.Docente docenteValido(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            if (!"registro-docente".equals(jwt.getClaimAsString("purpose")) || jwt.getSubject() == null
                    || !jwt.getSubject().startsWith("docente:")) throw new IllegalArgumentException();
            Long id = Long.valueOf(jwt.getSubject().substring("docente:".length()));
            var docente = docentes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el docente"));
            if (docente.getUsuario() != null || !normalizar(docente.getCorreo()).equals(jwt.getClaimAsString("email")))
                throw new ApiException(HttpStatus.CONFLICT, "La invitación ya se usó o el correo cambió");
            return docente;
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La invitación caducó o no es válida");
        }
    }

    private String normalizar(String correo) { return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT); }
    public record Invitacion(String mensaje, String enlaceLocal) {}
    public record Datos(String nombre, String correo) {}
}
