package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.ResenaCursoRequest;
import sv.edu.udb.ucacfcconnect.dto.ResenaResumenDTO;
import sv.edu.udb.ucacfcconnect.entity.ResenaCurso;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.DocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.InscripcionRepository;
import sv.edu.udb.ucacfcconnect.repository.ResenaCursoRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ResenaCursoService {
    private final ResenaCursoRepository resenas;
    private final InscripcionRepository inscripciones;
    private final CursoRepository cursos;
    private final DocenteRepository docentes;

    public ResenaCursoService(ResenaCursoRepository resenas, InscripcionRepository inscripciones,
                              CursoRepository cursos, DocenteRepository docentes) {
        this.resenas = resenas;
        this.inscripciones = inscripciones;
        this.cursos = cursos;
        this.docentes = docentes;
    }

    @Transactional(readOnly = true)
    public List<ResenaResumenDTO> listar(Long idCurso) {
        if (!cursos.existsById(idCurso)) throw new RecursoNoEncontradoException("Curso", idCurso);
        return resenas.findByInscripcion_Curso_IdCursoOrderByFechaCreacionDesc(idCurso).stream()
                .map(r -> new ResenaResumenDTO(
                        r.getInscripcion().getParticipante().getNombre() + " " + r.getInscripcion().getParticipante().getApellido(),
                        r.getInscripcion().getCurso().getTitulo(), r.getCalificacionCurso(), r.getComentarioCurso(),
                        r.getFechaCreacion().toLocalDate()))
                .toList();
    }

    @Transactional
    public ResenaResumenDTO crear(Long idCurso, Long usuarioId, ResenaCursoRequest solicitud) {
        var inscripcion = inscripciones.findByIdAndParticipante_Cliente_Usuario_IdAndCurso_IdCurso(
                        solicitud.idInscripcion(), usuarioId, idCurso)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción del curso", solicitud.idInscripcion()));
        if (!"CONFIRMADA".equalsIgnoreCase(inscripcion.getEstado())
                || inscripcion.getCurso().getFechaFin().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("Podrás valorar el curso cuando haya finalizado y tu inscripción esté confirmada");
        }
        if (resenas.existeResenaUsuarioCurso(usuarioId, idCurso)
                || resenas.existsByInscripcion_Id(inscripcion.getId()))
            throw new ConflictException("Ya enviaste una reseña para este curso");
        ResenaCurso resena = new ResenaCurso();
        resena.setInscripcion(inscripcion);
        resena.setCalificacionCurso(solicitud.calificacionCurso());
        resena.setComentarioCurso(solicitud.comentarioCurso().strip());
        if (solicitud.idDocente() != null) {
            boolean asignado = inscripcion.getCurso().getDocentes().stream()
                    .anyMatch(a -> a.getDocente().getId().equals(solicitud.idDocente()));
            if (!asignado) throw new ReglaNegocioException("El docente seleccionado no está asignado a este curso");
            if (solicitud.calificacionDocente() == null || solicitud.comentarioDocente() == null
                    || solicitud.comentarioDocente().isBlank())
                throw new ReglaNegocioException("Completa la calificación y el comentario del docente");
            resena.setDocente(docentes.findById(solicitud.idDocente()).orElseThrow());
            resena.setCalificacionDocente(solicitud.calificacionDocente());
            resena.setComentarioDocente(solicitud.comentarioDocente().strip());
        }
        resena = resenas.save(resena);
        var participante = inscripcion.getParticipante();
        return new ResenaResumenDTO(participante.getNombre() + " " + participante.getApellido(),
                inscripcion.getCurso().getTitulo(), resena.getCalificacionCurso(), resena.getComentarioCurso(),
                resena.getFechaCreacion().toLocalDate());
    }
}
