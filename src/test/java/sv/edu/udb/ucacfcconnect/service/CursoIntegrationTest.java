package sv.edu.udb.ucacfcconnect.service;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoResponseDTO;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CursoIntegrationTest {

    @Autowired
    private CursoService cursoService;
    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ModalidadRepository modalidadRepository;
    @Autowired
    private Validator validator;

    private Long idCategoria;
    private Long idModalidad;

    @BeforeEach
    void crearCatalogos() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Tecnología");
        categoria.setDescripcion("Cursos tecnológicos");
        idCategoria = categoriaRepository.save(categoria).getIdCategoria();

        Modalidad modalidad = new Modalidad();
        modalidad.setNombre("Virtual");
        modalidad.setDescripcion("Sesiones en línea");
        idModalidad = modalidadRepository.save(modalidad).getIdModalidad();
    }

    @Test
    void crudBusquedaYEstadoFuncionanConPersistenciaJpa() {
        CursoDTO solicitud = cursoValido();

        CursoResponseDTO creado = cursoService.crear(solicitud);
        assertTrue(cursoRepository.existsById(creado.idCurso()));

        var pagina = cursoService.listar(
                "excel", idCategoria, idModalidad, true, 0, 10, "costo", "desc"
        );
        assertEquals(1, pagina.totalElementos());
        assertEquals(creado.idCurso(), pagina.contenido().getFirst().idCurso());

        solicitud.setTitulo("Excel financiero actualizado");
        CursoResponseDTO actualizado = cursoService.actualizar(creado.idCurso(), solicitud);
        assertEquals("Excel financiero actualizado", actualizado.titulo());

        CursoResponseDTO inactivo = cursoService.cambiarEstado(creado.idCurso(), false);
        assertFalse(inactivo.activo());

        cursoService.eliminar(creado.idCurso());
        assertFalse(cursoRepository.existsById(creado.idCurso()));
    }

    @Test
    void dtoRechazaCamposObligatoriosAusentes() {
        var camposInvalidos = validator.validate(new CursoDTO()).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(camposInvalidos.contains("titulo"));
        assertTrue(camposInvalidos.contains("descripcion"));
        assertTrue(camposInvalidos.contains("horario"));
        assertTrue(camposInvalidos.contains("cupoMaximo"));
        assertTrue(camposInvalidos.contains("costo"));
        assertTrue(camposInvalidos.contains("fechaInicio"));
        assertTrue(camposInvalidos.contains("fechaFin"));
        assertTrue(camposInvalidos.contains("duracionHoras"));
        assertTrue(camposInvalidos.contains("idCategoria"));
        assertTrue(camposInvalidos.contains("idModalidad"));
    }

    @Test
    void dtoRechazaCuposCostosYHorarioInvalidos() {
        CursoDTO dto = cursoValido();
        dto.setCupoMaximo(-10);
        dto.setDuracionHoras(0);
        dto.setCosto(new BigDecimal("-10.555"));
        dto.setHorario("cuando se pueda");

        var camposInvalidos = validator.validate(dto).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(camposInvalidos.contains("cupoMaximo"));
        assertTrue(camposInvalidos.contains("duracionHoras"));
        assertTrue(camposInvalidos.contains("costo"));
        assertTrue(camposInvalidos.contains("horario"));
    }

    private CursoDTO cursoValido() {
        CursoDTO dto = new CursoDTO();
        dto.setTitulo("Excel avanzado para negocios");
        dto.setDescripcion("Curso práctico de análisis de datos empresariales");
        dto.setDuracionHoras(32);
        dto.setCupoMaximo(25);
        dto.setCosto(new BigDecimal("125.00"));
        dto.setFechaInicio(LocalDate.of(2026, 10, 5));
        dto.setFechaFin(LocalDate.of(2026, 11, 5));
        dto.setHorario("Lunes, Miércoles | 18:00-20:00");
        dto.setIdCategoria(idCategoria);
        dto.setIdModalidad(idModalidad);
        return dto;
    }
}
