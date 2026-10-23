package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import sv.edu.udb.ucacfcconnect.dto.PagoClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.PagoInscripcionRequest;
import sv.edu.udb.ucacfcconnect.service.PagoClienteService;
import sv.edu.udb.ucacfcconnect.service.PagoWompiService;

import java.util.List;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/cliente/pagos")
public class PagoClienteController {
    private final PagoClienteService pagos;
    private final PagoWompiService wompi;
    @Value("${app.wompi.frontend-return-url:http://localhost:8080/cliente/aprendizaje.html}")
    private String wompiFrontendReturnUrl;

    public PagoClienteController(PagoClienteService pagos, PagoWompiService wompi) {
        this.pagos = pagos;
        this.wompi = wompi;
    }

    @GetMapping
    public List<PagoClienteDTO> listar(@AuthenticationPrincipal Jwt jwt) {
        return pagos.listar(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoClienteDTO registrar(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody PagoInscripcionRequest request) {
        return pagos.registrar(Long.valueOf(jwt.getSubject()), request);
    }

    @PostMapping("/wompi/iniciar")
    public java.util.Map<String, String> iniciarWompi(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody IniciarPagoWompiRequest request) {
        return java.util.Map.of("url", wompi.iniciar(Long.valueOf(jwt.getSubject()), request.idInscripcion()));
    }

    @GetMapping("/wompi/retorno")
    public ResponseEntity<Void> retornoWompi(
            @RequestParam(required = false) String identificadorEnlaceComercio,
            @RequestParam(required = false) String idTransaccion,
            @RequestParam(required = false) String idEnlace,
            @RequestParam(required = false) String monto,
            @RequestParam(required = false) String hash) {
        String resultado = wompi.procesarRetorno(identificadorEnlaceComercio, idTransaccion, idEnlace, monto, hash);
        String url = UriComponentsBuilder.fromUriString(wompiFrontendReturnUrl)
                .queryParam("pago", "wompi")
                .queryParam("estado", resultado)
                .build().encode().toUriString();
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
    }

    @PostMapping("/wompi/webhook")
    public ResponseEntity<Void> recibirWebhookWompi(
            @RequestBody String body,
            @RequestHeader(value = "wompi_hash", required = false) String hash) {
        return wompi.procesarWebhook(body, hash)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    public record IniciarPagoWompiRequest(@NotNull Long idInscripcion) {}
}
