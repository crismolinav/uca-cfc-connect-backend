package sv.edu.udb.ucacfcconnect.dto.auth;

import sv.edu.udb.ucacfcconnect.entity.Usuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String dui,
        String carnet,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
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
                usuario.getCliente() == null ? null : usuario.getCliente().getCarnet(),
                usuario.getCliente() == null ? null : usuario.getCliente().getPrimerNombre(),
                usuario.getCliente() == null ? null : usuario.getCliente().getSegundoNombre(),
                usuario.getCliente() == null ? null : usuario.getCliente().getPrimerApellido(),
                usuario.getCliente() == null ? null : usuario.getCliente().getSegundoApellido(),
                usuario.getRol().getNombre(),
                usuario.getOauthProvider(),
                usuario.isEmailVerificado()
        );
    }
}
