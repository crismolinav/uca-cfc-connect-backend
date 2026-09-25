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
import sv.edu.udb.ucacfcconnect.service.AlquilerService;
import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/v1/espacios")
@Tag(name = "Espacios", description = "Catálogo de espacios físicos disponibles para alquiler")
public class EspacioController {
    private final AlquilerService servicio;
    public EspacioController(AlquilerService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Listar y buscar espacios")
    public ResponseEntity<PaginaDTO<EspacioResponseDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(required = false) @Positive Integer capacidadMinima,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "nombre") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direccion) {
        return ResponseEntity.ok(servicio.listarEspacios(texto, disponible, capacidadMinima,
                pagina, tamano, ordenarPor, direccion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspacioResponseDTO> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.obtenerEspacio(id));
    }

    @PostMapping
    public ResponseEntity<EspacioResponseDTO> crear(@Valid @RequestBody EspacioDTO dto) {
        EspacioResponseDTO creado = servicio.crearEspacio(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.idEspacio()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EspacioResponseDTO> actualizar(@PathVariable @Positive Long id,
                                                          @Valid @RequestBody EspacioDTO dto) {
        return ResponseEntity.ok(servicio.actualizarEspacio(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        servicio.eliminarEspacio(id);
        return ResponseEntity.noContent().build();
    }
}
