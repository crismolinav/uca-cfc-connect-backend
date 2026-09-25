package sv.edu.udb.ucacfcconnect.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import sv.edu.udb.ucacfcconnect.dto.CatalogoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.AsignacionDocentesDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.DocenteResumenDTO;
import sv.edu.udb.ucacfcconnect.dto.ErrorResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.EstadoCursoDTO;
import sv.edu.udb.ucacfcconnect.dto.PaginaDTO;
import sv.edu.udb.ucacfcconnect.service.CursoService;
import sv.edu.udb.ucacfcconnect.service.DocenteAsignacionService;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/cursos")
@Tag(name = "Cursos", description = "Administración completa de la oferta de cursos")
public class CursoController {

    private final CursoService cursoService;
    private final DocenteAsignacionService docenteAsignacionService;

    public CursoController(CursoService cursoService, DocenteAsignacionService docenteAsignacionService) {
        this.cursoService = cursoService;
        this.docenteAsignacionService = docenteAsignacionService;
    }

    @GetMapping
    @Operation(
            summary = "Listar y buscar cursos",
            description = "Permite filtrar por texto, categoría, modalidad y estado, con paginación y ordenamiento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de cursos obtenida correctamente",
                    content = @Content(schema = @Schema(implementation = PaginaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de búsqueda inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PaginaDTO<CursoResponseDTO>> listar(
            @Parameter(description = "Texto contenido en el título o descripción")
            @RequestParam(required = false) String texto,
            @Parameter(description = "Identificador de la categoría")
            @RequestParam(required = false) @Positive Long idCategoria,
            @Parameter(description = "Identificador de la modalidad")
            @RequestParam(required = false) @Positive Long idModalidad,
            @Parameter(description = "Estado activo o inactivo")
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @Parameter(description = "Campo de ordenamiento", example = "titulo")
            @RequestParam(defaultValue = "titulo") String ordenarPor,
            @Parameter(description = "Dirección: asc o desc", example = "asc")
            @RequestParam(defaultValue = "asc") String direccion
    ) {
        return ResponseEntity.ok(cursoService.listar(
                texto, idCategoria, idModalidad, activo, pagina, tamano, ordenarPor, direccion
        ));
    }

    @GetMapping("/catalogos/categorias")
    @Operation(summary = "Listar categorías disponibles", description = "Devuelve los identificadores válidos para crear o actualizar un curso.")
    @ApiResponse(responseCode = "200", description = "Categorías ordenadas por nombre")
    public ResponseEntity<List<CatalogoResponseDTO>> listarCategorias() {
        return ResponseEntity.ok(cursoService.listarCategorias());
    }

    @GetMapping("/catalogos/modalidades")
    @Operation(summary = "Listar modalidades disponibles", description = "Devuelve los identificadores válidos para crear o actualizar un curso.")
    @ApiResponse(responseCode = "200", description = "Modalidades ordenadas por nombre")
    public ResponseEntity<List<CatalogoResponseDTO>> listarModalidades() {
        return ResponseEntity.ok(cursoService.listarModalidades());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un curso por identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curso encontrado",
                    content = @Content(schema = @Schema(implementation = CursoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CursoResponseDTO> obtenerPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(cursoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/docentes")
    @Operation(summary = "Listar docentes asignados al curso")
    public ResponseEntity<List<DocenteResumenDTO>> listarDocentes(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(docenteAsignacionService.listarCurso(id));
    }

    @PutMapping("/{id}/docentes")
    @Operation(
            summary = "Asignar docentes al curso",
            description = "Reemplaza la asignación actual y rechaza conflictos de fecha y horario."
    )
    public ResponseEntity<List<DocenteResumenDTO>> asignarDocentes(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AsignacionDocentesDTO solicitud
    ) {
        return ResponseEntity.ok(docenteAsignacionService.asignarCurso(id, solicitud));
    }

    @PostMapping
    @Operation(summary = "Crear un curso", description = "El curso se crea con estado activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Curso creado",
                    content = @Content(schema = @Schema(implementation = CursoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Categoría o modalidad inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CursoResponseDTO> crear(@Valid @RequestBody CursoDTO cursoDTO) {
        CursoResponseDTO creado = cursoService.crear(cursoDTO);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.idCurso())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar completamente un curso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curso actualizado",
                    content = @Content(schema = @Schema(implementation = CursoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Curso, categoría o modalidad inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CursoResponseDTO> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CursoDTO cursoDTO
    ) {
        return ResponseEntity.ok(cursoService.actualizar(id, cursoDTO));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar un curso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(implementation = CursoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CursoResponseDTO> cambiarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EstadoCursoDTO estado
    ) {
        return ResponseEntity.ok(cursoService.cambiarEstado(id, estado.activo()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un curso")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Curso eliminado"),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "El curso posee registros relacionados",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
