package sv.edu.udb.ucacfcconnect.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteResumen;
import sv.edu.udb.ucacfcconnect.dto.admin.EstadoClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.PaginaClientes;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;

@Service
public class AdminClienteService {
    private final ClienteRepository clientes;
    private final ParticipanteRegistroService registroParticipantes;

    public AdminClienteService(ClienteRepository clientes, ParticipanteRegistroService registroParticipantes) {
        this.clientes = clientes;
        this.registroParticipantes = registroParticipantes;
    }

    @Transactional(readOnly = true)
    public PaginaClientes listar(String q, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || q.length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los filtros de búsqueda no son válidos");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        Page<ClienteResumen> resultado = (q.isBlank() ? clientes.findAll(pageable) : clientes.buscar(q.trim(), pageable))
                .map(ClienteResumen::from);
        return PaginaClientes.from(resultado);
    }

    @Transactional(readOnly = true)
    public ClienteResumen detalle(Long id) {
        return ClienteResumen.from(obtener(id));
    }

    @Transactional
    public ClienteResumen crear(ClienteRequest request) {
        String documento = opcional(request.duiNit());
        validarDocumentoDisponible(documento, null);
        validarCorreoDisponible(opcional(request.correo()), null);
        Cliente cliente = new Cliente();
        aplicar(cliente, request);
        clientes.saveAndFlush(cliente);
        registroParticipantes.asegurar(cliente);
        return ClienteResumen.from(cliente);
    }

    @Transactional
    public ClienteResumen actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtener(id);
        if (cliente.getUsuario() != null &&
                !iguales(cliente.getCorreo(), opcional(request.correo()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El correo de una cuenta vinculada se gestiona desde usuarios");
        }
        validarDocumentoDisponible(opcional(request.duiNit()), id);
        validarCorreoDisponible(opcional(request.correo()), id);
        aplicar(cliente, request);
        if (cliente.getUsuario() != null) cliente.getUsuario().setNombre(cliente.getNombre());
        clientes.saveAndFlush(cliente);
        registroParticipantes.asegurar(cliente);
        return ClienteResumen.from(cliente);
    }

    @Transactional
    public ClienteResumen cambiarEstado(Long id, EstadoClienteRequest request) {
        Cliente cliente = obtener(id);
        Usuario usuario = cliente.getUsuario();
        if (usuario == null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Este cliente no tiene una cuenta de acceso vinculada");
        }
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Solo se puede cambiar el estado de una cuenta de cliente");
        }
        usuario.setActivo(request.activo());
        return ClienteResumen.from(cliente);
    }

    private Cliente obtener(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el cliente"));
    }

    private void validarDocumentoDisponible(String documento, Long idActual) {
        if (documento == null) return;
        clientes.findByDuiNit(documento).ifPresent(otro -> {
            if (!otro.getId().equals(idActual)) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente con ese DUI o NIT");
            }
        });
    }

    private void aplicar(Cliente cliente, ClienteRequest request) {
        cliente.setPrimerNombre(request.primerNombre());
        cliente.setSegundoNombre(request.segundoNombre());
        cliente.setPrimerApellido(request.primerApellido());
        cliente.setSegundoApellido(request.segundoApellido());
        cliente.setDuiNit(opcional(request.duiNit()));
        cliente.setEmpresa(opcional(request.empresa()));
        cliente.setCorreo(opcional(request.correo()));
        cliente.setTelefono(opcional(request.telefono()));
        cliente.setDireccion(opcional(request.direccion()));
    }

    private void validarCorreoDisponible(String correo, Long idActual) {
        if (correo == null) return;
        clientes.findByCorreoIgnoreCase(correo).ifPresent(otro -> {
            if (!otro.getId().equals(idActual)) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente con ese correo. Busca su ficha para evitar duplicados");
            }
        });
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private boolean iguales(String primero, String segundo) {
        return primero == null ? segundo == null : primero.equals(segundo);
    }
}
