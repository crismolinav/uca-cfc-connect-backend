package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CursoFrontendIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sirveLaInterfazAdministrativaDeCursos() throws Exception {
        mockMvc.perform(get("/admin/cursos.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h1 id=\"page-title\">Cursos</h1>")))
                .andExpect(content().string(containsString("+ Nuevo curso")))
                .andExpect(content().string(containsString("name=\"diasHorario\"")))
                .andExpect(content().string(containsString("type=\"time\"")))
                .andExpect(content().string(containsString("name=\"cupoMaximo\" type=\"number\" min=\"1\" step=\"1\"")))
                .andExpect(content().string(containsString("/js/cursos.js")));

        mockMvc.perform(get("/js/cursos.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"))
                .andExpect(content().string(containsString("/api/v1/cursos")))
                .andExpect(content().string(containsString("cargarCatalogos")))
                .andExpect(content().string(containsString("construirHorario")))
                .andExpect(content().string(containsString("Number.isInteger")));
    }

    @Test
    void sirveElCatalogoRealDeCursosParaClientes() throws Exception {
        mockMvc.perform(get("/cliente/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h2 id=\"catalog-title\">Cursos disponibles</h2>")))
                .andExpect(content().string(containsString("id=\"course-category-filter\"")))
                .andExpect(content().string(containsString("id=\"course-modality-filter\"")))
                .andExpect(content().string(containsString("id=\"course-detail-dialog\"")))
                .andExpect(content().string(containsString("/js/cliente.js")));

        mockMvc.perform(get("/js/cliente.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"))
                .andExpect(content().string(containsString("activo: 'true'")))
                .andExpect(content().string(containsString("/catalogos/categorias")))
                .andExpect(content().string(containsString("Ver detalles")));
    }

    @Test
    void sirveLaAdministracionYElCatalogoDeDiplomados() throws Exception {
        mockMvc.perform(get("/admin/diplomados.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h1 id=\"page-title\">Diplomados</h1>")))
                .andExpect(content().string(containsString("+ Nuevo diplomado")))
                .andExpect(content().string(containsString("id=\"sessions-dialog\"")))
                .andExpect(content().string(containsString("id=\"session-progress\"")))
                .andExpect(content().string(containsString("/js/diplomados.js")));

        mockMvc.perform(get("/cliente/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h2 id=\"diploma-catalog-title\">Diplomados disponibles</h2>")))
                .andExpect(content().string(containsString("id=\"diploma-grid\"")))
                .andExpect(content().string(containsString("id=\"diploma-detail-dialog\"")));

        mockMvc.perform(get("/js/diplomados.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"))
                .andExpect(content().string(containsString("/api/v1/diplomados")))
                .andExpect(content().string(containsString("/actividades")))
                .andExpect(content().string(containsString("Number.isInteger")));

        mockMvc.perform(get("/js/cliente.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("const diplomaApi = '/api/v1/diplomados'")))
                .andExpect(content().string(containsString("openDiplomaDetail")));
    }

    @Test
    void todasLasPantallasAdministrativasCompartenLaMismaNavegacion() throws Exception {
        for (String ruta : new String[]{
                "/admin/index.html", "/admin/cursos.html", "/admin/diplomados.html"
        }) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"/admin/index.html\"")))
                    .andExpect(content().string(containsString("href=\"/admin/cursos.html\"")))
                    .andExpect(content().string(containsString("href=\"/admin/diplomados.html\"")))
                    .andExpect(content().string(containsString("nav-pending\">Agenda <small>")))
                    .andExpect(content().string(containsString("nav-pending\">Cotizaciones <small>")))
                    .andExpect(content().string(containsString("nav-pending\">Pagos <small>")));
        }
    }
}
