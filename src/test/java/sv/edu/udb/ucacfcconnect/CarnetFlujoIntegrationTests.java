package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.InscripcionCursoRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.*;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.*;
import sv.edu.udb.ucacfcconnect.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarnetFlujoIntegrationTests {
    @Autowired AdminClienteService clientesService;
    @Autowired RecepcionOperacionesService recepcion;
    @Autowired ClienteFormacionService formacion;
    @Autowired ClienteRepository clientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired ParticipanteRepository participantes;
    @Autowired CursoRepository cursos;
    @Autowired DiplomadoRepository diplomados;
    @Autowired CategoriaRepository categorias;
    @Autowired ModalidadRepository modalidades;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired InvitacionClienteService invitaciones;

    private ClienteRequest ficha(String nombre, String apellido) {
        return new ClienteRequest(nombre, "María", apellido, "Díaz", null, null, null, null, null);
    }

    @Test
    void carnetAutomaticoUnicoPersistenteYNoEditableDesdeElApi() throws Exception {
        var primera = clientesService.crear(ficha("Ana", "Portillo"));
        var segunda = clientesService.crear(ficha("Ana", "Portillo"));
        String prefijo = "PD" + LocalDate.now(ZoneId.of("America/El_Salvador")).getYear();
        assertThat(primera.carnet()).matches(prefijo + "[A-F0-9]{12}").isNotEqualTo(segunda.carnet());
        assertThat(primera.nombre()).isEqualTo("Ana María Portillo Díaz");
        assertThat(clientes.findByCarnetIgnoreCase(primera.carnet().toLowerCase())).isPresent();
        String token = token("RECEPCIONISTA", "recepcion-carnet@example.test");
        mvc.perform(put("/api/v1/recepcion/clientes/" + primera.id()).with(csrf())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"primerNombre":"Ana","segundoNombre":"María","primerApellido":"Álvarez",
                                 "segundoApellido":"Díaz","carnet":"PD2026ALTERADO"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.carnet").value(primera.carnet()))
                .andExpect(jsonPath("$.primerApellido").value("Álvarez"));
        assertThat(clientesService.detalle(primera.id()).carnet()).isEqualTo(primera.carnet());
        assertThat(clientesService.listar(primera.carnet(), 0, 20).content()).hasSize(1);
    }

    @Test
    void clienteCreadoYaEsParticipanteYRepetirElAltaNoLoDuplica() {
        assertThatThrownBy(() -> recepcion.buscarCarnet("XX2026INEXISTENTE"))
                .isInstanceOf(ApiException.class).hasMessageContaining("No existe un cliente");
        var cliente = clientesService.crear(ficha("Lucía", "Ñúñez"));
        assertThat(cliente.carnet()).startsWith("ND");
        assertThat(recepcion.buscarCarnet(cliente.carnet())).containsEntry("esParticipante", true);
        var participante = recepcion.registrarParticipante(new ParticipanteRequest(cliente.carnet()));
        assertThat(participante).containsEntry("nombre", "Lucía María").containsEntry("apellido", "Ñúñez Díaz");
        assertThat(recepcion.buscarCarnet(cliente.carnet())).containsEntry("esParticipante", true).containsEntry("puedeInscribirse", true);
        assertThat(recepcion.registrarParticipante(new ParticipanteRequest(cliente.carnet())).get("id"))
                .isEqualTo(participante.get("id"));
        assertThat(participantes.findAll().stream().filter(p -> cliente.id().equals(p.getCliente().getId()))).hasSize(1);
    }

    @Test
    void participanteSinCuentaPuedeInscribirseEnCursoYDiplomadoSinDuplicarse() {
        var cliente = clientesService.crear(ficha("Elena", "López"));
        Categoria categoria = new Categoria(); categoria.setNombre("Carnet prueba"); categoria = categorias.save(categoria);
        Modalidad modalidad = new Modalidad(); modalidad.setNombre("Virtual carnet"); modalidad = modalidades.save(modalidad);
        Curso curso = new Curso(); curso.setTitulo("Curso carnet"); curso.setDuracionHoras(10); curso.setCupoMaximo(5);
        curso.setCosto(new BigDecimal("25.00")); curso.setCategoria(categoria); curso.setModalidad(modalidad);
        curso = cursos.save(curso);
        Diplomado diplomado = new Diplomado(); diplomado.setNombre("Diplomado carnet");
        diplomado.setCosto(new BigDecimal("100.00")); diplomado.setDuracionHoras(40);
        diplomado.setCategoria(categoria); diplomado.setModalidad(modalidad); diplomado = diplomados.save(diplomado);
        Long cursoId = curso.getIdCurso(), diplomadoId = diplomado.getId();
        assertThat(recepcion.inscribir(new InscripcionRequest(cliente.carnet(), "CURSO", cursoId)))
                .containsEntry("estado", "PENDIENTE").containsEntry("carnet", cliente.carnet());
        assertThat(recepcion.inscribir(new InscripcionRequest(cliente.carnet(), "DIPLOMADO", diplomadoId)))
                .containsEntry("programa", "Diplomado carnet");
        assertThatThrownBy(() -> recepcion.inscribir(new InscripcionRequest(cliente.carnet(), "CURSO", cursoId)))
                .isInstanceOf(ApiException.class).hasMessageContaining("ya está inscrito");
        assertThatThrownBy(() -> recepcion.inscribir(new InscripcionRequest(cliente.carnet(), "DIPLOMADO", diplomadoId)))
                .isInstanceOf(ApiException.class).hasMessageContaining("ya está inscrito");
    }

    @Test
    void portalRecibeSuParticipanteAutomaticamenteYCarnetEsVisibleSoloEnSuCuenta() throws Exception {
        String token = token("CLIENTE", "estudiante-carnet@example.test");
        Usuario usuario = usuarios.findByCorreoIgnoreCase("estudiante-carnet@example.test").orElseThrow();
        var ficha = clientesService.crear(ficha("Ana", "Portillo"));
        Cliente cliente = clientes.findById(ficha.id()).orElseThrow();
        cliente.setUsuario(usuario); cliente.setCorreo(usuario.getCorreo()); clientes.saveAndFlush(cliente);
        usuario.setCliente(cliente);
        assertThat(participantes.existsByClienteId(cliente.getId())).isTrue();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.carnet").value(ficha.carnet()));
        mvc.perform(get("/api/v1/recepcion/clientes/carnet/" + ficha.carnet()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/recepcion/clientes/carnet/" + ficha.carnet()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void migradoConservaNombreYSoloObtieneCarnetAlConfirmarApellidos() {
        Cliente anterior = new Cliente(); anterior.setNombre("Kevin"); anterior = clientes.saveAndFlush(anterior);
        assertThat(anterior.getCarnet()).isNull();
        assertThat(clientesService.detalle(anterior.getId()).nombre()).isEqualTo("Kevin");
        var actualizado = clientesService.actualizar(anterior.getId(), ficha("Kevin", "Portillo"));
        assertThat(actualizado.carnet()).startsWith("PD");
        assertThat(participantes.existsByClienteId(anterior.getId())).isTrue();
        assertThat(clientesService.actualizar(anterior.getId(), ficha("Kevin", "López")).carnet()).isEqualTo(actualizado.carnet());
    }

    @Test
    void apiRechazaNombresIncompletosYCarnetVacio() throws Exception {
        String token = token("RECEPCIONISTA", "validacion-carnet@example.test");
        mvc.perform(post("/api/v1/recepcion/clientes").with(csrf()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"primerNombre\":\"Ana\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/recepcion/participantes").with(csrf()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"carnet\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void autorregistroPermiteInscribirseSinUnAltaManualDeParticipante() {
        var sesion = auth.registrar(new sv.edu.udb.ucacfcconnect.dto.auth.RegistroRequest(
                " Lucía ", "María", "Ñúñez", "Álvarez", "76543210-1",
                "autoregistro@example.test", "ClaveSegura123", true));
        Usuario usuario = usuarios.findByCorreoIgnoreCase("autoregistro@example.test").orElseThrow();
        Cliente cliente = clientes.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(sesion.usuario().carnet()).startsWith("NA");
        var participante = participantes.findFirstByClienteIdOrderByIdAsc(cliente.getId()).orElseThrow();
        assertThat(participante.getNombre()).isEqualTo("Lucía María");
        assertThat(participante.getApellido()).isEqualTo("Ñúñez Álvarez");
        Categoria categoria = new Categoria(); categoria.setNombre("Autorregistro"); categoria = categorias.save(categoria);
        Modalidad modalidad = new Modalidad(); modalidad.setNombre("Autorregistro"); modalidad = modalidades.save(modalidad);
        Curso curso = new Curso(); curso.setTitulo("Curso autorregistro"); curso.setDuracionHoras(8);
        curso.setCupoMaximo(3); curso.setCosto(new BigDecimal("25.00")); curso.setCategoria(categoria); curso.setModalidad(modalidad);
        curso = cursos.save(curso);
        assertThat(formacion.inscribir(usuario.getId(), new InscripcionCursoRequest(curso.getIdCurso()))).isNotNull();
        assertThat(participantes.findAll().stream().filter(p -> cliente.getId().equals(p.getCliente().getId()))).hasSize(1);
        assertThatThrownBy(() -> clientesService.crear(new ClienteRequest("Lucía", "María", "Ñúñez", "Álvarez",
                null, null, "AUTOREGISTRO@example.test", null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Ya existe un cliente con ese correo");
    }

    @Test
    void invitacionDeClienteCreadoPorRecepcionReutilizaParticipanteYCarnet() {
        var ficha = clientesService.crear(new ClienteRequest("Ana", null, "Portillo", "Díaz", "23456789-0",
                null, "invitacion-automatica@example.test", null, null));
        Long participanteId = participantes.findFirstByClienteIdOrderByIdAsc(ficha.id()).orElseThrow().getId();
        String enlace = invitaciones.invitar(ficha.id()).enlaceLocal();
        String token = enlace.substring(enlace.indexOf("#token=") + 7);
        var sesion = invitaciones.aceptar(new sv.edu.udb.ucacfcconnect.dto.auth.AceptarInvitacionRequest(
                token, "23456789-0", "ClaveSegura123", true));
        assertThat(sesion.usuario().carnet()).isEqualTo(ficha.carnet());
        assertThat(participantes.findFirstByClienteIdOrderByIdAsc(ficha.id()).orElseThrow().getId()).isEqualTo(participanteId);
        assertThat(participantes.findAll().stream().filter(p -> ficha.id().equals(p.getCliente().getId()))).hasSize(1);
    }

    private String token(String nombreRol, String correo) {
        Rol rol = roles.findByNombreIgnoreCase(nombreRol).orElseGet(() -> {
            Rol nuevo = new Rol(); nuevo.setNombre(nombreRol); return roles.save(nuevo);
        });
        Usuario usuario = new Usuario(); usuario.setNombre("Prueba carnet"); usuario.setCorreo(correo);
        usuario.setRol(rol); usuario.setActivo(true); usuario = usuarios.saveAndFlush(usuario);
        return jwt.generarToken(usuario);
    }
}
