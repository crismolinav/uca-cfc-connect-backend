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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sv.edu.udb.ucacfcconnect.dto.DocenteDTO;
import sv.edu.udb.ucacfcconnect.dto.DocenteResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.ErrorResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.PaginaDTO;
import sv.edu.udb.ucacfcconnect.service.DocenteService;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/v1/docentes")
@Tag(name = "Docentes", description = "Catálogo administrativo de docentes")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping
    @Operation(summary = "Listar y buscar docentes")
    public ResponseEntity<PaginaDTO<DocenteResponseDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "nombre") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direccion
    ) {
        return ResponseEntity.ok(docenteService.listar(texto, pagina, tamano, ordenarPor, direccion));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un docente")
    public ResponseEntity<DocenteResponseDTO> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(docenteService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Registrar un docente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Docente registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<DocenteResponseDTO> crear(@Valid @RequestBody DocenteDTO dto) {
        DocenteResponseDTO creado = docenteService.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.idDocente())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un docente")
    public ResponseEntity<DocenteResponseDTO> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody DocenteDTO dto
    ) {
        return ResponseEntity.ok(docenteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un docente no asignado")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        docenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
