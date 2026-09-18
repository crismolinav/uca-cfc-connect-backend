package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente") private Long id;
    @Column(name = "dui_nit", length = 20) private String duiNit;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(length = 150) private String empresa;
    @Column(length = 120) private String correo;
    @Column(length = 20) private String telefono;
    @Column(length = 255) private String direccion;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true, foreignKey = @ForeignKey(name = "fk_cliente_usuario"))
    private Usuario usuario;
    @OneToMany(mappedBy = "cliente") private List<Alquiler> alquileres = new ArrayList<>();
    @OneToMany(mappedBy = "cliente") private List<Cotizacion> cotizaciones = new ArrayList<>();
    @OneToMany(mappedBy = "cliente") private List<Participante> participantes = new ArrayList<>();
    @OneToMany(mappedBy = "cliente") private List<Pago> pagos = new ArrayList<>();
    @OneToMany(mappedBy = "cliente") private List<SolicitudCatering> solicitudesCatering = new ArrayList<>();

    public Cliente() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getDuiNit() { return duiNit; } public void setDuiNit(String duiNit) { this.duiNit = duiNit; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmpresa() { return empresa; } public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getCorreo() { return correo; } public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; } public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; } public void setDireccion(String direccion) { this.direccion = direccion; }
    public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public List<Alquiler> getAlquileres() { return alquileres; } public void setAlquileres(List<Alquiler> alquileres) { this.alquileres = alquileres; }
    public List<Cotizacion> getCotizaciones() { return cotizaciones; } public void setCotizaciones(List<Cotizacion> cotizaciones) { this.cotizaciones = cotizaciones; }
    public List<Participante> getParticipantes() { return participantes; } public void setParticipantes(List<Participante> participantes) { this.participantes = participantes; }
    public List<Pago> getPagos() { return pagos; } public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
    public List<SolicitudCatering> getSolicitudesCatering() { return solicitudesCatering; } public void setSolicitudesCatering(List<SolicitudCatering> solicitudes) { this.solicitudesCatering = solicitudes; }
}
