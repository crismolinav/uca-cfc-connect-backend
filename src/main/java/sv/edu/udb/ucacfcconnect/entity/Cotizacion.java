package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cotizaciones")
public class Cotizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion") private Long id;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, length = 30) private String estado;
    @Column(name = "monto_estimado", precision = 10, scale = 2) private BigDecimal montoEstimado;
    @Column(length = 255) private String observaciones;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_cotizacion_cliente"))
    private Cliente cliente;
    @OneToMany(mappedBy = "cotizacion") private List<DetalleCotizacion> detalles = new ArrayList<>();
    @OneToMany(mappedBy = "cotizacion") private List<Pago> pagos = new ArrayList<>();

    public Cotizacion() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; } public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getMontoEstimado() { return montoEstimado; } public void setMontoEstimado(BigDecimal montoEstimado) { this.montoEstimado = montoEstimado; }
    public String getObservaciones() { return observaciones; } public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Cliente getCliente() { return cliente; } public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public List<DetalleCotizacion> getDetalles() { return detalles; } public void setDetalles(List<DetalleCotizacion> detalles) { this.detalles = detalles; }
    public List<Pago> getPagos() { return pagos; } public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}
