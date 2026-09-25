package sv.edu.udb.ucacfcconnect.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import sv.edu.udb.ucacfcconnect.dto.*;
import sv.edu.udb.ucacfcconnect.service.AlquilerService;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/alquileres")
@Tag(name = "Alquileres", description = "Reservas y administración del alquiler de espacios")
public class AlquilerController {
    private final AlquilerService servicio;
    public AlquilerController(AlquilerService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Listar y buscar alquileres")
    public ResponseEntity<PaginaDTO<AlquilerResponseDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @Positive Long idEspacio,
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "fecha") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direccion) {
        return ResponseEntity.ok(servicio.listarAlquileres(texto, estado, idEspacio, fecha,
                pagina, tamano, ordenarPor, direccion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlquilerResponseDTO> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.obtenerAlquiler(id));
    }

    @GetMapping("/mios")
    @Operation(summary = "Consultar los alquileres del cliente autenticado")
    public ResponseEntity<List<AlquilerResponseDTO>> listarMios(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(servicio.listarAlquileresDelUsuario(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/solicitudes")
    @Operation(summary = "Solicitar un espacio para el cliente autenticado")
    public ResponseEntity<AlquilerResponseDTO> solicitar(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody SolicitudAlquilerDTO dto) {
        AlquilerResponseDTO creado = servicio.solicitarAlquiler(Long.valueOf(jwt.getSubject()), dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/alquileres/{id}")
                .buildAndExpand(creado.idAlquiler()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una reserva propia")
    public ResponseEntity<AlquilerResponseDTO> cancelar(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.cancelarAlquilerDelUsuario(Long.valueOf(jwt.getSubject()), id));
    }

    @PostMapping
    @Operation(summary = "Solicitar el alquiler de un espacio")
    public ResponseEntity<AlquilerResponseDTO> crear(@Valid @RequestBody AlquilerDTO dto) {
        AlquilerResponseDTO creado = servicio.crearAlquiler(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.idAlquiler()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlquilerResponseDTO> actualizar(@PathVariable @Positive Long id,
                                                           @Valid @RequestBody AlquilerDTO dto) {
        return ResponseEntity.ok(servicio.actualizarAlquiler(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<AlquilerResponseDTO> cambiarEstado(@PathVariable @Positive Long id,
                                                              @Valid @RequestBody EstadoAlquilerDTO dto) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, dto.estado()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        servicio.eliminarAlquiler(id);
        return ResponseEntity.noContent().build();
    }
}
