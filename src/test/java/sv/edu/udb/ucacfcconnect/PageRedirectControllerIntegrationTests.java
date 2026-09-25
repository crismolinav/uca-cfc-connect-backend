package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageRedirectControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "/login.html, /auth/login.html",
            "/registro.html, /auth/registro.html",
            "/auth-callback.html, /auth/callback.html",
            "/perfil.html, /cuenta/perfil.html",
            "/cliente.html, /cliente/index.html",
            "/aprendizaje.html, /cliente/aprendizaje.html",
            "/configuracion.html, /cliente/configuracion.html",
            "/admin.html, /admin/index.html",
            "/cursos.html, /admin/cursos.html",
            "/espacios.html, /admin/espacios.html",
            "/alquileres.html, /admin/alquileres.html",
            "/cotizaciones.html, /admin/cotizaciones.html"
    })
    void redirigeLasRutasAnterioresALaNuevaEstructura(String rutaAnterior, String rutaNueva) throws Exception {
        mockMvc.perform(get(rutaAnterior))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(rutaNueva));
    }
}
