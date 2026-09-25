package sv.edu.udb.ucacfcconnect.dto.admin;

import sv.edu.udb.ucacfcconnect.entity.Usuario;

public record RecepcionistaResponse(Long id, String nombre, String correo, boolean activo) {
    public static RecepcionistaResponse from(Usuario usuario) {
        return new RecepcionistaResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.isActivo());
    }
}
