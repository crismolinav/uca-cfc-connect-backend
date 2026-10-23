package sv.edu.udb.ucacfcconnect.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.auth.AceptarInvitacionRequest;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class InvitacionClienteService {
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final ParticipanteRegistroService registroParticipantes;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtService jwtService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String issuer;
    private final String delivery;
    private final String baseUrl;
    private final String mailUsername;

    public InvitacionClienteService(ClienteRepository clientes, UsuarioRepository usuarios, RolRepository roles,
                                     ParticipanteRegistroService registroParticipantes, PasswordEncoder passwords,
                                     JwtEncoder encoder, JwtDecoder decoder, JwtService jwtService,
                                     ObjectProvider<JavaMailSender> mailSender,
                                     @Value("${app.jwt.issuer:uca-cfc-connect}") String issuer,
                                     @Value("${app.invitation.delivery:local}") String delivery,
                                     @Value("${app.invitation.base-url:http://localhost:8080}") String baseUrl,
                                     @Value("${spring.mail.username:}") String mailUsername) {
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.roles = roles;
        this.registroParticipantes = registroParticipantes;
        this.passwords = passwords;
        this.encoder = encoder;
        this.decoder = decoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
        this.issuer = issuer;
        this.delivery = delivery;
        this.baseUrl = baseUrl;
        this.mailUsername = mailUsername;
    }

    @Transactional(readOnly = true)
    public InvitacionEnviada invitar(Long clienteId) {
        Cliente cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el cliente"));
        if (cliente.getUsuario() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Este cliente ya tiene una cuenta de acceso");
        }
        String correo = normalizar(cliente.getCorreo());
        if (correo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Registra un correo antes de enviar la invitación");
        }
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya pertenece a una cuenta; revisa el cliente antes de invitarlo");
        }
        Instant ahora = Instant.now();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(issuer).issuedAt(ahora)
                        .expiresAt(ahora.plus(24, ChronoUnit.HOURS))
                        .subject("invitacion:" + clienteId)
                        .claim("purpose", "registro-cliente")
                        .claim("email", correo).build())).getTokenValue();
        String enlace = baseUrl.replaceAll("/+$", "") + "/auth/invitacion.html#token=" + token;
        if ("smtp".equalsIgnoreCase(delivery)) {
            if (mailUsername.isBlank() || mailSender.getIfAvailable() == null) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Configura MAIL_USERNAME y MAIL_PASSWORD para enviar correos");
            }
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(mailUsername);
            mensaje.setTo(correo);
            mensaje.setSubject("Crea tu cuenta en UCA CFC Connect");
            mensaje.setText("Hola " + cliente.getNombre() + ",\n\n" +
                    "Recepción te invitó a crear tu cuenta. Abre este enlace (vigente durante 24 horas):\n" +
                    enlace + "\n\nSi no solicitaste esta invitación, ignora este mensaje.");
            try {
                mailSender.getObject().send(mensaje);
            } catch (MailException ex) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo enviar el correo. Revisa la configuración SMTP");
            }
            return new InvitacionEnviada("Enviamos la invitación a " + correo, null);
        }
        if (!"local".equalsIgnoreCase(delivery)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El modo de envío de invitaciones no es válido");
        }
        return new InvitacionEnviada("Enlace de prueba local para " + correo, enlace);
    }

    @Transactional(readOnly = true)
    public DatosInvitacion consultar(String token) {
        Cliente cliente = clienteVigente(token, false);
        return new DatosInvitacion(cliente.getNombre(), cliente.getCorreo(), cliente.getDuiNit() != null);
    }

    @Transactional
    public AuthSession aceptar(AceptarInvitacionRequest request) {
        Cliente cliente = clienteVigente(request.token(), true);
        String correo = normalizar(cliente.getCorreo());
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Este correo ya tiene una cuenta");
        }
        String dui = request.dui().trim();
        if (cliente.getDuiNit() != null && !cliente.getDuiNit().equals(dui)) {
            throw new ApiException(HttpStatus.CONFLICT, "El DUI no coincide con la ficha del cliente");
        }
        if (cliente.getDuiNit() == null && clientes.existsByDuiNit(dui)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese DUI ya está registrado en otro cliente");
        }
        Rol rol = roles.findByNombreIgnoreCase("CLIENTE").orElseGet(() -> {
            Rol nuevo = new Rol(); nuevo.setNombre("CLIENTE"); return roles.save(nuevo);
        });
        Usuario usuario = new Usuario();
        usuario.setNombre(cliente.getNombre());
        usuario.setCorreo(correo);
        usuario.setPassword(passwords.encode(request.password()));
        usuario.setActivo(true);
        // El enlace visible en modo local no demuestra posesión del buzón.
        usuario.setEmailVerificado("smtp".equalsIgnoreCase(delivery));
        usuario.setRol(rol);
        usuario = usuarios.saveAndFlush(usuario);
        cliente.setDuiNit(dui);
        cliente.setUsuario(usuario);
        usuario.setCliente(cliente);
        registroParticipantes.asegurar(cliente);
        return new AuthSession(jwtService.generarToken(usuario), jwtService.getExpirationSeconds(),
                sv.edu.udb.ucacfcconnect.dto.auth.UsuarioResponse.from(usuario));
    }

    private Cliente clienteVigente(String token, boolean bloquear) {
        try {
            Jwt jwt = decoder.decode(token);
            if (!"registro-cliente".equals(jwt.getClaimAsString("purpose")) ||
                    jwt.getSubject() == null || !jwt.getSubject().startsWith("invitacion:")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El enlace de invitación no es válido");
            }
            Long id = Long.valueOf(jwt.getSubject().substring("invitacion:".length()));
            Cliente cliente = (bloquear ? clientes.bloquear(id) : clientes.findById(id))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el cliente"));
            if (cliente.getUsuario() != null || !jwt.getClaimAsString("email").equals(normalizar(cliente.getCorreo()))) {
                throw new ApiException(HttpStatus.CONFLICT, "La invitación ya se usó o el correo cambió");
            }
            return cliente;
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El enlace de invitación caducó o no es válido");
        }
    }

    private String normalizar(String correo) {
        return correo == null || correo.isBlank() ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    public record InvitacionEnviada(String mensaje, String enlaceLocal) {}
    public record DatosInvitacion(String nombre, String correo, boolean tieneDui) {}
}
