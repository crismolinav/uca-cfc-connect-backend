package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscripciones")
public class Inscripcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion") private Long id;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, length = 30) private String estado;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal total;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participante", nullable = false, foreignKey = @ForeignKey(name = "fk_inscripcion_participante"))
    private Participante participante;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", foreignKey = @ForeignKey(name = "fk_inscripcion_curso"))
    private Curso curso;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_diplomado", foreignKey = @ForeignKey(name = "fk_inscripcion_diplomado"))
    private Diplomado diplomado;
    @OneToMany(mappedBy = "inscripcion") private List<Pago> pagos = new ArrayList<>();

    public Inscripcion() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; } public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; } public void setTotal(BigDecimal total) { this.total = total; }
    public Participante getParticipante() { return participante; } public void setParticipante(Participante participante) { this.participante = participante; }
    public Curso getCurso() { return curso; } public void setCurso(Curso curso) { this.curso = curso; }
    public Diplomado getDiplomado() { return diplomado; } public void setDiplomado(Diplomado diplomado) { this.diplomado = diplomado; }
    public List<Pago> getPagos() { return pagos; } public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}
