package sv.edu.udb.ucacfcconnect.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.PagoClienteDTO;
import sv.edu.udb.ucacfcconnect.dto.PagoInscripcionRequest;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Inscripcion;
import sv.edu.udb.ucacfcconnect.entity.Pago;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.InscripcionRepository;
import sv.edu.udb.ucacfcconnect.repository.PagoRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class PagoClienteService {
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private static final List<String> METODOS = List.of("EFECTIVO", "TRANSFERENCIA", "TARJETA");
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final InscripcionRepository inscripciones;
    private final PagoRepository pagos;

    public PagoClienteService(UsuarioRepository usuarios, ClienteRepository clientes,
                              InscripcionRepository inscripciones, PagoRepository pagos) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.inscripciones = inscripciones;
        this.pagos = pagos;
    }

    @Transactional
    public PagoClienteDTO registrar(Long usuarioId, PagoInscripcionRequest request) {
        Cliente cliente = clienteExistente(usuarioId);
        String metodo = request.metodo().trim().toUpperCase(Locale.ROOT);
        if (!METODOS.contains(metodo)) throw invalido("Selecciona un método de pago válido");
        Inscripcion inscripcion = inscripciones.bloquear(request.idInscripcion())
                .orElseThrow(() -> noEncontrado("inscripción"));
        if (!cliente.getId().equals(inscripcion.getParticipante().getCliente().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La inscripción no pertenece a tu cuenta");
        }
        if (inscripcion.getCurso() == null) throw invalido("El pago en línea solo está disponible para cursos");
        if ("CONFIRMADA".equals(inscripcion.getEstado())) throw conflicto("La inscripción ya está pagada");
        if (pagos.existsByInscripcionIdAndEstadoIn(inscripcion.getId(), List.of("PENDIENTE", "CONFIRMADO"))) {
            throw conflicto("Ya existe un pago pendiente o confirmado para esta inscripción");
        }

        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setInscripcion(inscripcion);
        pago.setMonto(inscripcion.getTotal());
        pago.setFecha(LocalDate.now(ZONA));
        pago.setMetodo(metodo);
        pago.setReferencia(request.referencia() == null || request.referencia().isBlank()
                ? null : request.referencia().trim());
        pago.setEstado("PENDIENTE");
        return vista(pagos.save(pago));
    }

    @Transactional(readOnly = true)
    public List<PagoClienteDTO> listar(Long usuarioId) {
        usuarioCliente(usuarioId);
        Cliente cliente = clientes.findByUsuarioId(usuarioId).orElse(null);
        if (cliente == null) return List.of();
        return pagos.findByClienteIdOrderByFechaDescIdDesc(cliente.getId()).stream().map(this::vista).toList();
    }

    private Cliente clienteExistente(Long usuarioId) {
        usuarioCliente(usuarioId);
        return clientes.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Inscríbete en un curso antes de registrar un pago"));
    }

    private void usuarioCliente(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo una cuenta de cliente puede registrar pagos");
        }
    }

    private PagoClienteDTO vista(Pago pago) {
        Inscripcion inscripcion = pago.getInscripcion();
        String curso = inscripcion == null ? "Otro servicio" : inscripcion.getCurso() != null
                ? inscripcion.getCurso().getTitulo() : inscripcion.getDiplomado().getNombre();
        return new PagoClienteDTO(pago.getId(), inscripcion == null ? null : inscripcion.getId(), curso,
                pago.getMonto(), pago.getFecha(), pago.getMetodo(), pago.getReferencia(), pago.getEstado());
    }

    private ApiException noEncontrado(String recurso) { return new ApiException(HttpStatus.NOT_FOUND, "No se encontró la " + recurso); }
    private ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
    private ApiException invalido(String mensaje) { return new ApiException(HttpStatus.BAD_REQUEST, mensaje); }
}
