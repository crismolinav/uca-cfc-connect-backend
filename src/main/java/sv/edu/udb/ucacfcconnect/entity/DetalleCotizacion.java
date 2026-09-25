package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_cotizacion")
public class DetalleCotizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_cotizacion") private Long id;
    @Column(length = 255) private String descripcion;
    @Column(nullable = false) private Integer cantidad;
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2) private BigDecimal precioUnitario;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal subtotal;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cotizacion", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_cotizacion"))
    private Cotizacion cotizacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", foreignKey = @ForeignKey(name = "fk_detalle_curso")) private Curso curso;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_diplomado", foreignKey = @ForeignKey(name = "fk_detalle_diplomado")) private Diplomado diplomado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_espacio", foreignKey = @ForeignKey(name = "fk_detalle_espacio")) private Espacio espacio;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio_catering", foreignKey = @ForeignKey(name = "fk_detalle_catering")) private ServicioCatering servicioCatering;

    public DetalleCotizacion() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getCantidad() { return cantidad; } public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; } public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; } public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Cotizacion getCotizacion() { return cotizacion; } public void setCotizacion(Cotizacion cotizacion) { this.cotizacion = cotizacion; }
    public Curso getCurso() { return curso; } public void setCurso(Curso curso) { this.curso = curso; }
    public Diplomado getDiplomado() { return diplomado; } public void setDiplomado(Diplomado diplomado) { this.diplomado = diplomado; }
    public Espacio getEspacio() { return espacio; } public void setEspacio(Espacio espacio) { this.espacio = espacio; }
    public ServicioCatering getServicioCatering() { return servicioCatering; } public void setServicioCatering(ServicioCatering servicio) { this.servicioCatering = servicio; }
}
