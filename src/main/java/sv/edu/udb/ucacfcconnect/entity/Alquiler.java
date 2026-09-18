package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alquileres")
public class Alquiler {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alquiler") private Long id;
    @Column(nullable = false) private LocalDate fecha;
    @Column(name = "hora_inicio", nullable = false) private LocalTime horaInicio;
    @Column(name = "hora_fin", nullable = false) private LocalTime horaFin;
    @Column(length = 255) private String motivo;
    @Column(nullable = false, length = 30) private String estado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_alquiler_cliente"))
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_espacio", nullable = false, foreignKey = @ForeignKey(name = "fk_alquiler_espacio"))
    private Espacio espacio;
    @OneToMany(mappedBy = "alquiler") private List<Actividad> actividades = new ArrayList<>();
    @OneToMany(mappedBy = "alquiler") private List<Pago> pagos = new ArrayList<>();

    public Alquiler() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; } public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; } public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getMotivo() { return motivo; } public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; } public void setEstado(String estado) { this.estado = estado; }
    public Cliente getCliente() { return cliente; } public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Espacio getEspacio() { return espacio; } public void setEspacio(Espacio espacio) { this.espacio = espacio; }
    public List<Actividad> getActividades() { return actividades; } public void setActividades(List<Actividad> actividades) { this.actividades = actividades; }
    public List<Pago> getPagos() { return pagos; } public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}
