package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void limpiarBaseDeDatos() {
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
    }

    @Test
    void registraIniciaSesionYConsultaPerfilConJwt() throws Exception {
        String registro = """
                {
                  "nombre": "Kevin Zepeda",
                  "dui": "12345678-9",
                  "correo": "  KEVIN@EXAMPLE.COM ",
                  "password": "ClaveSegura123",
                  "aceptaTerminos": true
                }
                """;

        MvcResult resultadoRegistro = mockMvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.usuario.correo").value("kevin@example.com"))
                .andExpect(jsonPath("$.usuario.dui").value("12345678-9"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn();

        jakarta.servlet.http.Cookie cookieRegistro =
                resultadoRegistro.getResponse().getCookie("uca_cfc_session");
        assertThat(cookieRegistro).isNotNull();
        assertThat(cookieRegistro.isHttpOnly()).isTrue();
        assertThat(resultadoRegistro.getResponse().getHeader("Set-Cookie"))
                .contains("HttpOnly", "SameSite=Lax");

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase("kevin@example.com").orElseThrow();
        assertThat(usuario.getPassword()).isNotEqualTo("ClaveSegura123");
        assertThat(passwordEncoder.matches("ClaveSegura123", usuario.getPassword())).isTrue();
        assertThat(clienteRepository.findByUsuarioId(usuario.getId()).orElseThrow().getDuiNit())
                .isEqualTo("12345678-9");

        MvcResult resultadoLogin = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "kevin@example.com",
                                  "password": "ClaveSegura123",
                                  "recordar": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();
        assertThat(resultadoLogin.getResponse().getCookie("uca_cfc_session").getMaxAge())
                .isGreaterThan(0);

        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(cookieRegistro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Kevin Zepeda"))
                .andExpect(jsonPath("$.correo").value("kevin@example.com"))
                .andExpect(jsonPath("$.dui").value("12345678-9"));
    }

    @Test
    void rechazaCorreoDuplicadoYCredencialesIncorrectas() throws Exception {
        String registro = """
                {
                  "nombre": "Kevin Zepeda",
                  "dui": "12345678-9",
                  "correo": "kevin@example.com",
                  "password": "ClaveSegura123",
                  "aceptaTerminos": true
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con ese correo"));

        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "kevin@example.com",
                                  "password": "ClaveIncorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void validaRegistroYProtegeElPerfil() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "dui": "123",
                                  "correo": "correo-invalido",
                                  "password": "123",
                                  "aceptaTerminos": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.dui").exists())
                .andExpect(jsonPath("$.correo").exists())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.aceptaTerminos").exists());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje")
                        .value("Debe iniciar sesión para acceder a este recurso"));
    }

    @Test
    void protegeOperacionesConCsrfYEliminaLaCookieAlCerrarSesion() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token").asText();
        MockHttpSession csrfSession = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertThat(csrfSession).isNotNull();

        String registro = """
                {"nombre":"Ana López","dui":"87654321-0","correo":"ana@example.com",
                 "password":"ClaveSegura123","aceptaTerminos":true}
                """;
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isForbidden());

        MvcResult registroResult = mockMvc.perform(post("/api/v1/auth/registro")
                        .session(csrfSession)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isCreated())
                .andReturn();
        jakarta.servlet.http.Cookie authCookie =
                registroResult.getResponse().getCookie("uca_cfc_session");
        assertThat(authCookie).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@example.com"));

        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .session(csrfSession)
                        .cookie(authCookie)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent())
                .andReturn();
        jakarta.servlet.http.Cookie deleted = logoutResult.getResponse().getCookie("uca_cfc_session");
        assertThat(deleted).isNotNull();
        assertThat(deleted.getMaxAge()).isZero();
    }


    @Test
    void sirveLaInterfazDeAutenticacion() throws Exception {
        mockMvc.perform(get("/auth/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bienvenido de nuevo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continuar con Google")));

        mockMvc.perform(get("/auth/registro.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crea tu cuenta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DUI")));
    }

    @Test
    void completaDuiDeCuentaGoogleYEvitaDuplicados() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Kevin Zepeda","dui":"12345678-9",
                                 "correo":"kevin@example.com","password":"ClaveSegura123",
                                 "aceptaTerminos":true}
                                """))
                .andExpect(status().isCreated());

        Usuario google = new Usuario();
        google.setNombre("Ana López");
        google.setCorreo("ana@example.com");
        google.setRol(rolRepository.findByNombreIgnoreCase("CLIENTE").orElseThrow());
        google.setActivo(true);
        google.setEmailVerificado(true);
        google.setOauthProvider("google");
        google.setOauthProviderId("google-ana");
        google = usuarioRepository.saveAndFlush(google);
        String token = jwtService.generarToken(google);

        mockMvc.perform(post("/api/v1/auth/me/dui").with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dui\":\"12345678-9\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/auth/me/dui").with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dui\":\"87654321-0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dui").value("87654321-0"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dui").value("87654321-0"));
    }
}
