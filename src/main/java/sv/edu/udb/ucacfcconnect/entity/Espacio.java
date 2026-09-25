package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "espacios")
public class Espacio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_espacio") private Long id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(nullable = false, length = 80) private String tipo;
    @Column(nullable = false) private Integer capacidad;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal precio;
    @Column(nullable = false) private boolean disponible = true;
    @Column(length = 255) private String equipamiento;
    @Column(name = "duracion_maxima_horas", nullable = false, precision = 4, scale = 2)
    private BigDecimal duracionMaximaHoras;
    @OneToMany(mappedBy = "espacio") private List<Alquiler> alquileres = new ArrayList<>();
    @OneToMany(mappedBy = "espacio") private List<DetalleCotizacion> detallesCotizacion = new ArrayList<>();

    public Espacio() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; } public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getCapacidad() { return capacidad; } public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public BigDecimal getPrecio() { return precio; } public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public boolean isDisponible() { return disponible; } public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public String getEquipamiento() { return equipamiento; } public void setEquipamiento(String equipamiento) { this.equipamiento = equipamiento; }
    public BigDecimal getDuracionMaximaHoras() { return duracionMaximaHoras; }
    public void setDuracionMaximaHoras(BigDecimal duracionMaximaHoras) { this.duracionMaximaHoras = duracionMaximaHoras; }
    public List<Alquiler> getAlquileres() { return alquileres; } public void setAlquileres(List<Alquiler> alquileres) { this.alquileres = alquileres; }
    public List<DetalleCotizacion> getDetallesCotizacion() { return detallesCotizacion; } public void setDetallesCotizacion(List<DetalleCotizacion> detalles) { this.detallesCotizacion = detalles; }
}
