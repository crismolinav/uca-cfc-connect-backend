package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class AdminClienteIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ClienteRepository clientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void limpiar() {
        clientes.deleteAll();
        usuarios.deleteAll();
        roles.deleteAll();
    }

    @Test
    void soloAdministradorPuedeConsultarYModificarClientes() throws Exception {
        mockMvc.perform(get("/admin/index.html"))
                .andExpect(status().isOk());
        String clienteToken = tokenPara("CLIENTE", "cliente@example.com");
        String adminToken = tokenPara("ADMIN", "admin@example.com");

        mockMvc.perform(get("/api/v1/admin/clientes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/clientes")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/clientes").with(csrf())
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana López\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/clientes").with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana López","duiNit":"87654321-0",
                                 "empresa":"Empresa UCA","correo":"ana@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana López"));

        Long id = clientes.findByDuiNit("87654321-0").orElseThrow().getId();
        mockMvc.perform(get("/api/v1/admin/clientes?q=Ana&page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].duiNit").value("87654321-0"));

        mockMvc.perform(put("/api/v1/admin/clientes/" + id).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana López","duiNit":"87654321-0",
                                 "empresa":"Nueva Empresa","correo":"ana@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa").value("Nueva Empresa"));
    }

    @Test
    void administradorPuedeDesactivarYReactivarAccesoDeCliente() throws Exception {
        String adminToken = tokenPara("ADMIN", "admin@example.com");
        String clienteToken = tokenPara("CLIENTE", "ana@example.com");
        Usuario usuario = usuarios.findByCorreoIgnoreCase("ana@example.com").orElseThrow();
        usuario.setNombre("Ana López");
        usuario.setPassword(passwordEncoder.encode("ClaveSegura123"));
        usuarios.saveAndFlush(usuario);
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana López");
        cliente.setCorreo("ana@example.com");
        cliente.setUsuario(usuario);
        cliente = clientes.saveAndFlush(cliente);
        String ruta = "/api/v1/admin/clientes/" + cliente.getId() + "/estado";

        mockMvc.perform(patch(ruta).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(ruta).with(csrf())
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(ruta).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(ruta).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/v1/admin/clientes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activo").value(false));
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@example.com\",\"password\":\"ClaveSegura123\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(ruta).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@example.com\",\"password\":\"ClaveSegura123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void clienteSinCuentaNoPuedeCambiarEstadoDeAcceso() throws Exception {
        String adminToken = tokenPara("ADMIN", "admin@example.com");
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente sin cuenta");
        cliente = clientes.saveAndFlush(cliente);

        mockMvc.perform(patch("/api/v1/admin/clientes/" + cliente.getId() + "/estado").with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isConflict());
    }

    private String tokenPara(String nombreRol, String correo) {
        Rol rol = new Rol();
        rol.setNombre(nombreRol);
        rol = roles.saveAndFlush(rol);
        Usuario usuario = new Usuario();
        usuario.setNombre(nombreRol);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario = usuarios.saveAndFlush(usuario);
        return jwtService.generarToken(usuario);
    }
}
