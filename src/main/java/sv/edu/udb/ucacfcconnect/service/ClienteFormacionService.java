package sv.edu.udb.ucacfcconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.InscripcionClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.InscripcionCursoRequest;
import sv.edu.udb.ucacfcconnect.dto.CertificadoCursoDTO;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TreeSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ClienteFormacionService {
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final ParticipanteRepository participantes;
    private final InscripcionRepository inscripciones;
    private final CursoRepository cursos;
    private final DiplomadoRepository diplomados;
    private final ResenaCursoRepository resenas;

    @Autowired
    public ClienteFormacionService(UsuarioRepository usuarios, ClienteRepository clientes,
                                   ParticipanteRepository participantes, InscripcionRepository inscripciones,
                                   CursoRepository cursos, DiplomadoRepository diplomados, ResenaCursoRepository resenas) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.participantes = participantes;
        this.inscripciones = inscripciones;
        this.cursos = cursos;
        this.diplomados = diplomados;
        this.resenas = resenas;
    }

    public ClienteFormacionService(UsuarioRepository usuarios, ClienteRepository clientes,
                                   ParticipanteRepository participantes, InscripcionRepository inscripciones,
                                   CursoRepository cursos) {
        this(usuarios, clientes, participantes, inscripciones, cursos, null, null);
    }

    @Transactional
    public InscripcionClienteDTO inscribir(Long usuarioId, InscripcionCursoRequest request) {
        Usuario usuario = usuarioCliente(usuarioId);
        Cliente cliente = clientes.findByUsuarioId(usuarioId)
                .orElseThrow(() -> conflicto("Solicita a recepción que complete tu ficha de cliente y tu carnet"));
        Participante participante = participantes.findFirstByClienteIdOrderByIdAsc(cliente.getId())
                .orElseThrow(() -> conflicto("Debes estar registrado como participante por recepción antes de inscribirte"));

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setParticipante(participante);
        if (request.idCurso() != null) {
            Curso curso = cursos.bloquear(request.idCurso()).orElseThrow(() -> noEncontrado("curso"));
            if (!Boolean.TRUE.equals(curso.getActivo())) throw conflicto("El curso no está activo");
            if (inscripciones.existsByParticipanteIdAndCursoIdCurso(participante.getId(), curso.getIdCurso())) {
                throw conflicto("Ya estás inscrito en este curso");
            }
            if (inscripciones.countByCursoIdCursoAndEstadoNot(curso.getIdCurso(), "CANCELADA") >= curso.getCupoMaximo()) {
                throw conflicto("El curso ya alcanzó su cupo máximo");
            }
            inscripcion.setCurso(curso);
            inscripcion.setTotal(curso.getCosto());
        } else {
            Diplomado diplomado = diplomados.findById(request.idDiplomado())
                    .orElseThrow(() -> noEncontrado("diplomado"));
            if (!diplomado.isActivo()) throw conflicto("El diplomado no está activo");
            if (inscripciones.existsByParticipanteIdAndDiplomadoId(participante.getId(), diplomado.getId())) {
                throw conflicto("Ya estás inscrito en este diplomado");
            }
            inscripcion.setDiplomado(diplomado);
            inscripcion.setTotal(diplomado.getCosto());
        }
        inscripcion.setFecha(LocalDate.now(ZONA));
        inscripcion.setEstado("PENDIENTE");
        return vista(inscripciones.save(inscripcion));
    }

    @Transactional
    public List<InscripcionClienteDTO> inscripciones(Long usuarioId) {
        usuarioCliente(usuarioId);
        Cliente cliente = clientes.findByUsuarioId(usuarioId).orElse(null);
        if (cliente == null) return List.of();
        return inscripciones.findByParticipanteClienteIdOrderByFechaDescIdDesc(cliente.getId()).stream()
                .map(this::vista).toList();
    }

    @Transactional
    public InscripcionClienteDTO completarModulo(Long usuarioId, Long cursoId, Integer numeroModulo) {
        usuarioCliente(usuarioId);
        Inscripcion inscripcion = inscripciones.buscarInscripcionCursoDeUsuario(usuarioId, cursoId).stream()
                .findFirst().orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Necesitas una inscripción activa para guardar tu progreso."));
        if (!"CONFIRMADA".equalsIgnoreCase(inscripcion.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tu acceso al curso está bloqueado hasta que recepción verifique y confirme tu pago.");
        }
        int total = contarModulos(inscripcion.getCurso());
        if (numeroModulo == null || numeroModulo < 1 || numeroModulo > total) {
            throw conflicto("El módulo indicado no pertenece a este curso.");
        }
        Set<Integer> completados = leerModulosCompletados(inscripcion.getModulosCompletados(), total);
        if (numeroModulo > 1 && !completados.contains(numeroModulo - 1)) {
            throw conflicto("Completa el módulo anterior para desbloquear este contenido.");
        }
        completados.add(numeroModulo);
        inscripcion.setModulosCompletados(completados.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",")));
        if (completados.size() == total && inscripcion.getFechaFinalizacion() == null) {
            inscripcion.setFechaFinalizacion(LocalDate.now(ZONA));
        }
        return vista(inscripciones.save(inscripcion));
    }

    @Transactional
    public CertificadoCursoDTO certificado(Long usuarioId, Long cursoId) {
        usuarioCliente(usuarioId);
        Inscripcion inscripcion = inscripciones.buscarInscripcionCursoDeUsuario(usuarioId, cursoId).stream()
                .findFirst().orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Necesitas una inscripción para obtener el certificado."));
        int total = contarModulos(inscripcion.getCurso());
        Set<Integer> completados = leerModulosCompletados(inscripcion.getModulosCompletados(), total);
        if (!"CONFIRMADA".equalsIgnoreCase(inscripcion.getEstado()) || total == 0 || completados.size() != total) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "El certificado estará disponible cuando completes el curso y recepción confirme tu pago.");
        }
        if (inscripcion.getFechaFinalizacion() == null) {
            inscripcion.setFechaFinalizacion(LocalDate.now(ZONA));
            inscripciones.save(inscripcion);
        }
        Curso curso = inscripcion.getCurso();
        List<String> docentes = curso.getDocentes().stream()
                .map(asignacion -> asignacion.getDocente().getNombre()).toList();
        String nombre = inscripcion.getParticipante().getNombre() + " " + inscripcion.getParticipante().getApellido();
        String codigo = "UCA-CFC-" + inscripcion.getFechaFinalizacion().getYear() + "-" + inscripcion.getId();
        return new CertificadoCursoDTO(nombre, curso.getTitulo(), docentes,
                inscripcion.getFechaFinalizacion(), codigo);
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
        int totalModulos = curso == null ? 0 : contarModulos(curso);
        List<Integer> completados = leerModulosCompletados(inscripcion.getModulosCompletados(), totalModulos)
                .stream().toList();
        int porcentaje = totalModulos == 0 ? 0 : completados.size() * 100 / totalModulos;
        boolean finalizado = "CONFIRMADA".equalsIgnoreCase(inscripcion.getEstado())
                && totalModulos > 0 && porcentaje == 100;
        if (finalizado && inscripcion.getFechaFinalizacion() == null) {
            inscripcion.setFechaFinalizacion(LocalDate.now(ZONA));
        }
        Diplomado diplomado = inscripcion.getDiplomado();
        return new InscripcionClienteDTO(inscripcion.getId(), curso == null ? null : curso.getIdCurso(),
                curso == null && diplomado != null ? diplomado.getId() : null,
                curso == null && diplomado != null ? diplomado.getNombre() : curso.getTitulo(), inscripcion.getFecha(), inscripcion.getEstado(),
                inscripcion.getTotal(), curso != null && resenas != null && resenas.existsByInscripcion_Id(inscripcion.getId()),
                curso == null ? null : curso.getImagenUrl(), completados, totalModulos, porcentaje,
                finalizado, finalizado ? inscripcion.getFechaFinalizacion() : null);
    }

    private int contarModulos(Curso curso) {
        String contenido = curso.getContenidoPrograma();
        if (contenido == null || contenido.isBlank()) return 0;
        try {
            JsonNode modulos = JSON.readTree(contenido);
            if (modulos != null && modulos.isArray()) return modulos.size();
        } catch (Exception ignored) {
            // Los programas anteriores al editor por módulos se guardaban como una lista de líneas.
        }
        return (int) contenido.lines().map(String::trim).filter(linea -> !linea.isEmpty()).count();
    }

    private Set<Integer> leerModulosCompletados(String valor, int totalModulos) {
        Set<Integer> resultado = new TreeSet<>();
        if (valor == null || valor.isBlank()) return resultado;
        for (String item : valor.split(",")) {
            try {
                int numero = Integer.parseInt(item.trim());
                if (numero > 0 && numero <= totalModulos) resultado.add(numero);
            } catch (NumberFormatException ignored) {
                // Ignora datos antiguos que no correspondan a un número de módulo.
            }
        }
        return resultado;
    }

    private ApiException noEncontrado(String recurso) { return new ApiException(HttpStatus.NOT_FOUND, "No se encontró el " + recurso); }
    private ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
}
