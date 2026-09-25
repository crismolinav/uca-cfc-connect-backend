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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CursoControllerValidationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RolRepository roles;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private JwtService jwtService;

    @Test
    void rechazaCupoDecimalAunqueLaApiSeInvoqueDirectamente() throws Exception {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        rol = roles.saveAndFlush(rol);

        Usuario admin = new Usuario();
        admin.setNombre("Administrador de prueba");
        admin.setCorreo("admin-cursos-validacion@example.com");
        admin.setRol(rol);
        admin.setActivo(true);
        admin = usuarios.saveAndFlush(admin);

        mockMvc.perform(post("/api/v1/cursos").with(csrf())
                        .header("Authorization", "Bearer " + jwtService.generarToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Curso con cupo decimal",
                                  "descripcion": "Este registro debe ser rechazado por el controlador",
                                  "duracionHoras": 20,
                                  "cupoMaximo": 10.5,
                                  "costo": 25.50,
                                  "fechaInicio": "2026-10-10",
                                  "fechaFin": "2026-10-20",
                                  "horario": "Lunes | 18:00-20:00",
                                  "idCategoria": 1,
                                  "idModalidad": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo o los parámetros de la solicitud tienen un formato inválido"
                ));
    }
}
