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
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true,
            foreignKey = @ForeignKey(name = "fk_docente_usuario"))
    private Usuario usuario;
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
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public List<CursoDocente> getCursos() { return cursos; }
    public void setCursos(List<CursoDocente> cursos) { this.cursos = cursos; }
    public List<DiplomadoDocente> getDiplomados() { return diplomados; }
    public void setDiplomados(List<DiplomadoDocente> diplomados) { this.diplomados = diplomados; }
}
