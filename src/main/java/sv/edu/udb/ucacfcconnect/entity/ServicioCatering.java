package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "servicios_catering")
public class ServicioCatering {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_catering") private Long id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(name = "tipo_servicio", nullable = false, length = 80) private String tipoServicio;
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2) private BigDecimal precioBase;
    @Column(length = 255) private String descripcion;
    @Column(nullable = false) private boolean activo = true;
    @OneToMany(mappedBy = "servicioCatering") private List<SolicitudCatering> solicitudes = new ArrayList<>();
    @OneToMany(mappedBy = "servicioCatering") private List<DetalleCotizacion> detallesCotizacion = new ArrayList<>();

    public ServicioCatering() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipoServicio() { return tipoServicio; } public void setTipoServicio(String tipoServicio) { this.tipoServicio = tipoServicio; }
    public BigDecimal getPrecioBase() { return precioBase; } public void setPrecioBase(BigDecimal precioBase) { this.precioBase = precioBase; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isActivo() { return activo; } public void setActivo(boolean activo) { this.activo = activo; }
    public List<SolicitudCatering> getSolicitudes() { return solicitudes; } public void setSolicitudes(List<SolicitudCatering> solicitudes) { this.solicitudes = solicitudes; }
    public List<DetalleCotizacion> getDetallesCotizacion() { return detallesCotizacion; } public void setDetallesCotizacion(List<DetalleCotizacion> detalles) { this.detallesCotizacion = detalles; }
}
