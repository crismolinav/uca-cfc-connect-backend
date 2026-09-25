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
                .andExpect(content().string(containsString("/js/cursos.js")));

        mockMvc.perform(get("/js/cursos.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"))
                .andExpect(content().string(containsString("/api/v1/cursos")))
                .andExpect(content().string(containsString("cargarCatalogos")));
    }
}
