package sv.edu.udb.ucacfcconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.DocenteDTO;
import sv.edu.udb.ucacfcconnect.dto.DocenteResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.PaginaDTO;
import sv.edu.udb.ucacfcconnect.entity.Docente;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;
import sv.edu.udb.ucacfcconnect.repository.CursoDocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.DiplomadoDocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.DocenteRepository;

import java.util.Locale;
import java.util.Map;

@Service
public class DocenteService {

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "idDocente", "id",
            "nombre", "nombre",
            "especialidad", "especialidad",
            "correo", "correo"
    );

    private final DocenteRepository docenteRepository;
    private final CursoDocenteRepository cursoDocenteRepository;
    private final DiplomadoDocenteRepository diplomadoDocenteRepository;

    public DocenteService(
            DocenteRepository docenteRepository,
            CursoDocenteRepository cursoDocenteRepository,
            DiplomadoDocenteRepository diplomadoDocenteRepository
    ) {
        this.docenteRepository = docenteRepository;
        this.cursoDocenteRepository = cursoDocenteRepository;
        this.diplomadoDocenteRepository = diplomadoDocenteRepository;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<DocenteResponseDTO> listar(
            String texto,
            int pagina,
            int tamano,
            String ordenarPor,
            String direccion
    ) {
        String propiedad = CAMPOS_ORDENAMIENTO.get(ordenarPor);
        if (propiedad == null) {
            throw new SolicitudInvalidaException(
                    "Campo de ordenamiento no permitido. Use: " + String.join(", ", CAMPOS_ORDENAMIENTO.keySet())
            );
        }
        Sort.Direction sentido;
        try {
            sentido = Sort.Direction.fromString(direccion.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("La dirección debe ser 'asc' o 'desc'");
        }

        String filtro = texto == null || texto.isBlank() ? null : texto.strip();
        Page<DocenteResponseDTO> resultado = docenteRepository.buscar(
                filtro,
                PageRequest.of(pagina, tamano, Sort.by(sentido, propiedad))
        ).map(this::aRespuesta);
        return PaginaDTO.desde(resultado);
    }

    @Transactional(readOnly = true)
    public DocenteResponseDTO obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public DocenteResponseDTO crear(DocenteDTO dto) {
        String correo = dto.correo().strip().toLowerCase(Locale.ROOT);
        if (docenteRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un docente registrado con ese correo");
        }
        Docente docente = new Docente();
        asignarDatos(docente, dto, correo);
        return aRespuesta(docenteRepository.save(docente));
    }

    @Transactional
    public DocenteResponseDTO actualizar(Long id, DocenteDTO dto) {
        Docente docente = buscar(id);
        String correo = dto.correo().strip().toLowerCase(Locale.ROOT);
        if (docenteRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw new ConflictException("Ya existe otro docente registrado con ese correo");
        }
        asignarDatos(docente, dto, correo);
        return aRespuesta(docenteRepository.save(docente));
    }

    @Transactional
    public void eliminar(Long id) {
        Docente docente = buscar(id);
        if (cursoDocenteRepository.existsByDocente_Id(id)
                || diplomadoDocenteRepository.existsByDocente_Id(id)) {
            throw new ConflictException(
                    "No se puede eliminar el docente porque está asignado a cursos o diplomados"
            );
        }
        try {
            docenteRepository.delete(docente);
            docenteRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se puede eliminar el docente porque posee registros relacionados");
        }
    }

    private Docente buscar(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Docente", id));
    }

    private void asignarDatos(Docente docente, DocenteDTO dto, String correo) {
        docente.setNombre(dto.nombre().strip());
        docente.setEspecialidad(dto.especialidad().strip());
        docente.setCorreo(correo);
        docente.setTelefono(dto.telefono() == null || dto.telefono().isBlank() ? null : dto.telefono().strip());
    }

    private DocenteResponseDTO aRespuesta(Docente docente) {
        return new DocenteResponseDTO(
                docente.getId(),
                docente.getNombre(),
                docente.getEspecialidad(),
                docente.getCorreo(),
                docente.getTelefono()
        );
    }
}
