package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categorias")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(length = 255)
    private String descripcion;
    @OneToMany(mappedBy = "categoria")
    private List<Curso> cursos = new ArrayList<>();
    @OneToMany(mappedBy = "categoria")
    private List<Diplomado> diplomados = new ArrayList<>();

    public Categoria() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public List<Curso> getCursos() { return cursos; }
    public void setCursos(List<Curso> cursos) { this.cursos = cursos; }
    public List<Diplomado> getDiplomados() { return diplomados; }
    public void setDiplomados(List<Diplomado> diplomados) { this.diplomados = diplomados; }
}
