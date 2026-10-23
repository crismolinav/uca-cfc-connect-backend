package sv.edu.udb.ucacfcconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Inscripcion;
import sv.edu.udb.ucacfcconnect.entity.Pago;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.InscripcionRepository;
import sv.edu.udb.ucacfcconnect.repository.PagoRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PagoWompiService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PagoWompiService.class);
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final InscripcionRepository inscripciones;
    private final PagoRepository pagos;
    private final WompiGateway wompi;
    private final String apiSecret;
    private final boolean allowTestPayments;
    private final boolean allowProduction;

    public PagoWompiService(UsuarioRepository usuarios, ClienteRepository clientes,
                            InscripcionRepository inscripciones, PagoRepository pagos,
                            WompiGateway wompi,
                            @Value("${app.wompi.client-secret:}") String apiSecret,
                            @Value("${app.wompi.allow-test-payments:true}") boolean allowTestPayments,
                            @Value("${app.wompi.allow-production:false}") boolean allowProduction) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.inscripciones = inscripciones;
        this.pagos = pagos;
        this.wompi = wompi;
        this.apiSecret = apiSecret;
        this.allowTestPayments = allowTestPayments;
        this.allowProduction = allowProduction;
    }

    @Transactional
    public String iniciar(Long usuarioId, Long inscripcionId) {
        if (!wompi.configurado()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configura las credenciales de Wompi en .env para iniciar pagos de prueba.");
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo una cuenta de cliente puede pagar una inscripción");
        }
        Cliente cliente = clientes.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No se encontró tu perfil de cliente"));
        Inscripcion inscripcion = inscripciones.bloquear(inscripcionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró la inscripción"));
        if (inscripcion.getParticipante() == null || inscripcion.getParticipante().getCliente() == null
                || !cliente.getId().equals(inscripcion.getParticipante().getCliente().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La inscripción no pertenece a tu cuenta");
        }
        if (inscripcion.getCurso() == null || !"PENDIENTE".equals(inscripcion.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo puedes pagar cursos con inscripción pendiente");
        }
        if (pagos.existsByInscripcionIdAndEstadoIn(inscripcionId, List.of("PENDIENTE", "CONFIRMADO"))) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya hay un pago pendiente o confirmado para este curso");
        }
        String reference = "WOMPI-" + UUID.randomUUID();
        String email = usuario.getCorreo();
        try {
            String url = wompi.crearEnlace(reference, inscripcion.getTotal(), inscripcion.getCurso().getTitulo(), email);
            Pago pago = new Pago();
            pago.setCliente(cliente);
            pago.setInscripcion(inscripcion);
            pago.setMonto(inscripcion.getTotal());
            pago.setFecha(LocalDate.now(ZONA));
            pago.setMetodo("TARJETA");
            pago.setReferencia(reference);
            pago.setEstado("PENDIENTE");
            pagos.save(pago);
            return url;
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.error("Fallo al iniciar el pago Wompi para la inscripción {}.", inscripcionId, ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "No se pudo iniciar el pago con Wompi. Revisa las credenciales y el modo de pruebas del negocio.");
        }
    }

    @Transactional
    public String procesarRetorno(String identificador, String idTransaccion, String idEnlace,
                                  String monto, String hash) {
        if (!hashValido(identificador, idTransaccion, idEnlace, monto, hash)) return "pendiente";
        Pago pago = pagos.findByReferencia(identificador).orElse(null);
        if (pago == null || pago.getInscripcion() == null) return "pendiente";
        if ("CONFIRMADO".equals(pago.getEstado())) return "confirmado";
        if (!"PENDIENTE".equals(pago.getEstado())) return "rechazado";

        WompiGateway.Transaccion transaccion;
        try {
            transaccion = wompi.consultarTransaccion(idTransaccion);
        } catch (RuntimeException ex) {
            return "pendiente";
        }
        if (transaccion.idTransaccion() == null || !idTransaccion.equals(transaccion.idTransaccion())
                || transaccion.monto() == null || transaccion.monto().compareTo(pago.getMonto()) != 0
                || new BigDecimal(monto).compareTo(pago.getMonto()) != 0) return "pendiente";
        if (!transaccion.esReal() && !allowTestPayments) return "pendiente";
        if (transaccion.esReal() && !allowProduction) return "pendiente";

        return registrarResultado(pago, idTransaccion, transaccion.esAprobada());
    }

    private String registrarResultado(Pago pago, String idTransaccion, boolean aprobado) {
        if (pago == null || pago.getInscripcion() == null) return "pendiente";
        if (!"PENDIENTE".equals(pago.getEstado())) return "CONFIRMADO".equals(pago.getEstado()) ? "confirmado" : "rechazado";
        if (!aprobado) {
            pago.setEstado("RECHAZADO");
            return "rechazado";
        }
        pago.setEstado("CONFIRMADO");
        pago.getInscripcion().setEstado("CONFIRMADA");
        return "confirmado";
    }

    @Transactional
    public boolean procesarWebhook(String body, String recibido) {
        if (body == null || recibido == null || apiSecret.isBlank() || !webhookHashValido(body, recibido)) return false;
        try {
            Map<?, ?> evento = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class);
            Object enlaceValue = valor(evento, "EnlacePago");
            if (!(enlaceValue instanceof Map<?, ?> enlace)) return false;
            String referencia = texto(valor(enlace, "IdentificadorEnlaceComercio"));
            String idTransaccion = texto(valor(evento, "IdTransaccion"));
            String montoTexto = texto(valor(evento, "Monto"));
            if (referencia == null || idTransaccion == null || montoTexto == null) return false;
            Pago pago = pagos.findByReferencia(referencia).orElse(null);
            if (pago == null || pago.getInscripcion() == null) return false;
            if ("CONFIRMADO".equals(pago.getEstado())) return true;
            if (!"PENDIENTE".equals(pago.getEstado())) return true;
            WompiGateway.Transaccion transaccion = wompi.consultarTransaccion(idTransaccion);
            BigDecimal monto = new BigDecimal(montoTexto);
            if (!idTransaccion.equals(transaccion.idTransaccion()) || transaccion.monto() == null
                    || transaccion.monto().compareTo(pago.getMonto()) != 0
                    || monto.compareTo(pago.getMonto()) != 0
                    || (!transaccion.esReal() && !allowTestPayments)
                    || (transaccion.esReal() && !allowProduction)) return false;
            registrarResultado(pago, idTransaccion, transaccion.esAprobada());
            return true;
        } catch (Exception ex) {
            LOGGER.warn("No se pudo validar el webhook de Wompi.");
            return false;
        }
    }

    private boolean webhookHashValido(String body, String recibido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] provisto = HexFormat.of().parseHex(recibido.trim());
            return MessageDigest.isEqual(esperado, provisto);
        } catch (Exception ex) {
            return false;
        }
    }

    private Object valor(Map<?, ?> map, String key) {
        return map.entrySet().stream()
                .filter(entry -> key.equalsIgnoreCase(String.valueOf(entry.getKey())))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private String texto(Object value) {
        return value == null ? null : value.toString();
    }
    private boolean hashValido(String identificador, String idTransaccion, String idEnlace,
                               String monto, String recibido) {
        if (apiSecret.isBlank() || identificador == null || !(identificador.startsWith("WOMPI-") || identificador.startsWith("WOMPI:"))
                || idTransaccion == null || idEnlace == null || monto == null || recibido == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal((identificador + idTransaccion + idEnlace + monto)
                    .getBytes(StandardCharsets.UTF_8));
            byte[] provisto = HexFormat.of().parseHex(recibido);
            return MessageDigest.isEqual(esperado, provisto);
        } catch (Exception ex) {
            return false;
        }
    }
}
