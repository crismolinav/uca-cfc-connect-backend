package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.AsignacionDocentesDTO;
import sv.edu.udb.ucacfcconnect.dto.DocenteResumenDTO;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.CursoDocente;
import sv.edu.udb.ucacfcconnect.entity.Diplomado;
import sv.edu.udb.ucacfcconnect.entity.DiplomadoDocente;
import sv.edu.udb.ucacfcconnect.entity.Docente;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;
import sv.edu.udb.ucacfcconnect.repository.CursoDocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.DiplomadoDocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.DiplomadoRepository;
import sv.edu.udb.ucacfcconnect.repository.DocenteRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocenteAsignacionService {

    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final CursoDocenteRepository cursoDocenteRepository;
    private final DiplomadoDocenteRepository diplomadoDocenteRepository;
    private final DocenteDisponibilidadService disponibilidadService;

    public DocenteAsignacionService(
            DocenteRepository docenteRepository,
            CursoRepository cursoRepository,
            DiplomadoRepository diplomadoRepository,
            CursoDocenteRepository cursoDocenteRepository,
            DiplomadoDocenteRepository diplomadoDocenteRepository,
            DocenteDisponibilidadService disponibilidadService
    ) {
        this.docenteRepository = docenteRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
        this.cursoDocenteRepository = cursoDocenteRepository;
        this.diplomadoDocenteRepository = diplomadoDocenteRepository;
        this.disponibilidadService = disponibilidadService;
    }

    @Transactional(readOnly = true)
    public List<DocenteResumenDTO> listarCurso(Long idCurso) {
        buscarCurso(idCurso);
        return cursoDocenteRepository.findByCurso_IdCursoOrderByDocente_NombreAsc(idCurso).stream()
                .map(CursoDocente::getDocente)
                .map(this::aResumen)
                .toList();
    }

    @Transactional
    public List<DocenteResumenDTO> asignarCurso(Long idCurso, AsignacionDocentesDTO solicitud) {
        Curso curso = buscarCurso(idCurso);
        List<Docente> docentes = resolverDocentes(solicitud);
        disponibilidadService.validarCurso(curso, docentes);

        Map<Long, CursoDocente> actuales = cursoDocenteRepository
                .findByCurso_IdCursoOrderByDocente_NombreAsc(idCurso)
                .stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getDocente().getId(), Function.identity()));
        Set<Long> seleccionados = docentes.stream().map(Docente::getId).collect(Collectors.toSet());
        actuales.keySet().stream()
                .filter(id -> !seleccionados.contains(id))
                .forEach(id -> cursoDocenteRepository.deleteByCurso_IdCursoAndDocente_Id(idCurso, id));

        List<CursoDocente> nuevas = docentes.stream()
                .filter(docente -> !actuales.containsKey(docente.getId()))
                .map(docente -> {
                    CursoDocente asignacion = new CursoDocente();
                    asignacion.setCurso(curso);
                    asignacion.setDocente(docente);
                    return asignacion;
                })
                .toList();
        cursoDocenteRepository.saveAll(nuevas);
        cursoDocenteRepository.flush();
        return ordenar(docentes);
    }

    @Transactional(readOnly = true)
    public List<DocenteResumenDTO> listarDiplomado(Long idDiplomado) {
        buscarDiplomado(idDiplomado);
        return diplomadoDocenteRepository.findByDiplomado_IdOrderByDocente_NombreAsc(idDiplomado).stream()
                .map(DiplomadoDocente::getDocente)
                .map(this::aResumen)
                .toList();
    }

    @Transactional
    public List<DocenteResumenDTO> asignarDiplomado(Long idDiplomado, AsignacionDocentesDTO solicitud) {
        Diplomado diplomado = buscarDiplomado(idDiplomado);
        List<Docente> docentes = resolverDocentes(solicitud);
        disponibilidadService.validarDiplomado(diplomado, docentes);

        Map<Long, DiplomadoDocente> actuales = diplomadoDocenteRepository
                .findByDiplomado_IdOrderByDocente_NombreAsc(idDiplomado)
                .stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getDocente().getId(), Function.identity()));
        Set<Long> seleccionados = docentes.stream().map(Docente::getId).collect(Collectors.toSet());
        actuales.keySet().stream()
                .filter(id -> !seleccionados.contains(id))
                .forEach(id -> diplomadoDocenteRepository.deleteByDiplomado_IdAndDocente_Id(idDiplomado, id));

        List<DiplomadoDocente> nuevas = docentes.stream()
                .filter(docente -> !actuales.containsKey(docente.getId()))
                .map(docente -> {
                    DiplomadoDocente asignacion = new DiplomadoDocente();
                    asignacion.setDiplomado(diplomado);
                    asignacion.setDocente(docente);
                    return asignacion;
                })
                .toList();
        diplomadoDocenteRepository.saveAll(nuevas);
        diplomadoDocenteRepository.flush();
        return ordenar(docentes);
    }

    private List<Docente> resolverDocentes(AsignacionDocentesDTO solicitud) {
        List<Long> ids = solicitud.idsDocentes();
        Set<Long> unicos = new LinkedHashSet<>(ids);
        if (unicos.size() != ids.size()) {
            throw new SolicitudInvalidaException("La lista contiene docentes repetidos");
        }
        List<Docente> docentes = docenteRepository.findAllById(unicos);
        if (docentes.size() != unicos.size()) {
            Set<Long> encontrados = docentes.stream().map(Docente::getId).collect(Collectors.toSet());
            Long faltante = unicos.stream().filter(id -> !encontrados.contains(id)).findFirst().orElseThrow();
            throw new RecursoNoEncontradoException("Docente", faltante);
        }
        return new ArrayList<>(docentes);
    }

    private List<DocenteResumenDTO> ordenar(List<Docente> docentes) {
        return docentes.stream()
                .sorted(Comparator.comparing(Docente::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aResumen)
                .toList();
    }

    private Curso buscarCurso(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso", id));
    }

    private Diplomado buscarDiplomado(Long id) {
        return diplomadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Diplomado", id));
    }

    private DocenteResumenDTO aResumen(Docente docente) {
        return new DocenteResumenDTO(docente.getId(), docente.getNombre(), docente.getEspecialidad());
    }
}
