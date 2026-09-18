package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    // Spring Boot inyecta el repositorio automáticamente aquí
    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public Curso crearCurso(CursoDTO dto) {
        Curso curso = new Curso();
        curso.setTitulo(dto.getTitulo());
        curso.setDescripcion(dto.getDescripcion());
        curso.setCupoMaximo(dto.getCupoMaximo());
        curso.setCosto(dto.getCosto());
        curso.setFechaInicio(dto.getFechaInicio());
        curso.setFechaFin(dto.getFechaFin());
        curso.setDuracionHoras(dto.getDuracionHoras());
        curso.setActivo(true); // Siempre activo al crearse por primera vez

        // Asignamos las llaves foráneas temporalmente solo por su ID
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(dto.getIdCategoria());
        curso.setCategoria(categoria);

        Modalidad modalidad = new Modalidad();
        modalidad.setIdModalidad(dto.getIdModalidad());
        curso.setModalidad(modalidad);

        // ¡Aquí ocurre la magia que hace el INSERT en MySQL!
        return cursoRepository.save(curso);
    }
}