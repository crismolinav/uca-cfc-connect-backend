package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.DocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocenteControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private RolRepository roles;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private DocenteRepository docentes;
    @Autowired private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void crearAdministrador() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        rol = roles.saveAndFlush(rol);

        Usuario admin = new Usuario();
        admin.setNombre("Administrador de docentes");
        admin.setCorreo("admin-docentes@example.com");
        admin.setRol(rol);
        admin.setActivo(true);
        adminToken = jwtService.generarToken(usuarios.saveAndFlush(admin));
    }

    @Test
    void ejecutaCrudCompletoYExponeLaInterfaz() throws Exception {
        mockMvc.perform(get("/admin/docentes.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nuevo docente")));

        mockMvc.perform(post("/api/v1/docentes").with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana Martínez",
                                  "especialidad": "Analítica de datos",
                                  "correo": "ana.martinez@uca.edu.sv",
                                  "telefono": "+503 2222-0000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana Martínez"));

        Long id = docentes.findAll().getFirst().getId();
        mockMvc.perform(put("/api/v1/docentes/{id}", id).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana María Martínez",
                                  "especialidad": "Ciencia de datos",
                                  "correo": "ana.martinez@uca.edu.sv",
                                  "telefono": "+503 2222-0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("Ciencia de datos"));

        mockMvc.perform(delete("/api/v1/docentes/{id}", id).with(csrf())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/docentes/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void protegeElCatalogoYValidaLosDatos() throws Exception {
        mockMvc.perform(get("/api/v1/docentes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/docentes").with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "A",
                                  "especialidad": "",
                                  "correo": "correo-invalido",
                                  "telefono": "12"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.especialidad").exists())
                .andExpect(jsonPath("$.correo").exists())
                .andExpect(jsonPath("$.telefono").exists());
    }
}
