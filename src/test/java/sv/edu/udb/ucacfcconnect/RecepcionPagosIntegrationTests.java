package sv.edu.udb.ucacfcconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.*;
import sv.edu.udb.ucacfcconnect.entity.*;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.*;
import sv.edu.udb.ucacfcconnect.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecepcionPagosIntegrationTests {
    @Autowired AdminClienteService clientesService;
    @Autowired ClienteRepository clientes;
    @Autowired RecepcionOperacionesService operaciones;
    @Autowired InscripcionRepository inscripciones;
    @Autowired ParticipanteRepository participantes;
    @Autowired PagoRepository pagos;
    @Autowired CursoRepository cursos;
    @Autowired DiplomadoRepository diplomados;
    @Autowired CategoriaRepository categorias;
    @Autowired ModalidadRepository modalidades;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;

    private Cliente cliente(String nombre) {
        return clientes.findById(clientesService.crear(new ClienteRequest(nombre, null, "Portillo", "Díaz",
                null, null, null, null, null)).id()).orElseThrow();
    }

    private Inscripcion inscripcion(Cliente cliente, boolean diplomado) {
        Categoria categoria = new Categoria(); categoria.setNombre("Pagos prueba"); categoria = categorias.save(categoria);
        Modalidad modalidad = new Modalidad(); modalidad.setNombre("Pagos virtual"); modalidad = modalidades.save(modalidad);
        Inscripcion i = new Inscripcion();
        i.setParticipante(participantes.findFirstByClienteIdOrderByIdAsc(cliente.getId()).orElseThrow());
        i.setFecha(LocalDate.now()); i.setEstado("PENDIENTE"); i.setTotal(new BigDecimal("125.50"));
        if (diplomado) {
            Diplomado d = new Diplomado(); d.setNombre("Diplomado prueba"); d.setCosto(new BigDecimal("999.00"));
            d.setDuracionHoras(40); d.setCategoria(categoria); d.setModalidad(modalidad);
            i.setDiplomado(diplomados.save(d));
        } else {
            Curso c = new Curso(); c.setTitulo("Curso prueba"); c.setCosto(new BigDecimal("999.00"));
            c.setDuracionHoras(8); c.setCupoMaximo(10); c.setCategoria(categoria); c.setModalidad(modalidad);
            i.setCurso(cursos.save(c));
        }
        return inscripciones.saveAndFlush(i);
    }

    private PagoRequest solicitud(Cliente cliente, Inscripcion i, String monto) {
        return new PagoRequest(cliente.getId(), new BigDecimal(monto), "EFECTIVO", null, "INSCRIPCION", i.getId());
    }

    @Test
    void listaCursosYDiplomadosPorClienteConElTotalAcordadoDeInscripcion() {
        Cliente ana = cliente("Ana"), luis = cliente("Luis");
        Inscripcion curso = inscripcion(ana, false), diplomado = inscripcion(ana, true), otro = inscripcion(luis, false);
        var lista = operaciones.inscripcionesPendientesPago(ana.getId());
        assertThat(lista).hasSize(2).allSatisfy(i -> {
            assertThat(i.get("clienteId")).isEqualTo(ana.getId());
            assertThat(i.get("total")).isEqualTo(new BigDecimal("125.50"));
        });
        assertThat(lista).extracting(i -> i.get("programaTipo")).containsExactlyInAnyOrder("CURSO", "DIPLOMADO");
        assertThat(lista).extracting(i -> i.get("id")).containsExactlyInAnyOrder(curso.getId(), diplomado.getId()).doesNotContain(otro.getId());
    }

    @Test
    void registrarPagoLoRetiraDeOpcionesYConfirmarloLoRetiraDePendientesSinBorrarHistorial() {
        Cliente cliente = cliente("Ana");
        for (boolean diplomado : new boolean[]{false, true}) {
            Inscripcion i = inscripcion(cliente, diplomado);
            var creado = operaciones.registrarPago(solicitud(cliente, i, "125.50"));
            Long pagoId = ((Number) creado.get("id")).longValue();
            assertThat(operaciones.inscripcionesPendientesPago(cliente.getId())).noneMatch(item -> item.get("id").equals(i.getId()));
            assertThat(operaciones.pagos()).anyMatch(p -> p.get("id").equals(pagoId));
            assertThatThrownBy(() -> operaciones.registrarPago(solicitud(cliente, i, "125.50")))
                    .isInstanceOf(ApiException.class).hasMessageContaining("ya tiene un pago");
            operaciones.confirmarPago(pagoId);
            assertThat(i.getEstado()).isEqualTo("CONFIRMADA");
            assertThat(operaciones.pagos()).noneMatch(p -> p.get("id").equals(pagoId));
            assertThat(pagos.findById(pagoId)).hasValueSatisfying(p -> assertThat(p.getEstado()).isEqualTo("CONFIRMADO"));
            assertThat(historial(operaciones.historialPagos(null, null, "CONFIRMADO", null, cliente.getId(), "", 0, 10)))
                    .anyMatch(p -> p.get("id").equals(pagoId));
            assertThatThrownBy(() -> operaciones.registrarPago(solicitud(cliente, i, "125.50")))
                    .isInstanceOf(ApiException.class).hasMessageContaining("inscripción pendiente");
            assertThatThrownBy(() -> operaciones.confirmarPago(pagoId)).isInstanceOf(ApiException.class);
        }
    }

    @Test
    void rechazaMontoManipuladoOtroClienteYEstadosNoCobrables() {
        Cliente ana = cliente("Ana"), luis = cliente("Luis");
        Inscripcion i = inscripcion(ana, false);
        long antes = pagos.count();
        assertThatThrownBy(() -> operaciones.registrarPago(solicitud(ana, i, "1.00")))
                .isInstanceOf(ApiException.class).hasMessageContaining("coincidir con el total");
        assertThatThrownBy(() -> operaciones.registrarPago(solicitud(luis, i, "125.50")))
                .isInstanceOf(ApiException.class).hasMessageContaining("no pertenece");
        for (String estado : new String[]{"CANCELADA", "CONFIRMADA"}) {
            i.setEstado(estado); inscripciones.saveAndFlush(i);
            assertThat(operaciones.inscripcionesPendientesPago(ana.getId())).noneMatch(row -> row.get("id").equals(i.getId()));
            assertThatThrownBy(() -> operaciones.registrarPago(solicitud(ana, i, "125.50")))
                    .isInstanceOf(ApiException.class).hasMessageContaining("inscripción pendiente");
        }
        assertThat(pagos.count()).isEqualTo(antes);
    }

    @Test
    void pagosDelPortalTambienImpidenDuplicarYCobroRechazadoPermiteReintento() {
        Cliente cliente = cliente("Ana"); Inscripcion i = inscripcion(cliente, true);
        Pago pago = new Pago(); pago.setCliente(cliente); pago.setInscripcion(i); pago.setMonto(i.getTotal());
        pago.setFecha(LocalDate.now()); pago.setMetodo("TRANSFERENCIA"); pago.setEstado("PENDIENTE");
        pago = pagos.saveAndFlush(pago);
        assertThat(operaciones.inscripcionesPendientesPago(cliente.getId())).isEmpty();
        assertThatThrownBy(() -> operaciones.registrarPago(solicitud(cliente, i, "125.50"))).isInstanceOf(ApiException.class);
        pago.setEstado("RECHAZADO"); pagos.saveAndFlush(pago);
        assertThat(operaciones.inscripcionesPendientesPago(cliente.getId())).hasSize(1);
        operaciones.registrarPago(solicitud(cliente, i, "125.50"));
        assertThat(operaciones.inscripcionesPendientesPago(cliente.getId())).isEmpty();
    }

    @Test
    void confirmacionRechazaImportesIncorrectosYConfirmadosPreviosAunqueLaInscripcionEstePendiente() {
        Cliente cliente = cliente("Ana"); Inscripcion i = inscripcion(cliente, false);
        Pago pago = new Pago(); pago.setCliente(cliente); pago.setInscripcion(i); pago.setMonto(new BigDecimal("1.00"));
        pago.setFecha(LocalDate.now()); pago.setMetodo("EFECTIVO"); pago.setEstado("PENDIENTE");
        pago = pagos.saveAndFlush(pago); Long pagoId = pago.getId();
        assertThatThrownBy(() -> operaciones.confirmarPago(pagoId)).isInstanceOf(ApiException.class).hasMessageContaining("no coincide");
        assertThat(i.getEstado()).isEqualTo("PENDIENTE");
        pago.setMonto(i.getTotal()); pago.setEstado("CONFIRMADO"); pagos.saveAndFlush(pago);
        assertThat(operaciones.inscripcionesPendientesPago(cliente.getId())).isEmpty();
        assertThatThrownBy(() -> operaciones.registrarPago(solicitud(cliente, i, "125.50"))).isInstanceOf(ApiException.class);
    }

    @Test
    void opcionesDeCobroRequierenRecepcionista() throws Exception {
        mvc.perform(get("/api/v1/recepcion/inscripciones/pendientes-pago")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/recepcion/pagos/historial")).andExpect(status().isUnauthorized());
        Rol rol = new Rol(); rol.setNombre("CLIENTE"); rol = roles.save(rol);
        Usuario usuario = new Usuario(); usuario.setNombre("Cliente"); usuario.setCorreo("pagos-permisos@example.test");
        usuario.setRol(rol); usuario.setActivo(true); usuario = usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/v1/recepcion/inscripciones/pendientes-pago").header("Authorization", "Bearer " + jwt.generarToken(usuario)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/recepcion/pagos/historial").header("Authorization", "Bearer " + jwt.generarToken(usuario)))
                .andExpect(status().isForbidden());
    }

    private Pago pagoHistorico(Cliente cliente, LocalDate fecha, String estado, String metodo, String referencia) {
        Pago pago = new Pago(); pago.setCliente(cliente); pago.setFecha(fecha); pago.setEstado(estado);
        pago.setMetodo(metodo); pago.setReferencia(referencia); pago.setMonto(new BigDecimal("25.00"));
        return pagos.saveAndFlush(pago);
    }

    @SuppressWarnings("unchecked")
    private java.util.List<java.util.Map<String, Object>> historial(java.util.Map<String, Object> pagina) {
        return (java.util.List<java.util.Map<String, Object>>) pagina.get("content");
    }

    @Test
    void historialCombinaFiltrosIncluyeAmbasFechasLimiteYBuscaCarnetOReferencia() {
        Cliente ana = cliente("Ana"), luis = cliente("Luis");
        LocalDate desde = LocalDate.of(2026, 10, 1), hasta = LocalDate.of(2026, 10, 31);
        Pago inicio = pagoHistorico(ana, desde, "CONFIRMADO", "EFECTIVO", "FILTRO-INICIO");
        Pago fin = pagoHistorico(ana, hasta, "CONFIRMADO", "EFECTIVO", "FILTRO-FIN");
        pagoHistorico(ana, desde.minusDays(1), "CONFIRMADO", "EFECTIVO", "FUERA");
        pagoHistorico(ana, desde.plusDays(10), "RECHAZADO", "EFECTIVO", "RECHAZADO");
        pagoHistorico(ana, desde.plusDays(10), "CONFIRMADO", "TRANSFERENCIA", "OTRO-METODO");
        pagoHistorico(luis, desde, "CONFIRMADO", "EFECTIVO", "OTRO-CLIENTE");
        var resultado = operaciones.historialPagos(desde, hasta, "confirmado", "efectivo", ana.getId(), "  ana  ", 0, 10);
        assertThat(resultado.get("totalElements")).isEqualTo(2L);
        assertThat(historial(resultado)).extracting(p -> p.get("id")).containsExactly(fin.getId(), inicio.getId());
        assertThat(historial(operaciones.historialPagos(null, null, null, null, null, ana.getCarnet().toLowerCase(), 0, 10))).hasSize(5);
        assertThat(historial(operaciones.historialPagos(null, null, null, null, ana.getId(), "filtro-", 0, 10))).hasSize(2);
    }

    @Test
    void historialPaginaConOrdenEstableYConsultaSinModificarLosPagos() {
        Cliente ana = cliente("Ana");
        Pago ultimo = null;
        for (int index = 0; index < 25; index++) {
            ultimo = pagoHistorico(ana, LocalDate.of(2026, 10, 15), index % 2 == 0 ? "CONFIRMADO" : "PENDIENTE", "TARJETA", "PAGINA-" + index);
        }
        long antes = pagos.count();
        var primera = operaciones.historialPagos(null, null, null, null, ana.getId(), "", 0, 10);
        var ultima = operaciones.historialPagos(null, null, null, null, ana.getId(), "", 2, 10);
        assertThat(primera.get("totalElements")).isEqualTo(25L);
        assertThat(primera.get("totalPages")).isEqualTo(3);
        assertThat(historial(primera)).hasSize(10);
        assertThat(historial(primera).get(0).get("id")).isEqualTo(ultimo.getId());
        assertThat(historial(ultima)).hasSize(5);
        assertThat(historial(primera)).extracting(p -> p.get("id"))
                .doesNotContainAnyElementsOf(historial(ultima).stream().map(p -> p.get("id")).toList());
        assertThat(historial(operaciones.historialPagos(null, null, "PENDIENTE", null, ana.getId(), "", 0, 50))).hasSize(12);
        assertThat(pagos.count()).isEqualTo(antes);
    }

    @Test
    void apiHistorialValidaFechasEstadosYTamanosDePagina() throws Exception {
        Rol rol = roles.findByNombreIgnoreCase("RECEPCIONISTA").orElseGet(() -> {
            Rol nuevo = new Rol(); nuevo.setNombre("RECEPCIONISTA"); return roles.save(nuevo);
        });
        Usuario usuario = new Usuario(); usuario.setNombre("Recepción historial"); usuario.setCorreo("historial-permisos@example.test");
        usuario.setRol(rol); usuario.setActivo(true); usuario = usuarios.saveAndFlush(usuario);
        String token = jwt.generarToken(usuario);
        mvc.perform(get("/api/v1/recepcion/pagos/historial").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/recepcion/pagos/historial").param("desde", "2026-10-31").param("hasta", "2026-10-01")
                        .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        for (var filtro : java.util.Map.of("page", "-1", "size", "101", "estado", "INVALIDO", "metodo", "INVALIDO", "clienteId", "0", "q", "a".repeat(121), "desde", "2026-99-99").entrySet()) {
            mvc.perform(get("/api/v1/recepcion/pagos/historial").param(filtro.getKey(), filtro.getValue())
                            .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        }
    }
}
