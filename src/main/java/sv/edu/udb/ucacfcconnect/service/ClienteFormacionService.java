package sv.edu.udb.ucacfcconnect.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.InscripcionClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.InscripcionCursoRequest;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class ClienteFormacionService {
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final ParticipanteRepository participantes;
    private final InscripcionRepository inscripciones;
    private final CursoRepository cursos;

    public ClienteFormacionService(UsuarioRepository usuarios, ClienteRepository clientes,
                                   ParticipanteRepository participantes, InscripcionRepository inscripciones,
                                   CursoRepository cursos) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.participantes = participantes;
        this.inscripciones = inscripciones;
        this.cursos = cursos;
    }

    @Transactional
    public InscripcionClienteDTO inscribir(Long usuarioId, InscripcionCursoRequest request) {
        Usuario usuario = usuarioCliente(usuarioId);
        Cliente cliente = clientes.findByUsuarioId(usuarioId).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setUsuario(usuario);
            nuevo.setNombre(usuario.getNombre());
            nuevo.setCorreo(usuario.getCorreo());
            return clientes.save(nuevo);
        });
        String correo = usuario.getCorreo().trim().toLowerCase(Locale.ROOT);
        Participante participante = participantes.findFirstByClienteIdAndCorreoIgnoreCase(cliente.getId(), correo)
                .orElseGet(() -> {
                    Participante nuevo = new Participante();
                    nuevo.setCliente(cliente);
                    String[] partes = usuario.getNombre().trim().split("\\s+", 2);
                    nuevo.setNombre(partes[0]);
                    nuevo.setApellido(partes.length > 1 ? partes[1] : "Cliente");
                    nuevo.setCorreo(correo);
                    return participantes.save(nuevo);
                });

        Curso curso = cursos.bloquear(request.idCurso()).orElseThrow(() -> noEncontrado("curso"));
        if (!Boolean.TRUE.equals(curso.getActivo())) throw conflicto("El curso no está activo");
        if (inscripciones.countByCursoIdCursoAndEstadoNot(curso.getIdCurso(), "CANCELADA") >= curso.getCupoMaximo()) {
            throw conflicto("El curso ya alcanzó su cupo máximo");
        }
        if (inscripciones.existsByParticipanteIdAndCursoIdCurso(participante.getId(), curso.getIdCurso())) {
            throw conflicto("Ya estás inscrito en este curso");
        }

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setParticipante(participante);
        inscripcion.setCurso(curso);
        inscripcion.setFecha(LocalDate.now(ZONA));
        inscripcion.setEstado("PENDIENTE");
        inscripcion.setTotal(curso.getCosto());
        return vista(inscripciones.save(inscripcion));
    }

    @Transactional(readOnly = true)
    public List<InscripcionClienteDTO> inscripciones(Long usuarioId) {
        usuarioCliente(usuarioId);
        Cliente cliente = clientes.findByUsuarioId(usuarioId).orElse(null);
        if (cliente == null) return List.of();
        return inscripciones.findByParticipanteClienteIdOrderByFechaDescIdDesc(cliente.getId()).stream()
                .map(this::vista).toList();
    }

    private Usuario usuarioCliente(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo una cuenta de cliente puede inscribirse");
        }
        return usuario;
    }

    private InscripcionClienteDTO vista(Inscripcion inscripcion) {
        Curso curso = inscripcion.getCurso();
        return new InscripcionClienteDTO(inscripcion.getId(), curso == null ? null : curso.getIdCurso(),
                curso == null ? inscripcion.getDiplomado().getNombre() : curso.getTitulo(), inscripcion.getFecha(), inscripcion.getEstado(),
                inscripcion.getTotal());
    }

    private ApiException noEncontrado(String recurso) { return new ApiException(HttpStatus.NOT_FOUND, "No se encontró el " + recurso); }
    private ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
}
