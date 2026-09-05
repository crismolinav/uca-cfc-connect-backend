package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;
import sv.edu.udb.ucacfcconnect.service.CursoService;

@RestController
@RequestMapping("/api/v1/cursos") // Prefijo base estandarizado
public class CursoController {

    private final CursoService cursoService;

    // Constructor para inyectar tu servicio automáticamente
    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    // GET /api/v1/cursos: Obtiene el catálogo de cursos
    @GetMapping
    public ResponseEntity<String> listarCursos() {
        // Todo: Implementar la lista real desde el servicio más adelante
        return ResponseEntity.ok("Lista de cursos (Simulación)");
    }

    // POST /api/v1/cursos: Crea un nuevo curso y lo guarda en MySQL
    @PostMapping
    public ResponseEntity<String> crearCurso(@Valid @RequestBody CursoDTO cursoDTO) {
        // Llamamos al servicio para guardar en la BD real
        cursoService.crearCurso(cursoDTO);
        return new ResponseEntity<>("¡Éxito! Curso guardado en MySQL: " + cursoDTO.getTitulo(), HttpStatus.CREATED);
    }

    // PUT /api/v1/cursos/{id}: Modifica un curso
    @PutMapping("/{id}")
    public ResponseEntity<String> actualizarCurso(@PathVariable Long id, @Valid @RequestBody CursoDTO cursoDTO) {
        return ResponseEntity.ok("Curso con ID " + id + " actualizado exitosamente");
    }

    // PATCH /api/v1/cursos/{id}/estado: Activa o inactiva un curso
    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstadoCurso(@PathVariable Long id, @RequestBody Boolean activo) {
        return ResponseEntity.ok("Estado del curso " + id + " cambiado a: " + activo);
    }
}