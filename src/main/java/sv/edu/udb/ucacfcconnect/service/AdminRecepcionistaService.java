package sv.edu.udb.ucacfcconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.admin.RecepcionistaRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.RecepcionistaResponse;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.util.List;
import java.util.Locale;

@Service
public class AdminRecepcionistaService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwords;

    public AdminRecepcionistaService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwords) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<RecepcionistaResponse> listar() {
        return usuarios.findAllByRolNombreIgnoreCaseOrderByNombreAsc("RECEPCIONISTA")
                .stream().map(RecepcionistaResponse::from).toList();
    }

    @Transactional
    public RecepcionistaResponse crear(RecepcionistaRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        Rol rol = roles.findByNombreIgnoreCase("RECEPCIONISTA").orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setNombre("RECEPCIONISTA");
            nuevo.setDescripcion("Operación de recepción, alquileres y servicios");
            return roles.save(nuevo);
        });
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwords.encode(request.password()));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setEmailVerificado(false);
        try {
            return RecepcionistaResponse.from(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
    }

    @Transactional
    public RecepcionistaResponse cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró la cuenta"));
        if (!"RECEPCIONISTA".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.CONFLICT, "La cuenta no pertenece a recepción");
        }
        usuario.setActivo(activo);
        return RecepcionistaResponse.from(usuario);
    }
}
