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
import sv.edu.udb.ucacfcconnect.dto.DiplomadoDTO;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.DiplomadoService;
import sv.edu.udb.ucacfcconnect.service.JwtService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DiplomadoControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DiplomadoService diplomadoService;
    @Autowired
    private CategoriaRepository categorias;
    @Autowired
    private ModalidadRepository modalidades;
    @Autowired
    private RolRepository roles;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private JwtService jwtService;

    private Long idDiplomado;
    private String adminToken;
    private LocalDate fechaSesion;

    @BeforeEach
    void prepararDatos() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Finanzas");
        categoria.setDescripcion("Formación financiera");
        Long idCategoria = categorias.save(categoria).getIdCategoria();

        Modalidad modalidad = new Modalidad();
        modalidad.setNombre("Híbrida");
        modalidad.setDescripcion("Presencial y virtual");
        Long idModalidad = modalidades.save(modalidad).getIdModalidad();

        fechaSesion = LocalDate.now().plusDays(8);
        idDiplomado = diplomadoService.crear(new DiplomadoDTO(
                "Diplomado en finanzas", "Programa profesional de gestión financiera", 2,
                new BigDecimal("250.00"), LocalDate.now().plusDays(5), LocalDate.now().plusDays(12),
                idCategoria, idModalidad
        )).idDiplomado();

        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        rol = roles.save(rol);
        Usuario admin = new Usuario();
        admin.setNombre("Administrador de diplomados");
        admin.setCorreo("admin-diplomados@example.com");
        admin.setRol(rol);
        admin.setActivo(true);
        adminToken = jwtService.generarToken(usuarios.saveAndFlush(admin));
    }

    @Test
    void programaPublicaYExponeElDiplomadoPorApi() throws Exception {
        mockMvc.perform(post("/api/v1/diplomados/{id}/actividades", idDiplomado).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Taller aplicado",
                                  "fecha": "%s",
                                  "horaInicio": "08:00",
                                  "horaFin": "10:00",
                                  "cupo": 20
                                }
                                """.formatted(fechaSesion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DIPLOMADO"))
                .andExpect(jsonPath("$.cupo").value(20));

        mockMvc.perform(patch("/api/v1/diplomados/{id}/estado", idDiplomado).with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get("/api/v1/diplomados").param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].idDiplomado", hasItem(idDiplomado.intValue())));

        mockMvc.perform(get("/api/v1/diplomados/{id}/actividades", idDiplomado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Taller aplicado"));
    }

    @Test
    void soloUnAdministradorPuedeModificarDiplomados() throws Exception {
        mockMvc.perform(post("/api/v1/diplomados").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
