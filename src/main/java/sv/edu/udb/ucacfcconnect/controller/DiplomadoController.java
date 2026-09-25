package sv.edu.udb.ucacfcconnect.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sv.edu.udb.ucacfcconnect.dto.ActividadDiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.ActividadResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.CatalogoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.DiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.DiplomadoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.ErrorResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.EstadoDiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.PaginaDTO;
import sv.edu.udb.ucacfcconnect.service.DiplomadoService;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/diplomados")
@Tag(name = "Diplomados", description = "Administración de diplomados y sus sesiones programadas")
public class DiplomadoController {

    private final DiplomadoService diplomadoService;

    public DiplomadoController(DiplomadoService diplomadoService) {
        this.diplomadoService = diplomadoService;
    }

    @GetMapping
    @Operation(summary = "Listar y buscar diplomados")
    public ResponseEntity<PaginaDTO<DiplomadoResponseDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @Positive Long idCategoria,
            @RequestParam(required = false) @Positive Long idModalidad,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "nombre") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direccion
    ) {
        return ResponseEntity.ok(diplomadoService.listar(
                texto, idCategoria, idModalidad, activo, pagina, tamano, ordenarPor, direccion
        ));
    }

    @GetMapping("/catalogos/categorias")
    @Operation(summary = "Listar categorías disponibles")
    public ResponseEntity<List<CatalogoResponseDTO>> listarCategorias() {
        return ResponseEntity.ok(diplomadoService.listarCategorias());
    }

    @GetMapping("/catalogos/modalidades")
    @Operation(summary = "Listar modalidades disponibles")
    public ResponseEntity<List<CatalogoResponseDTO>> listarModalidades() {
        return ResponseEntity.ok(diplomadoService.listarModalidades());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un diplomado")
    public ResponseEntity<DiplomadoResponseDTO> obtenerPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(diplomadoService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear un diplomado", description = "Se crea como borrador inactivo hasta completar sus sesiones.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Diplomado creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<DiplomadoResponseDTO> crear(@Valid @RequestBody DiplomadoDTO dto) {
        DiplomadoResponseDTO creado = diplomadoService.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.idDiplomado())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar completamente un diplomado")
    public ResponseEntity<DiplomadoResponseDTO> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody DiplomadoDTO dto
    ) {
        return ResponseEntity.ok(diplomadoService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Publicar o retirar un diplomado")
    public ResponseEntity<DiplomadoResponseDTO> cambiarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EstadoDiplomadoDTO estado
    ) {
        return ResponseEntity.ok(diplomadoService.cambiarEstado(id, estado.activo()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un diplomado")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        diplomadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/actividades")
    @Operation(summary = "Listar las sesiones de un diplomado")
    public ResponseEntity<List<ActividadResponseDTO>> listarActividades(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(diplomadoService.listarActividades(id));
    }

    @PostMapping("/{id}/actividades")
    @Operation(summary = "Programar una sesión", description = "Rechaza cruces con cualquier actividad institucional.")
    public ResponseEntity<ActividadResponseDTO> crearActividad(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ActividadDiplomadoDTO dto
    ) {
        ActividadResponseDTO creada = diplomadoService.crearActividad(id, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idActividad}")
                .buildAndExpand(creada.idActividad())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}/actividades/{idActividad}")
    @Operation(summary = "Actualizar una sesión programada")
    public ResponseEntity<ActividadResponseDTO> actualizarActividad(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long idActividad,
            @Valid @RequestBody ActividadDiplomadoDTO dto
    ) {
        return ResponseEntity.ok(diplomadoService.actualizarActividad(id, idActividad, dto));
    }

    @DeleteMapping("/{id}/actividades/{idActividad}")
    @Operation(summary = "Eliminar una sesión programada")
    public ResponseEntity<Void> eliminarActividad(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long idActividad
    ) {
        diplomadoService.eliminarActividad(id, idActividad);
        return ResponseEntity.noContent().build();
    }
}
