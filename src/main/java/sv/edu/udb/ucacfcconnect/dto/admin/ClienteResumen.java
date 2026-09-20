package sv.edu.udb.ucacfcconnect.dto.admin;

import sv.edu.udb.ucacfcconnect.entity.Cliente;

public record ClienteResumen(
        Long id,
        String nombre,
        String duiNit,
        String empresa,
        String correo,
        String telefono,
        String direccion,
        boolean tieneCuenta,
        Boolean activo
) {
    public static ClienteResumen from(Cliente cliente) {
        return new ClienteResumen(
                cliente.getId(), cliente.getNombre(), cliente.getDuiNit(),
                cliente.getEmpresa(), cliente.getCorreo(), cliente.getTelefono(), cliente.getDireccion(),
                cliente.getUsuario() != null,
                cliente.getUsuario() == null ? null : cliente.getUsuario().isActivo()
        );
    }
}
