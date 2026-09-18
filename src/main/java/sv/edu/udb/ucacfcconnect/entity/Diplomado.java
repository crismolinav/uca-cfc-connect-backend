package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diplomados")
public class Diplomado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_diplomado")
    private Long id;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(name = "duracion_horas", nullable = false) private Integer duracionHoras;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal costo;
    @Column(name = "fecha_inicio") private LocalDate fechaInicio;
    @Column(name = "fecha_fin") private LocalDate fechaFin;
    @Column(nullable = false) private boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false, foreignKey = @ForeignKey(name = "fk_diplomado_categoria"))
    private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modalidad", nullable = false, foreignKey = @ForeignKey(name = "fk_diplomado_modalidad"))
    private Modalidad modalidad;
    @OneToMany(mappedBy = "diplomado") private List<DiplomadoDocente> docentes = new ArrayList<>();
    @OneToMany(mappedBy = "diplomado") private List<Actividad> actividades = new ArrayList<>();
    @OneToMany(mappedBy = "diplomado") private List<DetalleCotizacion> detallesCotizacion = new ArrayList<>();
    @OneToMany(mappedBy = "diplomado") private List<Inscripcion> inscripciones = new ArrayList<>();

    public Diplomado() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getDuracionHoras() { return duracionHoras; } public void setDuracionHoras(Integer duracionHoras) { this.duracionHoras = duracionHoras; }
    public BigDecimal getCosto() { return costo; } public void setCosto(BigDecimal costo) { this.costo = costo; }
    public LocalDate getFechaInicio() { return fechaInicio; } public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; } public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public boolean isActivo() { return activo; } public void setActivo(boolean activo) { this.activo = activo; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Modalidad getModalidad() { return modalidad; } public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
    public List<DiplomadoDocente> getDocentes() { return docentes; } public void setDocentes(List<DiplomadoDocente> docentes) { this.docentes = docentes; }
    public List<Actividad> getActividades() { return actividades; } public void setActividades(List<Actividad> actividades) { this.actividades = actividades; }
    public List<DetalleCotizacion> getDetallesCotizacion() { return detallesCotizacion; } public void setDetallesCotizacion(List<DetalleCotizacion> detalles) { this.detallesCotizacion = detalles; }
    public List<Inscripcion> getInscripciones() { return inscripciones; } public void setInscripciones(List<Inscripcion> inscripciones) { this.inscripciones = inscripciones; }
}
