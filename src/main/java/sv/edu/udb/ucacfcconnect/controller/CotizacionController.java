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
import sv.edu.udb.ucacfcconnect.dto.*;
import sv.edu.udb.ucacfcconnect.service.CotizacionService;
import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/v1/cotizaciones")
@Tag(name = "Cotizaciones", description = "Cotizaciones de cursos y alquiler de espacios")
public class CotizacionController {
    private final CotizacionService servicio;

    public CotizacionController(CotizacionService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Listar y buscar cotizaciones")
    public ResponseEntity<PaginaDTO<CotizacionResponseDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @Positive Long idCliente,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "fecha") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direccion) {
        return ResponseEntity.ok(servicio.listar(texto, estado, idCliente, pagina, tamano, ordenarPor, direccion));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una cotización con sus detalles")
    public ResponseEntity<CotizacionResponseDTO> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Solicitar una cotización")
    public ResponseEntity<CotizacionResponseDTO> crear(@Valid @RequestBody CotizacionDTO dto) {
        CotizacionResponseDTO creada = servicio.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.idCotizacion()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una cotización pendiente")
    public ResponseEntity<CotizacionResponseDTO> actualizar(@PathVariable @Positive Long id,
                                                             @Valid @RequestBody CotizacionDTO dto) {
        return ResponseEntity.ok(servicio.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de una cotización")
    public ResponseEntity<CotizacionResponseDTO> cambiarEstado(@PathVariable @Positive Long id,
                                                                @Valid @RequestBody EstadoCotizacionDTO dto) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, dto.estado()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una cotización")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
