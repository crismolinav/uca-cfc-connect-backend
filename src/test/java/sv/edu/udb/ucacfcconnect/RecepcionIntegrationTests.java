package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ActividadRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.CateringRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.CotizacionRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.InscripcionRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.PagoRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ParticipanteRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ServicioRequest;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.AlquilerRepository;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.EspacioRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;
import sv.edu.udb.ucacfcconnect.service.RecepcionOperacionesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecepcionIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired EspacioRepository espacios;
    @Autowired AlquilerRepository alquileres;
    @Autowired CategoriaRepository categorias;
    @Autowired ModalidadRepository modalidades;
    @Autowired CursoRepository cursos;
    @Autowired JwtService jwt;
    @Autowired RecepcionOperacionesService operaciones;

    @Test
    void adminCreaRecepcionistaQuePuedeEntrarPeroNoAdministrar() throws Exception {
        String admin = token("ADMIN", "admin-recepcion@example.com");
        String cliente = token("CLIENTE", "cliente-recepcion@example.com");
        String body = """
                {"nombre":"Ana Recepción","correo":"ANA.RECEPCION@example.com","password":"ClaveTemporal123"}
                """;
        mvc.perform(post("/api/v1/admin/recepcionistas").with(csrf())
                        .header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/recepcionistas").with(csrf())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana.recepcion@example.com"));
        Usuario recepcionista = usuarios.findByCorreoIgnoreCase("ana.recepcion@example.com").orElseThrow();
        assertThat(recepcionista.getPassword()).isNotEqualTo("ClaveTemporal123");
        assertThat(recepcionista.getRol().getNombre()).isEqualTo("RECEPCIONISTA");
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana.recepcion@example.com","password":"ClaveTemporal123","recordar":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("RECEPCIONISTA"));
        String recepcionToken = jwt.generarToken(recepcionista);
        mvc.perform(get("/api/v1/admin/recepcionistas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("ana.recepcion@example.com"));
        mvc.perform(get("/api/v1/recepcion/espacios").header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/clientes").header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/recepcion/espacios").header("Authorization", "Bearer " + cliente))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/recepcionistas/" + recepcionista.getId() + "/estado").with(csrf())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/v1/recepcion/espacios").header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservasSolapadasSeRechazanPeroHorarioContiguoSePermite() throws Exception {
        String recepcion = token("RECEPCIONISTA", "agenda-recepcion@example.com");
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Agenda");
        cliente = clientes.save(cliente);
        mvc.perform(post("/api/v1/recepcion/espacios").with(csrf())
                        .header("Authorization", "Bearer " + recepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cubículo 1","tipo":"Cubículo","capacidad":4,
                                 "precio":20.00,"equipamiento":"Wi-Fi","disponible":true}
                                """))
                .andExpect(status().isCreated());
        Long espacioId = espacios.findAll().stream().filter(e -> "Cubículo 1".equals(e.getNombre()))
                .findFirst().orElseThrow().getId();
        LocalDate manana = LocalDate.now(ZoneId.of("America/El_Salvador")).plusDays(1);
        String primera = reserva(cliente.getId(), espacioId, manana, "09:00", "10:00");
        mvc.perform(post("/api/v1/recepcion/alquileres").with(csrf())
                        .header("Authorization", "Bearer " + recepcion)
                        .contentType(MediaType.APPLICATION_JSON).content(primera))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/recepcion/alquileres").with(csrf())
                        .header("Authorization", "Bearer " + recepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva(cliente.getId(), espacioId, manana, "09:30", "10:30")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/recepcion/alquileres").with(csrf())
                        .header("Authorization", "Bearer " + recepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva(cliente.getId(), espacioId, manana, "10:00", "11:00")))
                .andExpect(status().isCreated());
        assertThat(alquileres.findByFechaOrderByHoraInicioAsc(manana)).hasSize(2);
    }

    @Test
    void cateringCotizacionActividadYPagoConservanSusRelaciones() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Catering");
        cliente = clientes.save(cliente);
        Map<String, Object> servicio = operaciones.registrarServicio(new ServicioRequest(
                "Coffee Break", "Coffee Break", new BigDecimal("12.50"), "Bebida y refrigerio"));
        Long servicioId = ((Number) servicio.get("id")).longValue();
        LocalDate manana = LocalDate.now(ZoneId.of("America/El_Salvador")).plusDays(1);
        Map<String, Object> solicitud = operaciones.solicitarCatering(new CateringRequest(
                cliente.getId(), servicioId, manana, LocalTime.of(10, 0), "Aula 1", 12, "Café y fruta"));
        Long solicitudId = ((Number) solicitud.get("id")).longValue();
        Map<String, Object> cotizacion = operaciones.cotizar(new CotizacionRequest(
                cliente.getId(), "CATERING", servicioId, 12, "Evento del equipo"));
        assertThat(cotizacion.get("montoEstimado")).isEqualTo(new BigDecimal("150.00"));
        Map<String, Object> actividad = operaciones.programarActividad(new ActividadRequest(
                "Preparar servicio", manana, LocalTime.of(9, 0), LocalTime.of(10, 0), 12,
                "CATERING", solicitudId));
        assertThat(actividad.get("tipo")).isEqualTo("CATERING");
        Map<String, Object> pago = operaciones.registrarPago(new PagoRequest(
                cliente.getId(), new BigDecimal("150.00"), "TRANSFERENCIA", "REF-123",
                "CATERING", solicitudId));
        assertThat(pago.get("estado")).isEqualTo("PENDIENTE");
        assertThat(operaciones.confirmarPago(((Number) pago.get("id")).longValue()).get("estado"))
                .isEqualTo("CONFIRMADO");
    }

    @Test
    void inscripcionVinculaParticipanteYRespetaCupoDelCurso() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Formación");
        cliente = clientes.save(cliente);
        Map<String, Object> participante = operaciones.registrarParticipante(new ParticipanteRequest(
                cliente.getId(), "Luis", "Prueba", "luis@example.test", null));
        Categoria categoria = new Categoria();
        categoria.setNombre("Pruebas de formación");
        categoria = categorias.save(categoria);
        Modalidad modalidad = new Modalidad();
        modalidad.setNombre("Presencial prueba");
        modalidad = modalidades.save(modalidad);
        Curso curso = new Curso();
        curso.setTitulo("Curso de prueba para recepción");
        curso.setDuracionHoras(8);
        curso.setCupoMaximo(1);
        curso.setCosto(new BigDecimal("35.00"));
        curso.setCategoria(categoria);
        curso.setModalidad(modalidad);
        curso = cursos.save(curso);
        Long participanteId = ((Number) participante.get("id")).longValue();
        Map<String, Object> inscripcion = operaciones.inscribir(new InscripcionRequest(
                participanteId, "CURSO", curso.getIdCurso()));
        assertThat(inscripcion.get("total")).isEqualTo(new BigDecimal("35.00"));
        assertThat(inscripcion.get("estado")).isEqualTo("PENDIENTE");
        assertThat(operaciones.inscripciones()).anyMatch(i -> i.get("id").equals(inscripcion.get("id")));
        Map<String, Object> segundo = operaciones.registrarParticipante(new ParticipanteRequest(
                cliente.getId(), "Ana", "Prueba", "ana@example.test", null));
        Long segundoId = ((Number) segundo.get("id")).longValue();
        Long cursoId = curso.getIdCurso();
        assertThatThrownBy(() -> operaciones.inscribir(new InscripcionRequest(segundoId, "CURSO", cursoId)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cupo máximo");
    }

    private String token(String rolNombre, String correo) {
        Rol rol = roles.findByNombreIgnoreCase(rolNombre).orElseGet(() -> {
            Rol nuevo = new Rol(); nuevo.setNombre(rolNombre); return roles.save(nuevo);
        });
        Usuario usuario = new Usuario();
        usuario.setNombre(rolNombre);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return jwt.generarToken(usuarios.save(usuario));
    }

    private String reserva(Long clienteId, Long espacioId, LocalDate fecha, String inicio, String fin) {
        return "{\"clienteId\":" + clienteId + ",\"espacioId\":" + espacioId +
                ",\"fecha\":\"" + fecha + "\",\"horaInicio\":\"" + inicio +
                "\",\"horaFin\":\"" + fin + "\",\"motivo\":\"Reunión\"}";
    }
}
