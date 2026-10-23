package sv.edu.udb.ucacfcconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.Inscripcion;
import sv.edu.udb.ucacfcconnect.entity.Participante;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.InscripcionRepository;
import sv.edu.udb.ucacfcconnect.repository.ParticipanteRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteCertificadoServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock ClienteRepository clientes;
    @Mock ParticipanteRepository participantes;
    @Mock InscripcionRepository inscripciones;
    @Mock CursoRepository cursos;

    private ClienteFormacionService servicio;
    private Inscripcion inscripcion;

    @BeforeEach
    void configurar() {
        servicio = new ClienteFormacionService(usuarios, clientes, participantes, inscripciones, cursos);
        Rol rol = new Rol();
        rol.setNombre("CLIENTE");
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setRol(rol);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));

        Curso curso = new Curso();
        curso.setIdCurso(12L);
        curso.setTitulo("Introducción a Tecnología");
        curso.setContenidoPrograma("[{\"titulo\":\"Introducción\"},{\"titulo\":\"Práctica\"},{\"titulo\":\"Proyecto\"}]");
        Participante participante = new Participante();
        participante.setNombre("Ana");
        participante.setApellido("Pérez");
        inscripcion = new Inscripcion();
        inscripcion.setId(42L);
        inscripcion.setFecha(LocalDate.now());
        inscripcion.setEstado("CONFIRMADA");
        inscripcion.setTotal(new BigDecimal("20.00"));
        inscripcion.setCurso(curso);
        inscripcion.setParticipante(participante);
        when(inscripciones.buscarInscripcionCursoDeUsuario(7L, 12L)).thenReturn(List.of(inscripcion));
    }

    @Test
    void certificadoSoloTrasCompletarModulosDeInscripcionConfirmada() {
        assertThrows(ApiException.class, () -> servicio.certificado(7L, 12L));
        when(inscripciones.save(any(Inscripcion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.completarModulo(7L, 12L, 1);
        servicio.completarModulo(7L, 12L, 2);
        servicio.completarModulo(7L, 12L, 3);

        var certificado = servicio.certificado(7L, 12L);
        assertEquals("Ana Pérez", certificado.estudiante());
        assertEquals("Introducción a Tecnología", certificado.curso());
        assertNotNull(certificado.fechaFinalizacion());
        assertEquals("UCA-CFC-" + certificado.fechaFinalizacion().getYear() + "-42", certificado.codigo());
    }

    @Test
    void noPermiteProgresoNiCertificadoConPagoPendiente() {
        inscripcion.setEstado("PENDIENTE");
        assertThrows(ApiException.class, () -> servicio.completarModulo(7L, 12L, 1));
        assertThrows(ApiException.class, () -> servicio.certificado(7L, 12L));
    }
}
