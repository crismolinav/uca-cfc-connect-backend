package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago") private Long id;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal monto;
    @Column(nullable = false, length = 30) private String metodo;
    @Column(nullable = false, length = 30) private String estado;
    @Column(length = 100) private String referencia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_cliente")) private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscripcion", foreignKey = @ForeignKey(name = "fk_pago_inscripcion")) private Inscripcion inscripcion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cotizacion", foreignKey = @ForeignKey(name = "fk_pago_cotizacion")) private Cotizacion cotizacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alquiler", foreignKey = @ForeignKey(name = "fk_pago_alquiler")) private Alquiler alquiler;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_catering", foreignKey = @ForeignKey(name = "fk_pago_catering")) private SolicitudCatering solicitudCatering;

    public Pago() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getMonto() { return monto; } public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMetodo() { return metodo; } public void setMetodo(String metodo) { this.metodo = metodo; }
    public String getEstado() { return estado; } public void setEstado(String estado) { this.estado = estado; }
    public String getReferencia() { return referencia; } public void setReferencia(String referencia) { this.referencia = referencia; }
    public Cliente getCliente() { return cliente; } public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Inscripcion getInscripcion() { return inscripcion; } public void setInscripcion(Inscripcion inscripcion) { this.inscripcion = inscripcion; }
    public Cotizacion getCotizacion() { return cotizacion; } public void setCotizacion(Cotizacion cotizacion) { this.cotizacion = cotizacion; }
    public Alquiler getAlquiler() { return alquiler; } public void setAlquiler(Alquiler alquiler) { this.alquiler = alquiler; }
    public SolicitudCatering getSolicitudCatering() { return solicitudCatering; } public void setSolicitudCatering(SolicitudCatering solicitud) { this.solicitudCatering = solicitud; }
}
