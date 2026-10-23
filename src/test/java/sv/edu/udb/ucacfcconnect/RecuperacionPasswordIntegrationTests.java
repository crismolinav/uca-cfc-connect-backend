package sv.edu.udb.ucacfcconnect;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.RecuperacionPasswordRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.mail.username=pruebas@example.test",
        "app.password-reset.base-url=http://localhost:8080"})
@AutoConfigureMockMvc
class RecuperacionPasswordIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired RecuperacionPasswordRepository recuperaciones;
    @Autowired ClienteRepository clientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @MockitoBean JavaMailSender mailSender;

    @Autowired sv.edu.udb.ucacfcconnect.repository.ParticipanteRepository participantes;

    @BeforeEach
    void limpiar() {
        recuperaciones.deleteAll();
        participantes.deleteAll();
        clientes.deleteAll();
        usuarios.deleteAll();
        roles.deleteAll();
    }

    @Test
    void recuperaPasswordUnaSolaVezYRevocaSesionAnterior() throws Exception {
        var registro = mvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"primerNombre":"Ana","primerApellido":"López","segundoApellido":"Díaz","dui":"12345678-9","correo":"ana@example.test",
                                 "password":"ClaveAnterior123","aceptaTerminos":true}
                                """))
                .andExpect(status().isCreated()).andReturn();
        Cookie cookieAnterior = new Cookie("uca_cfc_session", registro.getResponse().getCookie("uca_cfc_session").getValue());

        mvc.perform(post("/api/v1/auth/recuperar-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ANA@example.test\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(email.capture());
        assertThat(email.getValue().getTo()).containsExactly("ana@example.test");
        String texto = email.getValue().getText();
        Matcher matcher = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(texto);
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        mvc.perform(post("/api/v1/auth/restablecer-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"ClaveNueva456\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/restablecer-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"OtraClave789\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@example.test\",\"password\":\"ClaveAnterior123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .cookie(cookieAnterior)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@example.test\",\"password\":\"ClaveNueva456\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/csrf").cookie(cookieAnterior))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/cursos").cookie(cookieAnterior))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").cookie(cookieAnterior))
                .andExpect(status().isForbidden());
    }

    @Test
    void correoInexistenteRecibeMismaRespuestaSinEnviarCorreo() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperar-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"inexistente@example.test\"}"))
                .andExpect(status().isAccepted());
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void limitaSolicitudesSeguidasYPublicaLasPantallas() throws Exception {
        mvc.perform(get("/auth/recuperar.html")).andExpect(status().isOk());
        mvc.perform(get("/auth/restablecer.html")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"primerNombre":"Ana","primerApellido":"López","segundoApellido":"Díaz","dui":"12345678-9","correo":"ana@example.test",
                                 "password":"ClaveAnterior123","aceptaTerminos":true}
                                """))
                .andExpect(status().isCreated());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/recuperar-password").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"ana@example.test\"}"))
                    .andExpect(status().isAccepted());
        }
        verify(mailSender, timeout(5000).times(1)).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        assertThat(recuperaciones.count()).isEqualTo(1);
    }

    @Test
    void enlaceVencidoSeRechaza() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"primerNombre":"Ana","primerApellido":"López","segundoApellido":"Díaz","dui":"12345678-9","correo":"ana@example.test",
                                 "password":"ClaveAnterior123","aceptaTerminos":true}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/recuperar-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@example.test\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(email.capture());
        Matcher matcher = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(email.getValue().getText());
        assertThat(matcher.find()).isTrue();
        var recuperacion = recuperaciones.findAll().get(0);
        recuperacion.setExpiraEn(Instant.now().minusSeconds(1));
        recuperaciones.saveAndFlush(recuperacion);
        mvc.perform(post("/api/v1/auth/restablecer-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + matcher.group(1) + "\",\"password\":\"ClaveNueva456\"}"))
                .andExpect(status().isBadRequest());
    }
}
