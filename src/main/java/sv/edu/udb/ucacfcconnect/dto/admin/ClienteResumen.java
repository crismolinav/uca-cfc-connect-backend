package sv.edu.udb.ucacfcconnect.dto.admin;

import sv.edu.udb.ucacfcconnect.entity.Cliente;

public record ClienteResumen(
        Long id,
        String nombre,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String carnet,
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
                cliente.getId(), cliente.getNombre(), cliente.getPrimerNombre(), cliente.getSegundoNombre(),
                cliente.getPrimerApellido(), cliente.getSegundoApellido(), cliente.getCarnet(), cliente.getDuiNit(),
                cliente.getEmpresa(), cliente.getCorreo(), cliente.getTelefono(), cliente.getDireccion(),
                cliente.getUsuario() != null,
                cliente.getUsuario() == null ? null : cliente.getUsuario().isActivo()
        );
    }
}
