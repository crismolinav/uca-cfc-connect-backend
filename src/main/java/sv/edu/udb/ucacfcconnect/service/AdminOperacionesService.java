package sv.edu.udb.ucacfcconnect.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Cotizacion;
import sv.edu.udb.ucacfcconnect.entity.Pago;
import sv.edu.udb.ucacfcconnect.exception.ApiException;
import sv.edu.udb.ucacfcconnect.repository.AlquilerRepository;
import sv.edu.udb.ucacfcconnect.repository.CotizacionRepository;
import sv.edu.udb.ucacfcconnect.repository.PagoRepository;
import sv.edu.udb.ucacfcconnect.repository.SolicitudCateringRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminOperacionesService {
    private final CotizacionRepository cotizaciones;
    private final PagoRepository pagos;
    private final AlquilerRepository alquileres;
    private final SolicitudCateringRepository catering;

    public AdminOperacionesService(CotizacionRepository cotizaciones, PagoRepository pagos,
                                   AlquilerRepository alquileres, SolicitudCateringRepository catering) {
        this.cotizaciones = cotizaciones;
        this.pagos = pagos;
        this.alquileres = alquileres;
        this.catering = catering;
    }

    @Transactional
    public CotizacionAprobada aprobarCotizacion(Long id) {
        Cotizacion cotizacion = cotizaciones.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró la cotización"));
        if (!"PENDIENTE".equals(cotizacion.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se puede aprobar una cotización pendiente");
        }
        cotizacion.setEstado("APROBADA");
        return new CotizacionAprobada(cotizacion.getId(), cotizacion.getEstado());
    }

    @Transactional(readOnly = true)
    public ReporteResumen reporte(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona un rango de fechas válido");
        }
        List<Pago> pagosPeriodo = pagos.findAll().stream()
                .filter(p -> enRango(p.getFecha(), desde, hasta)).toList();
        long pagosConfirmados = pagosPeriodo.stream().filter(p -> "CONFIRMADO".equals(p.getEstado())).count();
        BigDecimal montoConfirmado = pagosPeriodo.stream().filter(p -> "CONFIRMADO".equals(p.getEstado()))
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = pagosPeriodo.stream().filter(p -> "PENDIENTE".equals(p.getEstado()))
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteResumen(desde, hasta, alquileres.findAll().stream()
                .filter(a -> enRango(a.getFecha(), desde, hasta) && !"CANCELADO".equals(a.getEstado())).count(),
                catering.findAll().stream().filter(c -> enRango(c.getFecha(), desde, hasta)).count(),
                cotizaciones.findAll().stream().filter(c -> enRango(c.getFecha(), desde, hasta)).count(),
                pagosPeriodo.size(), pagosConfirmados, montoConfirmado, montoPendiente);
    }

    private boolean enRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public record CotizacionAprobada(Long id, String estado) {}
    public record ReporteResumen(LocalDate desde, LocalDate hasta, long reservas, long solicitudesCatering,
                                 long cotizaciones, long pagos, long pagosConfirmados,
                                 BigDecimal montoConfirmado, BigDecimal montoPendiente) {}
}
