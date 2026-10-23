package sv.edu.udb.ucacfcconnect.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.RecuperacionPasswordRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

@Service
public class EliminacionCuentaService {
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final RecuperacionPasswordRepository recuperaciones;

    public EliminacionCuentaService(UsuarioRepository usuarios, ClienteRepository clientes,
                                    RecuperacionPasswordRepository recuperaciones) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.recuperaciones = recuperaciones;
    }

    @Transactional
    public void eliminar(Long usuarioId, String confirmacion) {
        if (!"ELIMINAR".equals(confirmacion)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe ELIMINAR para confirmar la eliminación");
        }

        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró la cuenta"));
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes eliminar tu cuenta de cliente desde esta página");
        }

        // La ficha y su historial operativo permanecen, pero ya no se vinculan a una cuenta.
        Cliente cliente = clientes.findByUsuarioId(usuarioId).orElse(null);
        if (cliente != null) {
            cliente.setUsuario(null);
            cliente.setCorreo(null);
            cliente.setDuiNit(null);
            cliente.setTelefono(null);
            cliente.setDireccion(null);
            usuario.setCliente(null);
            clientes.saveAndFlush(cliente);
        }

        recuperaciones.deleteAllByUsuarioId(usuarioId);
        recuperaciones.flush();
        usuarios.delete(usuario);
        usuarios.flush();
    }
}
