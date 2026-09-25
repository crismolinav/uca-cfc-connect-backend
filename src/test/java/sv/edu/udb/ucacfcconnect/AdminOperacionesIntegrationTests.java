package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.CotizacionRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.PagoRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ServicioRequest;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Rol;
import sv.edu.udb.ucacfcconnect.entity.Usuario;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.RolRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;
import sv.edu.udb.ucacfcconnect.service.JwtService;
import sv.edu.udb.ucacfcconnect.service.RecepcionOperacionesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOperacionesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired JwtService jwt;
    @Autowired RecepcionOperacionesService operaciones;

    @Test
    void soloAdminPuedeCrearRecursosYConsultarPagos() throws Exception {
        String admin = token("ADMIN", "admin-operaciones@example.com");
        String recepcion = token("RECEPCIONISTA", "recepcion-operaciones@example.com");
        String espacio = """
                {"nombre":"Sala de reuniones","tipo":"Sala","capacidad":12,
                 "precio":25.00,"equipamiento":"Proyector","disponible":true}
                """;
        mvc.perform(post("/api/v1/admin/operaciones/espacios").with(csrf())
                        .header("Authorization", "Bearer " + recepcion)
                        .contentType(MediaType.APPLICATION_JSON).content(espacio))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/operaciones/espacios").with(csrf())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(espacio))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Sala de reuniones"));
        mvc.perform(get("/api/v1/admin/operaciones/espacios")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Sala de reuniones"));
        mvc.perform(post("/api/v1/admin/operaciones/servicios-catering").with(csrf())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Coffee Break","tipoServicio":"Refrigerio",
                                 "precioBase":12.50,"descripcion":"Café y fruta"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Coffee Break"));
        mvc.perform(get("/api/v1/admin/operaciones/pagos")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/operaciones/pagos")
                        .header("Authorization", "Bearer " + recepcion))
                .andExpect(status().isForbidden());
    }

    @Test
    void apruebaCotizacionSoloUnaVezYReporteFiltraFechas() throws Exception {
        String admin = token("ADMIN", "admin-reportes@example.com");
        String recepcion = token("RECEPCIONISTA", "recepcion-reportes@example.com");
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente reporte");
        cliente = clientes.save(cliente);
        Long servicioId = ((Number) operaciones.registrarServicio(new ServicioRequest(
                "Almuerzo", "Alimentación", new BigDecimal("10.00"), null)).get("id")).longValue();
        Long cotizacionId = ((Number) operaciones.cotizar(new CotizacionRequest(
                cliente.getId(), "CATERING", servicioId, 3, null)).get("id")).longValue();
        Long pagoId = ((Number) operaciones.registrarPago(new PagoRequest(
                cliente.getId(), new BigDecimal("30.00"), "EFECTIVO", null,
                "COTIZACION", cotizacionId)).get("id")).longValue();
        operaciones.confirmarPago(pagoId);

        mvc.perform(patch("/api/v1/admin/operaciones/cotizaciones/" + cotizacionId + "/aprobar")
                        .with(csrf()).header("Authorization", "Bearer " + recepcion))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/operaciones/cotizaciones/" + cotizacionId + "/aprobar")
                        .with(csrf()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"));
        mvc.perform(patch("/api/v1/admin/operaciones/cotizaciones/" + cotizacionId + "/aprobar")
                        .with(csrf()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));
        mvc.perform(get("/api/v1/admin/operaciones/reportes")
                        .param("desde", hoy.toString()).param("hasta", hoy.toString())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotizaciones").value(1))
                .andExpect(jsonPath("$.pagosConfirmados").value(1))
                .andExpect(jsonPath("$.montoConfirmado").value(30.0));
        mvc.perform(get("/api/v1/admin/operaciones/reportes")
                        .param("desde", hoy.plusDays(1).toString()).param("hasta", hoy.plusDays(2).toString())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagos").value(0));
        mvc.perform(get("/api/v1/admin/operaciones/reportes")
                        .param("desde", hoy.toString()).param("hasta", hoy.minusDays(1).toString())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
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
}
