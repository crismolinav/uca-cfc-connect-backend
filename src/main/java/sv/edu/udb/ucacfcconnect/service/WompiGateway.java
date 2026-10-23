package sv.edu.udb.ucacfcconnect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Cliente de Wompi para enlaces de pago usando el transporte TLS nativo del sistema. */
@Component
public class WompiGateway {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT; Windows NT 10.0; en-US) WindowsPowerShell/5.1.26100.7462";
    private final ObjectMapper json = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUrl;
    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public WompiGateway(
            @Value("${app.wompi.client-id:}") String clientId,
            @Value("${app.wompi.client-secret:}") String clientSecret,
            @Value("${app.wompi.redirect-url:}") String redirectUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
    }

    public boolean configurado() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public String crearEnlace(String referencia, BigDecimal monto, String curso, String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("identificadorEnlaceComercio", referencia);
        body.put("monto", monto);
        body.put("nombreProducto", "Curso UCA CFC");
        if (!redirectUrl.isBlank()) {
            Map<String, Object> configuracion = new LinkedHashMap<>();
            configuracion.put("urlRedirect", redirectUrl);
            String webhookUrl = redirectUrl.replace("/retorno", "/webhook");
            if (!webhookUrl.equals(redirectUrl)) configuracion.put("urlWebhook", webhookUrl);
            body.put("configuracion", configuracion);
        }
        Map<?, ?> result = postJson("https://api.wompi.sv/EnlacePago", token(), body);
        if (result == null || !(result.get("urlEnlace") instanceof String url) || url.isBlank()) {
            throw new IllegalStateException("Wompi no devolvió el enlace de pago.");
        }
        if (Boolean.TRUE.equals(result.get("estaProductivo"))) {
            throw new IllegalStateException("Wompi está en modo productivo. Este proyecto está configurado para pruebas locales.");
        }
        return url;
    }

    public Transaccion consultarTransaccion(String idTransaccion) {
        String response = curlRequest("GET", "https://api.wompi.sv/TransaccionCompra/" + idTransaccion,
                token(), null, null, false);
        Map<?, ?> data = parseJson(response);
        if (data == null) throw new IllegalStateException("Wompi no devolvió el resultado de la transacción.");
        Object amount = data.get("monto");
        Object id = data.get("idTransaccion");
        return new Transaccion(id == null ? null : id.toString(),
                amount == null ? null : new BigDecimal(amount.toString()),
                Boolean.TRUE.equals(data.get("esReal")), Boolean.TRUE.equals(data.get("esAprobada")));
    }

    private Map<?, ?> postJson(String url, String bearerToken, Object body) {
        try {
            return parseJson(curlRequest("POST", url, bearerToken, "application/json",
                    json.writeValueAsString(body), false));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo preparar la solicitud de Wompi.", ex);
        }
    }

    private String curlRequest(String method, String url, String bearerToken, String contentType,
                               String body, boolean putBodyInConfig) {
        Path requestBodyFile = null;
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder builder = new ProcessBuilder(os.contains("win") ? "curl.exe" : "curl", "--config", "-");
            builder.redirectErrorStream(true);

            StringBuilder config = new StringBuilder();
            config.append("url = \"").append(url).append("\"\n");
            config.append("silent\nshow-error\nmax-time = 30\n");
            config.append("write-out = \"\\n__WOMPI_HTTP__:%{http_code}\"\n");
            config.append("request = \"").append(method).append("\"\n");
            if (bearerToken != null && !bearerToken.isBlank()) {
                config.append("header = \"Authorization: Bearer ").append(bearerToken).append("\"\n");
            }
            config.append("header = \"User-Agent: ").append(USER_AGENT).append("\"\n");
            config.append("header = \"Accept: application/json\"\n");
            if (contentType != null) config.append("header = \"Content-Type: ").append(contentType).append("\"\n");
            if (body != null && putBodyInConfig) {
                config.append("data = \"").append(body).append("\"\n");
            } else if (body != null) {
                requestBodyFile = Files.createTempFile("wompi-request-", ".json");
                Files.writeString(requestBodyFile, body, StandardCharsets.UTF_8);
                String path = requestBodyFile.toAbsolutePath().toString().replace('\\', '/');
                config.append("data-binary = \"@").append(path).append("\"\n");
            }

            Process process = builder.start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(config.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!process.waitFor(35, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("La conexión con Wompi agotó el tiempo de espera.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String marker = "\n__WOMPI_HTTP__:";
            int markerIndex = output.lastIndexOf(marker);
            if (markerIndex < 0) {
                throw new IllegalStateException("No se pudo completar la solicitud de Wompi: " + output.trim());
            }
            String responseBody = output.substring(0, markerIndex).trim();
            String statusText = output.substring(markerIndex + marker.length()).trim();
            int status = Integer.parseInt(statusText);
            if (process.exitValue() != 0 || status < 200 || status >= 300) {
                throw new IllegalStateException("Wompi respondió HTTP " + status + ": " + responseBody);
            }
            return responseBody;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo ejecutar curl para conectar con Wompi. Verifica que curl esté instalado.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la conexión con Wompi.", ex);
        } finally {
            if (requestBodyFile != null) {
                try { Files.deleteIfExists(requestBodyFile); } catch (IOException ignored) { }
            }
        }
    }
    private Map<?, ?> parseJson(String response) {
        try {
            return json.readValue(response, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Wompi devolvió una respuesta que no es JSON.", ex);
        }
    }

    private synchronized String token() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) return accessToken;
        if (!configurado()) throw new IllegalStateException("Configura WOMPI_CLIENT_ID y WOMPI_CLIENT_SECRET en el archivo local .env.");
        String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&audience=" + URLEncoder.encode("wompi_api", StandardCharsets.UTF_8);
        Map<?, ?> response = parseJson(curlRequest("POST", "https://id.wompi.sv/connect/token",
                null, "application/x-www-form-urlencoded", form, true));
        if (response == null || !(response.get("access_token") instanceof String token)) {
            throw new IllegalStateException("No se pudo autenticar con Wompi. Revisa las credenciales de prueba.");
        }
        long expires = response.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        accessToken = token;
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expires - 60));
        return accessToken;
    }

    public record Transaccion(String idTransaccion, BigDecimal monto, boolean esReal, boolean esAprobada) {}
}