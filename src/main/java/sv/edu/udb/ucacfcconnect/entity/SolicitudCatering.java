package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes_catering")
public class SolicitudCatering {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud_catering") private Long id;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false) private LocalTime hora;
    @Column(nullable = false, length = 150) private String lugar;
    @Column(name = "numero_asistentes", nullable = false) private Integer numeroAsistentes;
    @Column(length = 255) private String menu;
    @Column(nullable = false, length = 30) private String estado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_solicitud_cliente"))
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio_catering", nullable = false, foreignKey = @ForeignKey(name = "fk_solicitud_servicio"))
    private ServicioCatering servicioCatering;
    @OneToMany(mappedBy = "solicitudCatering") private List<Actividad> actividades = new ArrayList<>();
    @OneToMany(mappedBy = "solicitudCatering") private List<Pago> pagos = new ArrayList<>();

    public SolicitudCatering() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; } public void setHora(LocalTime hora) { this.hora = hora; }
    public String getLugar() { return lugar; } public void setLugar(String lugar) { this.lugar = lugar; }
    public Integer getNumeroAsistentes() { return numeroAsistentes; } public void setNumeroAsistentes(Integer numeroAsistentes) { this.numeroAsistentes = numeroAsistentes; }
    public String getMenu() { return menu; } public void setMenu(String menu) { this.menu = menu; }
    public String getEstado() { return estado; } public void setEstado(String estado) { this.estado = estado; }
    public Cliente getCliente() { return cliente; } public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public ServicioCatering getServicioCatering() { return servicioCatering; } public void setServicioCatering(ServicioCatering servicio) { this.servicioCatering = servicio; }
    public List<Actividad> getActividades() { return actividades; } public void setActividades(List<Actividad> actividades) { this.actividades = actividades; }
    public List<Pago> getPagos() { return pagos; } public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}
