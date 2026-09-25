package sv.edu.udb.ucacfcconnect.service;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.ActividadDiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.DiplomadoDTO;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.DiplomadoRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiplomadoIntegrationTest {

    @Autowired
    private DiplomadoService diplomadoService;
    @Autowired
    private DiplomadoRepository diplomadoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ModalidadRepository modalidadRepository;
    @Autowired
    private Validator validator;

    private Long idCategoria;
    private Long idModalidad;
    private LocalDate fechaSesion;

    @BeforeEach
    void crearCatalogos() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Gestión empresarial");
        categoria.setDescripcion("Programas empresariales");
        idCategoria = categoriaRepository.save(categoria).getIdCategoria();

        Modalidad modalidad = new Modalidad();
        modalidad.setNombre("Presencial");
        modalidad.setDescripcion("Sesiones en el campus");
        idModalidad = modalidadRepository.save(modalidad).getIdModalidad();
        fechaSesion = LocalDate.now().plusDays(7);
    }

    @Test
    void publicaSoloCuandoLasSesionesCompletanLaDuracionDeclarada() {
        var creado = diplomadoService.crear(diplomadoValido("Diplomado en liderazgo", 4));
        assertFalse(creado.activo());
        assertTrue(diplomadoRepository.existsById(creado.idDiplomado()));

        var primera = diplomadoService.crearActividad(creado.idDiplomado(),
                sesion("Fundamentos", LocalTime.of(8, 0), LocalTime.of(10, 0)));

        ReglaNegocioException incompleto = assertThrows(ReglaNegocioException.class,
                () -> diplomadoService.cambiarEstado(creado.idDiplomado(), true));
        assertTrue(incompleto.getMessage().contains("exactamente 4 horas"));

        diplomadoService.crearActividad(creado.idDiplomado(),
                sesion("Aplicación", LocalTime.of(10, 0), LocalTime.of(12, 0)));
        assertTrue(diplomadoService.cambiarEstado(creado.idDiplomado(), true).activo());

        diplomadoService.actualizarActividad(creado.idDiplomado(), primera.idActividad(),
                sesion("Fundamentos", LocalTime.of(8, 0), LocalTime.of(9, 0)));
        assertFalse(diplomadoService.obtenerPorId(creado.idDiplomado()).activo());
    }

    @Test
    void rechazaSesionesQueExcedenHorasOSeCruzanConOtraActividad() {
        var primero = diplomadoService.crear(diplomadoValido("Diplomado en innovación", 2));
        diplomadoService.crearActividad(primero.idDiplomado(),
                sesion("Laboratorio", LocalTime.of(8, 0), LocalTime.of(10, 0)));

        ReglaNegocioException exceso = assertThrows(ReglaNegocioException.class,
                () -> diplomadoService.crearActividad(primero.idDiplomado(),
                        sesion("Sesión adicional", LocalTime.of(10, 0), LocalTime.of(11, 0))));
        assertTrue(exceso.getMessage().contains("supera la duración total"));

        var segundo = diplomadoService.crear(diplomadoValido("Diplomado en proyectos", 3));
        ConflictException conflicto = assertThrows(ConflictException.class,
                () -> diplomadoService.crearActividad(segundo.idDiplomado(),
                        sesion("Planificación", LocalTime.of(9, 0), LocalTime.of(11, 0))));
        assertTrue(conflicto.getMessage().contains("Conflicto de horario"));
    }

    @Test
    void validaCamposNumericosYObligatorios() {
        var camposInvalidos = validator.validate(new DiplomadoDTO(
                        "", "corta", 0, new BigDecimal("-1.555"), null, null, 0L, null
                )).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(camposInvalidos.contains("nombre"));
        assertTrue(camposInvalidos.contains("descripcion"));
        assertTrue(camposInvalidos.contains("duracionHoras"));
        assertTrue(camposInvalidos.contains("costo"));
        assertTrue(camposInvalidos.contains("fechaInicio"));
        assertTrue(camposInvalidos.contains("fechaFin"));
        assertTrue(camposInvalidos.contains("idCategoria"));
        assertTrue(camposInvalidos.contains("idModalidad"));
    }

    private DiplomadoDTO diplomadoValido(String nombre, int duracionHoras) {
        return new DiplomadoDTO(
                nombre,
                "Programa completo de formación profesional aplicada",
                duracionHoras,
                new BigDecimal("300.00"),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(15),
                idCategoria,
                idModalidad
        );
    }

    private ActividadDiplomadoDTO sesion(String titulo, LocalTime inicio, LocalTime fin) {
        return new ActividadDiplomadoDTO(titulo, fechaSesion, inicio, fin, 25);
    }
}
