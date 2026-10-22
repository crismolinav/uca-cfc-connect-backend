package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "resenas_curso", uniqueConstraints =
        @UniqueConstraint(name = "uq_resena_inscripcion", columnNames = "id_inscripcion"))
public class ResenaCurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false,
            foreignKey = @ForeignKey(name = "fk_resena_inscripcion"))
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", foreignKey = @ForeignKey(name = "fk_resena_docente"))
    private Docente docente;

    @Column(name = "calificacion_curso", nullable = false)
    private Integer calificacionCurso;

    @Column(name = "comentario_curso", nullable = false, columnDefinition = "TEXT")
    private String comentarioCurso;

    @Column(name = "calificacion_docente")
    private Integer calificacionDocente;

    @Column(name = "comentario_docente", columnDefinition = "TEXT")
    private String comentarioDocente;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void antesDeCrear() { fechaCreacion = LocalDateTime.now(ZoneId.of("America/El_Salvador")); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Inscripcion getInscripcion() { return inscripcion; }
    public void setInscripcion(Inscripcion inscripcion) { this.inscripcion = inscripcion; }
    public Docente getDocente() { return docente; }
    public void setDocente(Docente docente) { this.docente = docente; }
    public Integer getCalificacionCurso() { return calificacionCurso; }
    public void setCalificacionCurso(Integer calificacionCurso) { this.calificacionCurso = calificacionCurso; }
    public String getComentarioCurso() { return comentarioCurso; }
    public void setComentarioCurso(String comentarioCurso) { this.comentarioCurso = comentarioCurso; }
    public Integer getCalificacionDocente() { return calificacionDocente; }
    public void setCalificacionDocente(Integer calificacionDocente) { this.calificacionDocente = calificacionDocente; }
    public String getComentarioDocente() { return comentarioDocente; }
    public void setComentarioDocente(String comentarioDocente) { this.comentarioDocente = comentarioDocente; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
