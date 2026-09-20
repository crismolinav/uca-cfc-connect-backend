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

    public AdminClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
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
        Cliente cliente = new Cliente();
        aplicar(cliente, request);
        return ClienteResumen.from(clientes.save(cliente));
    }

    @Transactional
    public ClienteResumen actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtener(id);
        if (cliente.getUsuario() != null &&
                (!cliente.getNombre().equals(request.nombre().trim()) ||
                        !iguales(cliente.getCorreo(), opcional(request.correo())))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El nombre y correo de una cuenta vinculada se gestionan desde usuarios");
        }
        validarDocumentoDisponible(opcional(request.duiNit()), id);
        aplicar(cliente, request);
        return ClienteResumen.from(clientes.save(cliente));
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
        cliente.setNombre(request.nombre().trim());
        cliente.setDuiNit(opcional(request.duiNit()));
        cliente.setEmpresa(opcional(request.empresa()));
        cliente.setCorreo(opcional(request.correo()));
        cliente.setTelefono(opcional(request.telefono()));
        cliente.setDireccion(opcional(request.direccion()));
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private boolean iguales(String primero, String segundo) {
        return primero == null ? segundo == null : primero.equals(segundo);
    }
}
