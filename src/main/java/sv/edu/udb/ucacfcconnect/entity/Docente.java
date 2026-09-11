package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "docentes")
public class Docente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente")
    private Long id;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(length = 120)
    private String especialidad;
    @Column(length = 120)
    private String correo;
    @Column(length = 20)
    private String telefono;
    @OneToMany(mappedBy = "docente")
    private List<CursoDocente> cursos = new ArrayList<>();
    @OneToMany(mappedBy = "docente")
    private List<DiplomadoDocente> diplomados = new ArrayList<>();

    public Docente() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public List<CursoDocente> getCursos() { return cursos; }
    public void setCursos(List<CursoDocente> cursos) { this.cursos = cursos; }
    public List<DiplomadoDocente> getDiplomados() { return diplomados; }
    public void setDiplomados(List<DiplomadoDocente> diplomados) { this.diplomados = diplomados; }
}
