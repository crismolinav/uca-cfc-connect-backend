package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Actividad;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.CursoDocente;
import sv.edu.udb.ucacfcconnect.entity.Diplomado;
import sv.edu.udb.ucacfcconnect.entity.DiplomadoDocente;
import sv.edu.udb.ucacfcconnect.entity.Docente;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.repository.ActividadRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoDocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.DiplomadoDocenteRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocenteDisponibilidadService {

    private static final Map<String, DayOfWeek> DIAS = Map.of(
            "Lunes", DayOfWeek.MONDAY,
            "Martes", DayOfWeek.TUESDAY,
            "Miércoles", DayOfWeek.WEDNESDAY,
            "Jueves", DayOfWeek.THURSDAY,
            "Viernes", DayOfWeek.FRIDAY,
            "Sábado", DayOfWeek.SATURDAY,
            "Domingo", DayOfWeek.SUNDAY
    );

    private final CursoDocenteRepository cursoDocenteRepository;
    private final DiplomadoDocenteRepository diplomadoDocenteRepository;
    private final ActividadRepository actividadRepository;

    public DocenteDisponibilidadService(
            CursoDocenteRepository cursoDocenteRepository,
            DiplomadoDocenteRepository diplomadoDocenteRepository,
            ActividadRepository actividadRepository
    ) {
        this.cursoDocenteRepository = cursoDocenteRepository;
        this.diplomadoDocenteRepository = diplomadoDocenteRepository;
        this.actividadRepository = actividadRepository;
    }

    @Transactional(readOnly = true)
    public void validarCurso(Curso curso, Collection<Docente> docentes) {
        HorarioCurso horario = leerHorario(curso);
        for (Docente docente : docentes) {
            for (CursoDocente asignacion : cursoDocenteRepository.findByDocente_Id(docente.getId())) {
                Curso otro = asignacion.getCurso();
                if (!otro.getIdCurso().equals(curso.getIdCurso()) && seCruzan(curso, horario, otro)) {
                    conflicto(docente, "el curso «" + otro.getTitulo() + "»");
                }
            }
            for (DiplomadoDocente asignacion : diplomadoDocenteRepository.findByDocente_Id(docente.getId())) {
                for (Actividad actividad : actividadRepository
                        .findByDiplomado_IdOrderByFechaAscHoraInicioAsc(asignacion.getDiplomado().getId())) {
                    if (seCruzan(curso, horario, actividad)) {
                        conflicto(docente, "la sesión «" + actividad.getTitulo() + "»");
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public void validarCursoConDocentesAsignados(Curso curso) {
        List<Docente> docentes = cursoDocenteRepository
                .findByCurso_IdCursoOrderByDocente_NombreAsc(curso.getIdCurso())
                .stream()
                .map(CursoDocente::getDocente)
                .toList();
        validarCurso(curso, docentes);
    }

    @Transactional(readOnly = true)
    public void validarDiplomado(Diplomado diplomado, Collection<Docente> docentes) {
        List<Actividad> sesiones = actividadRepository
                .findByDiplomado_IdOrderByFechaAscHoraInicioAsc(diplomado.getId());
        for (Docente docente : docentes) {
            for (Actividad sesion : sesiones) {
                validarActividadConDocente(diplomado, sesion.getFecha(), sesion.getHoraInicio(), sesion.getHoraFin(), docente);
            }
        }
    }

    @Transactional(readOnly = true)
    public void validarActividadDiplomado(
            Diplomado diplomado,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        List<Docente> docentes = diplomadoDocenteRepository
                .findByDiplomado_IdOrderByDocente_NombreAsc(diplomado.getId())
                .stream()
                .map(DiplomadoDocente::getDocente)
                .toList();
        for (Docente docente : docentes) {
            validarActividadConDocente(diplomado, fecha, horaInicio, horaFin, docente);
        }
    }

    private void validarActividadConDocente(
            Diplomado diplomado,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            Docente docente
    ) {
        for (CursoDocente asignacion : cursoDocenteRepository.findByDocente_Id(docente.getId())) {
            Curso curso = asignacion.getCurso();
            if (seCruzan(curso, leerHorario(curso), fecha, horaInicio, horaFin)) {
                conflicto(docente, "el curso «" + curso.getTitulo() + "»");
            }
        }
        for (DiplomadoDocente asignacion : diplomadoDocenteRepository.findByDocente_Id(docente.getId())) {
            Diplomado otro = asignacion.getDiplomado();
            if (otro.getId().equals(diplomado.getId())) {
                continue;
            }
            for (Actividad actividad : actividadRepository.findByDiplomado_IdOrderByFechaAscHoraInicioAsc(otro.getId())) {
                if (actividad.getFecha().equals(fecha)
                        && horasSeCruzan(horaInicio, horaFin, actividad.getHoraInicio(), actividad.getHoraFin())) {
                    conflicto(docente, "la sesión «" + actividad.getTitulo() + "»");
                }
            }
        }
    }

    private boolean seCruzan(Curso primero, HorarioCurso horarioPrimero, Curso segundo) {
        HorarioCurso horarioSegundo = leerHorario(segundo);
        LocalDate inicio = max(primero.getFechaInicio(), segundo.getFechaInicio());
        LocalDate fin = min(primero.getFechaFin(), segundo.getFechaFin());
        if (inicio.isAfter(fin)
                || !horasSeCruzan(horarioPrimero.inicio(), horarioPrimero.fin(), horarioSegundo.inicio(), horarioSegundo.fin())) {
            return false;
        }
        Set<DayOfWeek> diasComunes = horarioPrimero.dias().stream()
                .filter(horarioSegundo.dias()::contains)
                .collect(Collectors.toSet());
        return existeDia(inicio, fin, diasComunes);
    }

    private boolean seCruzan(Curso curso, HorarioCurso horario, Actividad actividad) {
        return seCruzan(curso, horario, actividad.getFecha(), actividad.getHoraInicio(), actividad.getHoraFin());
    }

    private boolean seCruzan(
            Curso curso,
            HorarioCurso horario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        return !fecha.isBefore(curso.getFechaInicio())
                && !fecha.isAfter(curso.getFechaFin())
                && horario.dias().contains(fecha.getDayOfWeek())
                && horasSeCruzan(horario.inicio(), horario.fin(), horaInicio, horaFin);
    }

    private HorarioCurso leerHorario(Curso curso) {
        try {
            String[] partes = curso.getHorario().split("\\s*\\|\\s*");
            String[] rango = partes[1].split("\\s*-\\s*");
            Set<DayOfWeek> dias = List.of(partes[0].split("\\s*,\\s*")).stream()
                    .map(DIAS::get)
                    .collect(Collectors.toSet());
            if (dias.contains(null)) {
                throw new IllegalArgumentException();
            }
            return new HorarioCurso(dias, LocalTime.parse(rango[0]), LocalTime.parse(rango[1]));
        } catch (RuntimeException ex) {
            throw new ReglaNegocioException(
                    "El curso «" + curso.getTitulo() + "» tiene un horario que no puede validarse"
            );
        }
    }

    private boolean existeDia(LocalDate inicio, LocalDate fin, Set<DayOfWeek> dias) {
        if (dias.isEmpty()) {
            return false;
        }
        return inicio.datesUntil(fin.plusDays(1)).anyMatch(fecha -> dias.contains(fecha.getDayOfWeek()));
    }

    private boolean horasSeCruzan(LocalTime inicioA, LocalTime finA, LocalTime inicioB, LocalTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    private LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private void conflicto(Docente docente, String actividad) {
        throw new ConflictException(
                "El docente «" + docente.getNombre() + "» ya tiene asignado " + actividad + " en ese horario"
        );
    }

    private record HorarioCurso(Set<DayOfWeek> dias, LocalTime inicio, LocalTime fin) {
    }
}
