package sv.edu.udb.ucacfcconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.ActividadDiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.AsignacionDocentesDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;
import sv.edu.udb.ucacfcconnect.dto.DiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.DocenteDTO;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocenteIntegrationTest {

    @Autowired private DocenteService docenteService;
    @Autowired private DocenteAsignacionService asignacionService;
    @Autowired private CursoService cursoService;
    @Autowired private DiplomadoService diplomadoService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ModalidadRepository modalidadRepository;

    private Long idCategoria;
    private Long idModalidad;
    private LocalDate proximoLunes;

    @BeforeEach
    void prepararCatalogos() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Tecnología docente");
        categoria.setDescripcion("Categoría para pruebas de docentes");
        idCategoria = categoriaRepository.save(categoria).getIdCategoria();

        Modalidad modalidad = new Modalidad();
        modalidad.setNombre("Presencial docente");
        modalidad.setDescripcion("Modalidad para pruebas de docentes");
        idModalidad = modalidadRepository.save(modalidad).getIdModalidad();

        proximoLunes = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    @Test
    void registraBuscaActualizaYProtegeElCorreo() {
        var docente = docenteService.crear(docente("Ana Martínez", "ana@uca.edu.sv"));
        assertEquals("ana@uca.edu.sv", docente.correo());
        assertEquals(1, docenteService.listar("Analítica", 0, 10, "nombre", "asc").totalElementos());

        var actualizado = docenteService.actualizar(
                docente.idDocente(), docente("Ana María Martínez", "ana@uca.edu.sv")
        );
        assertEquals("Ana María Martínez", actualizado.nombre());

        assertThrows(ConflictException.class,
                () -> docenteService.crear(docente("Otra docente", "ana@uca.edu.sv")));
    }

    @Test
    void asignaDocenteYRechazaCruceEntreCursos() {
        var docente = docenteService.crear(docente("Carlos López", "carlos@uca.edu.sv"));
        var primero = cursoService.crear(curso("Java empresarial", "Lunes | 09:00-11:00"));
        var segundo = cursoService.crear(curso("Bases de datos", "Lunes | 10:00-12:00"));

        var asignados = asignacionService.asignarCurso(
                primero.idCurso(), new AsignacionDocentesDTO(List.of(docente.idDocente()))
        );
        assertEquals("Carlos López", asignados.getFirst().nombre());

        ConflictException conflicto = assertThrows(ConflictException.class,
                () -> asignacionService.asignarCurso(
                        segundo.idCurso(), new AsignacionDocentesDTO(List.of(docente.idDocente()))
                ));
        assertTrue(conflicto.getMessage().contains("Java empresarial"));
        assertThrows(ConflictException.class, () -> docenteService.eliminar(docente.idDocente()));
    }

    @Test
    void rechazaSesionDeDiplomadoQueCruzaElCursoDelDocente() {
        var docente = docenteService.crear(docente("Lucía Pérez", "lucia@uca.edu.sv"));
        var curso = cursoService.crear(curso("Gestión ágil", "Lunes | 09:00-11:00"));
        asignacionService.asignarCurso(
                curso.idCurso(), new AsignacionDocentesDTO(List.of(docente.idDocente()))
        );

        var diplomado = diplomadoService.crear(new DiplomadoDTO(
                "Diplomado en innovación",
                "Programa profesional para desarrollar proyectos de innovación",
                2,
                new BigDecimal("250.00"),
                proximoLunes,
                proximoLunes,
                idCategoria,
                idModalidad
        ));
        asignacionService.asignarDiplomado(
                diplomado.idDiplomado(), new AsignacionDocentesDTO(List.of(docente.idDocente()))
        );

        ConflictException conflicto = assertThrows(ConflictException.class,
                () -> diplomadoService.crearActividad(
                        diplomado.idDiplomado(),
                        new ActividadDiplomadoDTO(
                                "Taller aplicado", proximoLunes,
                                LocalTime.of(10, 0), LocalTime.of(12, 0), 20
                        )
                ));
        assertTrue(conflicto.getMessage().contains("Gestión ágil"));
    }

    private DocenteDTO docente(String nombre, String correo) {
        return new DocenteDTO(nombre, "Analítica de datos", correo, "+503 2222-0000");
    }

    private CursoDTO curso(String titulo, String horario) {
        CursoDTO dto = new CursoDTO();
        dto.setTitulo(titulo);
        dto.setDescripcion("Curso profesional con aplicación práctica y ejercicios");
        dto.setDuracionHoras(2);
        dto.setCupoMaximo(25);
        dto.setCosto(new BigDecimal("100.00"));
        dto.setFechaInicio(proximoLunes);
        dto.setFechaFin(proximoLunes);
        dto.setHorario(horario);
        dto.setIdCategoria(idCategoria);
        dto.setIdModalidad(idModalidad);
        return dto;
    }
}
