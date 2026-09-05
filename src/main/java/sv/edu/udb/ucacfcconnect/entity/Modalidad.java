package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "modalidades")
public class Modalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modalidad")
    private Long idModalidad;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 150)
    private String descripcion;

    public Long getIdModalidad() {
        return idModalidad;
    }

    public void setIdModalidad(Long idModalidad) {
        this.idModalidad = idModalidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}