package sv.edu.udb.ucacfcconnect.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.EspacioRequest;
import sv.edu.udb.ucacfcconnect.dto.recepcion.RecepcionRequests.ServicioRequest;
import sv.edu.udb.ucacfcconnect.service.AdminOperacionesService;
import sv.edu.udb.ucacfcconnect.service.RecepcionEspaciosService;
import sv.edu.udb.ucacfcconnect.service.RecepcionOperacionesService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/operaciones")
public class AdminOperacionesController {
    private final RecepcionEspaciosService espacios;
    private final RecepcionOperacionesService operaciones;
    private final AdminOperacionesService administracion;

    public AdminOperacionesController(RecepcionEspaciosService espacios,
                                      RecepcionOperacionesService operaciones,
                                      AdminOperacionesService administracion) {
        this.espacios = espacios;
        this.operaciones = operaciones;
        this.administracion = administracion;
    }

    @GetMapping("/espacios")
    public List<RecepcionEspaciosService.EspacioView> espacios() { return espacios.espacios(); }

    @PostMapping("/espacios")
    @ResponseStatus(HttpStatus.CREATED)
    public RecepcionEspaciosService.EspacioView crearEspacio(@Valid @RequestBody EspacioRequest request) {
        return espacios.guardar(null, request);
    }

    @GetMapping("/servicios-catering")
    public List<Map<String, Object>> servicios() { return operaciones.servicios(); }

    @PostMapping("/servicios-catering")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearServicio(@Valid @RequestBody ServicioRequest request) {
        return operaciones.registrarServicio(request);
    }

    @GetMapping("/pagos")
    public List<Map<String, Object>> pagos() { return operaciones.pagos(); }

    @GetMapping("/cotizaciones")
    public List<Map<String, Object>> cotizaciones() { return operaciones.cotizaciones(); }

    @PatchMapping("/cotizaciones/{id}/aprobar")
    public AdminOperacionesService.CotizacionAprobada aprobar(@PathVariable Long id) {
        return administracion.aprobarCotizacion(id);
    }

    @GetMapping("/reportes")
    public AdminOperacionesService.ReporteResumen reporte(@RequestParam LocalDate desde,
                                                           @RequestParam LocalDate hasta) {
        return administracion.reporte(desde, hasta);
    }
}
