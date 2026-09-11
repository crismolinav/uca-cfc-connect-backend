package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "diplomado_docente", uniqueConstraints = @UniqueConstraint(name = "uk_diplomado_docente", columnNames = {"id_diplomado", "id_docente"}))
public class DiplomadoDocente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_diplomado_docente") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_diplomado", nullable = false, foreignKey = @ForeignKey(name = "fk_diplomado_docente_diplomado"))
    private Diplomado diplomado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_docente", nullable = false, foreignKey = @ForeignKey(name = "fk_diplomado_docente_docente"))
    private Docente docente;

    public DiplomadoDocente() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Diplomado getDiplomado() { return diplomado; } public void setDiplomado(Diplomado diplomado) { this.diplomado = diplomado; }
    public Docente getDocente() { return docente; } public void setDocente(Docente docente) { this.docente = docente; }
}
