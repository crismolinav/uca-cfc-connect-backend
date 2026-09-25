package sv.edu.udb.ucacfcconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.*;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CotizacionAlquilerIntegrationTest {
    @Autowired private CotizacionService cotizacionService;
    @Autowired private AlquilerService alquilerService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ModalidadRepository modalidadRepository;

    private Long idCliente;
    private Long idCurso;
    private Long idEspacio;

    @BeforeEach
    void prepararDatos() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Empresa de prueba");
        idCliente = clienteRepository.save(cliente).getId();

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría cotización");
        categoriaRepository.save(categoria);
        Modalidad modalidad = new Modalidad();
        modalidad.setNombre("Modalidad cotización");
        modalidadRepository.save(modalidad);

        Curso curso = new Curso();
        curso.setTitulo("Curso empresarial");
        curso.setDescripcion("Curso cotizable");
        curso.setDuracionHoras(8);
        curso.setCupoMaximo(20);
        curso.setCosto(new BigDecimal("100.00"));
        curso.setHorario("Sábado | 08:00-16:00");
        curso.setCategoria(categoria);
        curso.setModalidad(modalidad);
        curso.setActivo(true);
        idCurso = cursoRepository.save(curso).getIdCurso();

        idEspacio = alquilerService.crearEspacio(new EspacioDTO(
                "Auditorio de prueba", "AUDITORIO", 100, new BigDecimal("250.00"),
                "Proyector y sonido", new BigDecimal("4.00"), true
        )).idEspacio();
    }

    @Test
    void cotizacionCalculaElMontoConPreciosDelCatalogo() {
        CotizacionResponseDTO creada = cotizacionService.crear(new CotizacionDTO(
                idCliente, "Evento empresarial", List.of(
                new DetalleCotizacionDTO(idCurso, null, 2, "Dos grupos"),
                new DetalleCotizacionDTO(null, idEspacio, 1, "Auditorio")
        )));

        assertEquals("PENDIENTE", creada.estado());
        assertEquals(new BigDecimal("450.00"), creada.montoEstimado());
        assertEquals(2, creada.detalles().size());
        assertEquals("APROBADA", cotizacionService.cambiarEstado(creada.idCotizacion(), "aprobada").estado());
    }

    @Test
    void alquilerImpideReservarElMismoEspacioEnHorariosCruzados() {
        LocalDate fecha = LocalDate.now().plusDays(10);
        alquilerService.crearAlquiler(new AlquilerDTO(
                idCliente, idEspacio, fecha, LocalTime.of(8, 0), LocalTime.of(12, 0), "Capacitación"
        ));

        assertThrows(ConflictException.class, () -> alquilerService.crearAlquiler(new AlquilerDTO(
                idCliente, idEspacio, fecha, LocalTime.of(11, 0), LocalTime.of(13, 0), "Reunión"
        )));
    }

    @Test
    void alquilerRespetaLaDuracionMaximaDelEspacio() {
        LocalDate fecha = LocalDate.now().plusDays(12);

        assertThrows(ReglaNegocioException.class, () -> alquilerService.crearAlquiler(new AlquilerDTO(
                idCliente, idEspacio, fecha, LocalTime.of(8, 0), LocalTime.of(12, 30), "Evento extenso"
        )));
    }

    @Test
    void espaciosSePuedenBuscarPorCapacidadYDisponibilidad() {
        var pagina = alquilerService.listarEspacios(
                "auditorio", true, 80, 0, 10, "capacidad", "desc"
        );
        assertEquals(1, pagina.totalElementos());
        assertEquals(idEspacio, pagina.contenido().getFirst().idEspacio());
    }
}
