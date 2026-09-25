package sv.edu.udb.ucacfcconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoResponseDTO;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ModalidadRepository modalidadRepository;
    @InjectMocks
    private CursoService cursoService;

    private CursoDTO solicitud;
    private Categoria categoria;
    private Modalidad modalidad;

    @BeforeEach
    void configurar() {
        solicitud = new CursoDTO();
        solicitud.setTitulo("  Excel avanzado  ");
        solicitud.setDescripcion("  Curso práctico para empresas  ");
        solicitud.setDuracionHoras(32);
        solicitud.setCupoMaximo(25);
        solicitud.setCosto(new BigDecimal("125.00"));
        solicitud.setFechaInicio(LocalDate.of(2026, 10, 5));
        solicitud.setFechaFin(LocalDate.of(2026, 11, 5));
        solicitud.setHorario("  Lunes y miércoles 18:00-20:00  ");
        solicitud.setIdCategoria(1L);
        solicitud.setIdModalidad(2L);

        categoria = new Categoria();
        categoria.setIdCategoria(1L);
        categoria.setNombre("Tecnología");

        modalidad = new Modalidad();
        modalidad.setIdModalidad(2L);
        modalidad.setNombre("Virtual");
    }

    @Test
    void crearCursoGuardaRelacionesYDevuelveRespuesta() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(modalidadRepository.findById(2L)).thenReturn(Optional.of(modalidad));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
            Curso curso = invocation.getArgument(0);
            curso.setIdCurso(10L);
            return curso;
        });

        CursoResponseDTO respuesta = cursoService.crear(solicitud);

        assertEquals(10L, respuesta.idCurso());
        assertEquals("Excel avanzado", respuesta.titulo());
        assertEquals("Tecnología", respuesta.categoria());
        assertEquals("Virtual", respuesta.modalidad());
        assertTrue(respuesta.activo());
        verify(cursoRepository).save(any(Curso.class));
    }

    @Test
    void crearCursoRechazaFechaFinAnterior() {
        solicitud.setFechaFin(LocalDate.of(2026, 10, 4));

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> cursoService.crear(solicitud)
        );

        assertTrue(error.getMessage().contains("fecha de fin"));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void crearCursoRechazaCategoriaInexistente() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> cursoService.crear(solicitud));

        verify(modalidadRepository, never()).findById(any());
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoInactivaCursoExistente() {
        Curso curso = cursoExistente();
        when(cursoRepository.buscarPorId(10L)).thenReturn(Optional.of(curso));
        when(cursoRepository.save(curso)).thenReturn(curso);

        CursoResponseDTO respuesta = cursoService.cambiarEstado(10L, false);

        assertEquals(false, respuesta.activo());
        verify(cursoRepository).save(curso);
    }

    @Test
    void obtenerCursoInexistenteDevuelveError() {
        when(cursoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> cursoService.obtenerPorId(99L));
    }

    @Test
    void listarAplicaFiltrosPaginacionYOrdenamiento() {
        Curso curso = cursoExistente();
        when(cursoRepository.buscar(eq("excel"), eq(1L), eq(2L), eq(true), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(curso), invocation.getArgument(4), 1));

        var pagina = cursoService.listar("  excel  ", 1L, 2L, true, 0, 10, "costo", "desc");

        assertEquals(1, pagina.totalElementos());
        assertEquals("Excel avanzado", pagina.contenido().getFirst().titulo());
        verify(cursoRepository).buscar(eq("excel"), eq(1L), eq(2L), eq(true), any(Pageable.class));
    }

    @Test
    void listarCatalogosLosOrdenaPorNombre() {
        when(categoriaRepository.findAll(any(Sort.class))).thenReturn(List.of(categoria));
        when(modalidadRepository.findAll(any(Sort.class))).thenReturn(List.of(modalidad));

        assertEquals("Tecnología", cursoService.listarCategorias().getFirst().nombre());
        assertEquals("Virtual", cursoService.listarModalidades().getFirst().nombre());
    }

    @Test
    void actualizarCursoConservaEstadoYModificaDatos() {
        Curso curso = cursoExistente();
        when(cursoRepository.buscarPorId(10L)).thenReturn(Optional.of(curso));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(modalidadRepository.findById(2L)).thenReturn(Optional.of(modalidad));
        when(cursoRepository.save(curso)).thenReturn(curso);
        solicitud.setTitulo("Excel financiero");

        CursoResponseDTO respuesta = cursoService.actualizar(10L, solicitud);

        assertEquals("Excel financiero", respuesta.titulo());
        assertTrue(respuesta.activo());
    }

    @Test
    void eliminarCursoRelacionadoDevuelveConflicto() {
        Curso curso = cursoExistente();
        when(cursoRepository.buscarPorId(10L)).thenReturn(Optional.of(curso));
        doThrow(new DataIntegrityViolationException("foreign key"))
                .when(cursoRepository).flush();

        assertThrows(ConflictException.class, () -> cursoService.eliminar(10L));
    }

    private Curso cursoExistente() {
        Curso curso = new Curso();
        curso.setIdCurso(10L);
        curso.setTitulo("Excel avanzado");
        curso.setDescripcion("Curso práctico para empresas");
        curso.setDuracionHoras(32);
        curso.setCupoMaximo(25);
        curso.setCosto(new BigDecimal("125.00"));
        curso.setFechaInicio(LocalDate.of(2026, 10, 5));
        curso.setFechaFin(LocalDate.of(2026, 11, 5));
        curso.setHorario("Lunes y miércoles 18:00-20:00");
        curso.setActivo(true);
        curso.setCategoria(categoria);
        curso.setModalidad(modalidad);
        return curso;
    }
}
