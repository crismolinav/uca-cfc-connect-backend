package sv.edu.udb.ucacfcconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.auth.DuiRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.LoginRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.RegistroRequest;
import sv.edu.udb.ucacfcconnect.dto.auth.UsuarioResponse;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.util.Locale;

@Service
public class AuthService {
    private static final String GOOGLE = "google";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String defaultRole;

    public AuthService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.auth.default-role:CLIENTE}") String defaultRole
    ) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.defaultRole = defaultRole;
    }

    @Transactional
    public AuthSession registrar(RegistroRequest request) {
        String correo = normalizarCorreo(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        if (clienteRepository.existsByDuiNit(request.dui())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese DUI");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setActivo(true);
        usuario.setEmailVerificado(false);
        usuario.setRol(resolverRolPredeterminado());

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
            Cliente cliente = new Cliente();
            cliente.setUsuario(usuario);
            cliente.setNombre(usuario.getNombre());
            cliente.setCorreo(usuario.getCorreo());
            cliente.setDuiNit(request.dui());
            clienteRepository.saveAndFlush(cliente);
            usuario.setCliente(cliente);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo o DUI");
        }

        return crearRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public AuthSession login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizarCorreo(request.correo()))
                .orElseThrow(this::credencialesInvalidas);

        if (usuario.getPassword() == null || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw credencialesInvalidas();
        }
        validarActivo(usuario);
        return crearRespuesta(usuario);
    }

    @Transactional
    public Usuario procesarUsuarioGoogle(String providerId, String correo, String nombre, boolean emailVerificado) {
        if (!emailVerificado) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google no confirmó el correo de la cuenta");
        }

        String correoNormalizado = normalizarCorreo(correo);
        Usuario porProveedor = usuarioRepository
                .findByOauthProviderAndOauthProviderId(GOOGLE, providerId)
                .orElse(null);

        if (porProveedor != null) {
            validarActivo(porProveedor);
            porProveedor.setNombre(nombre.trim());
            porProveedor.setCorreo(correoNormalizado);
            porProveedor.setEmailVerificado(true);
            return usuarioRepository.save(porProveedor);
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoNormalizado).orElseGet(Usuario::new);
        if (usuario.getId() != null && usuario.getOauthProvider() != null
                && !GOOGLE.equalsIgnoreCase(usuario.getOauthProvider())) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está vinculado con otro proveedor");
        }

        if (usuario.getId() == null) {
            usuario.setCorreo(correoNormalizado);
            usuario.setRol(resolverRolPredeterminado());
            usuario.setActivo(true);
        } else {
            validarActivo(usuario);
        }

        usuario.setNombre(nombre.trim());
        usuario.setOauthProvider(GOOGLE);
        usuario.setOauthProviderId(providerId);
        usuario.setEmailVerificado(true);
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioGoogle(String providerId) {
        Usuario usuario = usuarioRepository.findByOauthProviderAndOauthProviderId(GOOGLE, providerId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No se encontró la cuenta autenticada"));
        validarActivo(usuario);
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el usuario"));
        validarActivo(usuario);
        return usuario;
    }

    @Transactional
    public Usuario completarDui(Long usuarioId, DuiRequest request) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un cliente puede completar este dato");
        }
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElse(null);
        if (cliente != null && cliente.getDuiNit() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "El DUI de esta cuenta ya está registrado");
        }
        if (clienteRepository.existsByDuiNit(request.dui())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con ese DUI");
        }
        if (cliente == null) {
            cliente = new Cliente();
            cliente.setUsuario(usuario);
            cliente.setNombre(usuario.getNombre());
            cliente.setCorreo(usuario.getCorreo());
        }
        cliente.setDuiNit(request.dui());
        clienteRepository.saveAndFlush(cliente);
        usuario.setCliente(cliente);
        return usuario;
    }

    private AuthSession crearRespuesta(Usuario usuario) {
        return new AuthSession(
                jwtService.generarToken(usuario),
                jwtService.getExpirationSeconds(),
                UsuarioResponse.from(usuario)
        );
    }

    private Rol resolverRolPredeterminado() {
        return rolRepository.findByNombreIgnoreCase(defaultRole)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(defaultRole.toUpperCase(Locale.ROOT));
                    rol.setDescripcion("Rol predeterminado para usuarios registrados");
                    return rolRepository.save(rol);
                });
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void validarActivo(Usuario usuario) {
        if (!usuario.isActivo()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La cuenta está desactivada");
        }
    }

    private ApiException credencialesInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }
}
