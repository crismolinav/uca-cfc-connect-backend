package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participantes")
public class Participante {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participante") private Long id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(nullable = false, length = 100) private String apellido;
    @Column(length = 120) private String correo;
    @Column(length = 20) private String telefono;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_participante_cliente"))
    private Cliente cliente;
    @OneToMany(mappedBy = "participante") private List<Inscripcion> inscripciones = new ArrayList<>();

    public Participante() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; } public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCorreo() { return correo; } public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; } public void setTelefono(String telefono) { this.telefono = telefono; }
    public Cliente getCliente() { return cliente; } public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public List<Inscripcion> getInscripciones() { return inscripciones; } public void setInscripciones(List<Inscripcion> inscripciones) { this.inscripciones = inscripciones; }
}
