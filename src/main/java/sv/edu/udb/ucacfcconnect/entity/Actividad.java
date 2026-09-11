package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "actividades")
public class Actividad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad") private Long id;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(nullable = false, length = 30) private String tipo;
    @Column(nullable = false) private LocalDate fecha;
    @Column(name = "hora_inicio", nullable = false) private LocalTime horaInicio;
    @Column(name = "hora_fin", nullable = false) private LocalTime horaFin;
    private Integer cupo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", foreignKey = @ForeignKey(name = "fk_actividad_curso"))
    private Curso curso;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_diplomado", foreignKey = @ForeignKey(name = "fk_actividad_diplomado"))
    private Diplomado diplomado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alquiler", foreignKey = @ForeignKey(name = "fk_actividad_alquiler"))
    private Alquiler alquiler;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_catering", foreignKey = @ForeignKey(name = "fk_actividad_catering"))
    private SolicitudCatering solicitudCatering;

    public Actividad() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; } public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTipo() { return tipo; } public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; } public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; } public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Integer getCupo() { return cupo; } public void setCupo(Integer cupo) { this.cupo = cupo; }
    public Curso getCurso() { return curso; } public void setCurso(Curso curso) { this.curso = curso; }
    public Diplomado getDiplomado() { return diplomado; } public void setDiplomado(Diplomado diplomado) { this.diplomado = diplomado; }
    public Alquiler getAlquiler() { return alquiler; } public void setAlquiler(Alquiler alquiler) { this.alquiler = alquiler; }
    public SolicitudCatering getSolicitudCatering() { return solicitudCatering; } public void setSolicitudCatering(SolicitudCatering solicitud) { this.solicitudCatering = solicitud; }
}
