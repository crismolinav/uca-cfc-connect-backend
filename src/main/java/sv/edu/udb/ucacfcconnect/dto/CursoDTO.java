package sv.edu.udb.ucacfcconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "CursoRequest", description = "Datos requeridos para crear o actualizar un curso")
public class CursoDTO {

    @NotBlank(message = "El título del curso es obligatorio")
    @Size(min = 3, max = 150, message = "El título debe tener entre 3 y 150 caracteres")
    @Schema(example = "Excel avanzado para negocios")
    private String titulo;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(min = 10, max = 5000, message = "La descripción debe tener entre 10 y 5000 caracteres")
    @Schema(example = "Curso práctico para el análisis y visualización de datos empresariales")
    private String descripcion;

    @NotNull(message = "Debe definir un cupo máximo")
    @Positive(message = "El cupo máximo debe ser mayor que cero")
    @Schema(example = "25")
    private Integer cupoMaximo;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El costo debe ser mayor a 0")
    @Schema(example = "125.00")
    private BigDecimal costo;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Schema(example = "2026-10-05")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Schema(example = "2026-11-05")
    private LocalDate fechaFin;

    @NotNull(message = "La duración en horas es obligatoria")
    @Positive(message = "La duración debe ser mayor que cero")
    @Schema(example = "32")
    private Integer duracionHoras;

    @NotBlank(message = "El horario es obligatorio")
    @Size(max = 100, message = "El horario no puede exceder 100 caracteres")
    @Schema(example = "Lunes y miércoles, 18:00-20:00")
    private String horario;

    @NotNull(message = "Debe seleccionar una categoría")
    @Positive(message = "El identificador de categoría debe ser mayor que cero")
    @Schema(example = "1")
    private Long idCategoria;

    @NotNull(message = "Debe seleccionar una modalidad")
    @Positive(message = "El identificador de modalidad debe ser mayor que cero")
    @Schema(example = "1")
    private Long idModalidad;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getDuracionHoras() {
        return duracionHoras;
    }

    public void setDuracionHoras(Integer duracionHoras) {
        this.duracionHoras = duracionHoras;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public Long getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Long getIdModalidad() {
        return idModalidad;
    }

    public void setIdModalidad(Long idModalidad) {
        this.idModalidad = idModalidad;
    }
}
