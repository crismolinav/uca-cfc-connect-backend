package sv.edu.udb.ucacfcconnect.dto.auth;

import sv.edu.udb.ucacfcconnect.entity.Usuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String dui,
        String rol,
        String proveedor,
        boolean emailVerificado
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getCliente() == null ? null : usuario.getCliente().getDuiNit(),
                usuario.getRol().getNombre(),
                usuario.getOauthProvider(),
                usuario.isEmailVerificado()
        );
    }
}
