package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminRecepcionistaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;

    @Test
    void soloAdminCreaYDesactivaCuentasDeRecepcion() throws Exception {
        String admin = token("ADMIN", "admin-alta-recepcion@example.test");
        String cliente = token("CLIENTE", "cliente-alta-recepcion@example.test");
        String body = """
                {"nombre":"Ana Recepción","correo":"ANA.RECEPCION@example.test","password":"ClaveTemporal123"}
                """;

        mvc.perform(post("/api/v1/admin/recepcionistas").with(csrf())
                        .header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/recepcionistas").with(csrf())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana.recepcion@example.test"));

        Usuario recepcionista = usuarios.findByCorreoIgnoreCase("ana.recepcion@example.test").orElseThrow();
        assertThat(recepcionista.getPassword()).isNotEqualTo("ClaveTemporal123");
        assertThat(recepcionista.getRol().getNombre()).isEqualTo("RECEPCIONISTA");
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana.recepcion@example.test","password":"ClaveTemporal123","recordar":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("RECEPCIONISTA"));

        String recepcionToken = jwt.generarToken(recepcionista);
        mvc.perform(get("/api/v1/admin/recepcionistas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("ana.recepcion@example.test"));
        mvc.perform(get("/api/v1/admin/recepcionistas").header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/recepcionistas/" + recepcionista.getId() + "/estado").with(csrf())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isForbidden());
    }

    private String token(String rolNombre, String correo) {
        Rol rol = roles.findByNombreIgnoreCase(rolNombre).orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setNombre(rolNombre);
            return roles.save(nuevo);
        });
        Usuario usuario = new Usuario();
        usuario.setNombre(rolNombre);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return jwt.generarToken(usuarios.save(usuario));
    }
}
