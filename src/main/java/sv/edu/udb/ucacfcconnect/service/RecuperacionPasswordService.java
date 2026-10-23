package sv.edu.udb.ucacfcconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sv.edu.udb.ucacfcconnect.dto.auth.RestablecerPasswordRequest;
import sv.edu.udb.ucacfcconnect.entity.RecuperacionPassword;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.RecuperacionPasswordRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class RecuperacionPasswordService {
    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionPasswordService.class);
    private static final Duration VIGENCIA = Duration.ofMinutes(20);
    private static final Duration INTERVALO = Duration.ofMinutes(2);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final RecuperacionPasswordRepository recuperaciones;
    private final PasswordEncoder passwords;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final TaskExecutor mailExecutor;
    private final String mailUsername;
    private final String baseUrl;

    public RecuperacionPasswordService(UsuarioRepository usuarios,
                                      RecuperacionPasswordRepository recuperaciones,
                                      PasswordEncoder passwords,
                                      ObjectProvider<JavaMailSender> mailSender,
                                      @Qualifier("recuperacionMailExecutor") TaskExecutor mailExecutor,
                                      @Value("${spring.mail.username:}") String mailUsername,
                                      @Value("${app.password-reset.base-url:http://localhost:8080}") String baseUrl) {
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.passwords = passwords;
        this.mailSender = mailSender;
        this.mailExecutor = mailExecutor;
        this.mailUsername = mailUsername;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void solicitar(String correo) {
        // La respuesta no depende de si existe una cuenta, si está activa o si usa Google.
        if (mailUsername.isBlank() || mailSender.getIfAvailable() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La recuperación por correo no está configurada. Contacta al administrador");
        }
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (usuario == null || !usuario.isActivo() || usuario.getPassword() == null) return;

        Instant ahora = Instant.now();
        RecuperacionPassword anterior = recuperaciones.findTopByUsuarioIdOrderByCreadoEnDesc(usuario.getId()).orElse(null);
        if (anterior != null && anterior.getCreadoEn().plus(INTERVALO).isAfter(ahora)) return;

        byte[] aleatorio = new byte[32];
        RANDOM.nextBytes(aleatorio);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        RecuperacionPassword recuperacion = new RecuperacionPassword();
        recuperacion.setUsuario(usuario);
        recuperacion.setTokenHash(hash(token));
        recuperacion.setCreadoEn(ahora);
        recuperacion.setExpiraEn(ahora.plus(VIGENCIA));
        recuperacion.setPasswordVersion(usuario.getPasswordVersion());
        recuperaciones.save(recuperacion);

        String destinatario = usuario.getCorreo();
        String enlace = baseUrl.replaceAll("/+$", "") + "/auth/restablecer.html#token=" + token;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mailExecutor.execute(() -> enviar(destinatario, enlace));
                } catch (RuntimeException ex) {
                    LOG.error("No se pudo programar un correo de recuperación", ex);
                }
            }
        });
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest request) {
        RecuperacionPassword recuperacion = recuperaciones.findByTokenHash(hash(request.token()))
                .orElseThrow(this::enlaceInvalido);
        Usuario usuario = recuperacion.getUsuario();
        if (recuperacion.getUsadoEn() != null || !recuperacion.getExpiraEn().isAfter(Instant.now())
                || !usuario.isActivo() || usuario.getPassword() == null
                || usuario.getPasswordVersion() != recuperacion.getPasswordVersion()) {
            throw enlaceInvalido();
        }
        usuario.setPassword(passwords.encode(request.password()));
        usuario.setPasswordVersion(usuario.getPasswordVersion() + 1);
        usuario.setEmailVerificado(true);
        recuperacion.setUsadoEn(Instant.now());
        usuarios.save(usuario);
        recuperaciones.save(recuperacion);
    }

    private void enviar(String correo, String enlace) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(mailUsername);
            mensaje.setTo(correo);
            mensaje.setSubject("Restablece tu contraseña de UCA CFC Connect");
            mensaje.setText("Recibimos una solicitud para restablecer tu contraseña. "
                    + "Abre este enlace dentro de los próximos 20 minutos:\n\n"
                    + enlace + "\n\nSi no lo solicitaste, ignora este correo. Tu contraseña no cambiará.");
            mailSender.getObject().send(mensaje);
        } catch (RuntimeException ex) {
            // Nunca registrar el enlace o el token: solo el fallo operativo.
            LOG.error("No se pudo enviar un correo de recuperación", ex);
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    private ApiException enlaceInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El enlace caducó o ya fue utilizado. Solicita uno nuevo");
    }
}
