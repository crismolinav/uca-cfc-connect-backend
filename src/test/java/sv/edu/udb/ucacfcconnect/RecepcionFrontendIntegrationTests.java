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
class RecepcionFrontendIntegrationTests {
    @Autowired MockMvc mvc;

    @Test
    void sirveElPanelConTodasLasOperacionesYRedireccionPorRol() throws Exception {
        mvc.perform(get("/recepcion/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("id=\"panel-agenda\"")))
                .andExpect(content().string(containsString("id=\"panel-inscripciones\"")))
                .andExpect(content().string(containsString("id=\"panel-catering\"")))
                .andExpect(content().string(containsString("id=\"panel-cotizaciones\"")))
                .andExpect(content().string(containsString("id=\"panel-actividades\"")))
                .andExpect(content().string(containsString("id=\"panel-pagos\"")))
                .andExpect(content().string(containsString("/js/recepcion.js")));

        mvc.perform(get("/js/auth.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("usuario?.rol === \"RECEPCIONISTA\"")))
                .andExpect(content().string(containsString("/recepcion/index.html")));
    }
}
