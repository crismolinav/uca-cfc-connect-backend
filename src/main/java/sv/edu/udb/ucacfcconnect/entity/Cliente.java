package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente") private Long id;
    @Column(name = "dui_nit", length = 20) private String duiNit;
    @Column(name = "nombre_original", length = 120) private String nombreOriginal;
    @Column(name = "primer_nombre", length = 60) private String primerNombre;
    @Column(name = "segundo_nombre", length = 60) private String segundoNombre;
    @Column(name = "primer_apellido", length = 60) private String primerApellido;
    @Column(name = "segundo_apellido", length = 60) private String segundoApellido;
    @Column(length = 24, unique = true) private String carnet;
    @Column(name = "fecha_registro", nullable = false, updatable = false) private LocalDate fechaRegistro;
    @Formula("coalesce(nullif(trim(concat_ws(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido)), ''), nombre_original)")
    private String nombre;
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
    public String getNombre() {
        String completo = Stream.of(primerNombre, segundoNombre, primerApellido, segundoApellido)
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
        return completo.isBlank() ? nombreOriginal : completo;
    }
    // Las cuentas anteriores conservan el nombre hasta confirmar su identidad en recepción.
    public void setNombre(String nombre) { this.nombreOriginal = nombre; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String valor) { primerNombre = limpio(valor); }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String valor) { segundoNombre = limpio(valor); }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String valor) { primerApellido = limpio(valor); }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String valor) { segundoApellido = limpio(valor); }
    public String getCarnet() { return carnet; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }

    @PrePersist
    @PreUpdate
    private void prepararIdentidad() {
        if (fechaRegistro == null) fechaRegistro = LocalDate.now(ZoneId.of("America/El_Salvador"));
        if (carnet == null && primerNombre != null && primerApellido != null && segundoApellido != null) {
            carnet = inicial(primerApellido) + inicial(segundoApellido) + fechaRegistro.getYear()
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        }
    }
    private static String limpio(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private static String inicial(String apellido) {
        String letras = Normalizer.normalize(apellido, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (letras.isEmpty()) throw new IllegalArgumentException("El apellido debe contener letras");
        return letras.substring(0, 1);
    }
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
