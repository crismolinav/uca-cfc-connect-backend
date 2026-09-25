package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteRequest;
import sv.edu.udb.ucacfcconnect.dto.admin.ClienteResumen;
import sv.edu.udb.ucacfcconnect.dto.admin.PaginaClientes;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.*;
import sv.edu.udb.ucacfcconnect.service.AdminClienteService;
import sv.edu.udb.ucacfcconnect.service.RecepcionEspaciosService;
import sv.edu.udb.ucacfcconnect.service.RecepcionOperacionesService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recepcion")
public class RecepcionController {
    private final RecepcionEspaciosService espacios;
    private final RecepcionOperacionesService operaciones;
    private final AdminClienteService clientes;

    public RecepcionController(RecepcionEspaciosService espacios, RecepcionOperacionesService operaciones,
                               AdminClienteService clientes) {
        this.espacios = espacios;
        this.operaciones = operaciones;
        this.clientes = clientes;
    }

    @GetMapping("/catalogos")
    public Map<String, Object> catalogos() { return operaciones.catalogos(); }

    @GetMapping("/clientes")
    public PaginaClientes clientes(@RequestParam(defaultValue = "") String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return clientes.listar(q, page, size);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResumen crearCliente(@Valid @RequestBody ClienteRequest request) { return clientes.crear(request); }

    @GetMapping("/espacios")
    public List<RecepcionEspaciosService.EspacioView> espacios() { return espacios.espacios(); }

    @PostMapping("/espacios")
    @ResponseStatus(HttpStatus.CREATED)
    public RecepcionEspaciosService.EspacioView crearEspacio(@Valid @RequestBody EspacioRequest request) {
        return espacios.guardar(null, request);
    }

    @PutMapping("/espacios/{id}")
    public RecepcionEspaciosService.EspacioView actualizarEspacio(@PathVariable Long id,
                                                                   @Valid @RequestBody EspacioRequest request) {
        return espacios.guardar(id, request);
    }

    @GetMapping("/alquileres")
    public List<RecepcionEspaciosService.AlquilerView> alquileres(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return espacios.alquileres(fecha);
    }

    @PostMapping("/alquileres")
    @ResponseStatus(HttpStatus.CREATED)
    public RecepcionEspaciosService.AlquilerView reservar(@Valid @RequestBody AlquilerRequest request) {
        return espacios.reservar(request);
    }

    @PatchMapping("/alquileres/{id}/cancelar")
    public RecepcionEspaciosService.AlquilerView cancelarReserva(@PathVariable Long id) {
        return espacios.cancelar(id);
    }

    @GetMapping("/participantes")
    public List<Map<String, Object>> participantes() { return operaciones.participantes(); }

    @PostMapping("/participantes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrarParticipante(@Valid @RequestBody ParticipanteRequest request) {
        return operaciones.registrarParticipante(request);
    }

    @GetMapping("/inscripciones")
    public List<Map<String, Object>> inscripciones() { return operaciones.inscripciones(); }

    @PostMapping("/inscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> inscribir(@Valid @RequestBody InscripcionRequest request) {
        return operaciones.inscribir(request);
    }

    @GetMapping("/servicios-catering")
    public List<Map<String, Object>> serviciosCatering() { return operaciones.servicios(); }

    @PostMapping("/servicios-catering")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrarServicio(@Valid @RequestBody ServicioRequest request) {
        return operaciones.registrarServicio(request);
    }

    @GetMapping("/catering")
    public List<Map<String, Object>> catering() { return operaciones.solicitudesCatering(); }

    @PostMapping("/catering")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> solicitarCatering(@Valid @RequestBody CateringRequest request) {
        return operaciones.solicitarCatering(request);
    }

    @GetMapping("/cotizaciones")
    public List<Map<String, Object>> cotizaciones() { return operaciones.cotizaciones(); }

    @PostMapping("/cotizaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> cotizar(@Valid @RequestBody CotizacionRequest request) {
        return operaciones.cotizar(request);
    }

    @GetMapping("/actividades")
    public List<Map<String, Object>> actividades() { return operaciones.actividades(); }

    @PostMapping("/actividades")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> programarActividad(@Valid @RequestBody ActividadRequest request) {
        return operaciones.programarActividad(request);
    }

    @GetMapping("/pagos")
    public List<Map<String, Object>> pagos() { return operaciones.pagos(); }

    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrarPago(@Valid @RequestBody PagoRequest request) {
        return operaciones.registrarPago(request);
    }

    @PatchMapping("/pagos/{id}/confirmar")
    public Map<String, Object> confirmarPago(@PathVariable Long id) { return operaciones.confirmarPago(id); }
}
