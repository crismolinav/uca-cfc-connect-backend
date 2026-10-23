package sv.edu.udb.ucacfcconnect;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.RecuperacionPassword;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.RecuperacionPasswordRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EliminarCuentaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired RolRepository roles;
    @Autowired RecuperacionPasswordRepository recuperaciones;
    @Autowired JwtService jwtService;

    @Autowired sv.edu.udb.ucacfcconnect.repository.ParticipanteRepository participantes;

    @BeforeEach
    @AfterEach
    void limpiar() {
        recuperaciones.deleteAll();
        participantes.deleteAll();
        clientes.deleteAll();
        usuarios.deleteAll();
        roles.deleteAll();
    }

    @Test
    void eliminaCuentaClienteSinBorrarFichaOperativaYRevocaSesion() throws Exception {
        var registro = mvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"primerNombre":"Ana","primerApellido":"López","segundoApellido":"Díaz","dui":"12345678-9","correo":"ana@example.test",
                                 "password":"ClaveSegura123","aceptaTerminos":true}
                                """))
                .andExpect(status().isCreated()).andReturn();
        Cookie sesion = registro.getResponse().getCookie("uca_cfc_session");
        Usuario usuario = usuarios.findByCorreoIgnoreCase("ana@example.test").orElseThrow();
        Cliente cliente = clientes.findByUsuarioId(usuario.getId()).orElseThrow();
        Long clienteId = cliente.getId();

        RecuperacionPassword recuperacion = new RecuperacionPassword();
        recuperacion.setUsuario(usuario);
        recuperacion.setTokenHash("a".repeat(64));
        recuperacion.setCreadoEn(Instant.now());
        recuperacion.setExpiraEn(Instant.now().plusSeconds(1200));
        recuperacion.setPasswordVersion(0L);
        recuperaciones.saveAndFlush(recuperacion);

        mvc.perform(delete("/api/v1/auth/me").with(csrf()).cookie(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmacion\":\"NO\"}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.existsById(usuario.getId())).isTrue();

        var eliminacion = mvc.perform(delete("/api/v1/auth/me").with(csrf()).cookie(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmacion\":\"ELIMINAR\"}"))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(eliminacion.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
        assertThat(usuarios.existsById(usuario.getId())).isFalse();
        assertThat(recuperaciones.count()).isZero();
        Cliente conservado = clientes.findById(clienteId).orElseThrow();
        assertThat(conservado.getUsuario()).isNull();
        assertThat(conservado.getCorreo()).isNull();
        assertThat(conservado.getDuiNit()).isNull();
        assertThat(conservado.getNombre()).isEqualTo("Ana López Díaz");
        mvc.perform(get("/api/v1/auth/me").cookie(sesion)).andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/registro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"primerNombre":"Ana","primerApellido":"López","segundoApellido":"Díaz","dui":"12345678-9","correo":"ana@example.test",
                                 "password":"OtraClave123","aceptaTerminos":true}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void noPermiteEliminarSinSesionNiDesdeCuentaAdministrativa() throws Exception {
        mvc.perform(delete("/api/v1/auth/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmacion\":\"ELIMINAR\"}"))
                .andExpect(status().isUnauthorized());

        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        rol = roles.saveAndFlush(rol);
        Usuario administrador = new Usuario();
        administrador.setNombre("Administrador");
        administrador.setCorreo("admin@example.test");
        administrador.setRol(rol);
        administrador = usuarios.saveAndFlush(administrador);
        Cookie sesion = new Cookie("uca_cfc_session", jwtService.generarToken(administrador));

        mvc.perform(delete("/api/v1/auth/me").with(csrf()).cookie(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmacion\":\"ELIMINAR\"}"))
                .andExpect(status().isForbidden());
        assertThat(usuarios.existsById(administrador.getId())).isTrue();
    }

    @Test
    void configuracionMuestraConfirmacionSinApariencia() throws Exception {
        mvc.perform(get("/cliente/configuracion.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"delete-account-dialog\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"appearance-title\""))));
    }
}
