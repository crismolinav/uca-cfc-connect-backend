package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "curso_docente", uniqueConstraints = @UniqueConstraint(name = "uk_curso_docente", columnNames = {"id_curso", "id_docente"}))
public class CursoDocente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso_docente") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_curso", nullable = false, foreignKey = @ForeignKey(name = "fk_curso_docente_curso"))
    private Curso curso;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_docente", nullable = false, foreignKey = @ForeignKey(name = "fk_curso_docente_docente"))
    private Docente docente;

    public CursoDocente() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Curso getCurso() { return curso; } public void setCurso(Curso curso) { this.curso = curso; }
    public Docente getDocente() { return docente; } public void setDocente(Docente docente) { this.docente = docente; }
}
