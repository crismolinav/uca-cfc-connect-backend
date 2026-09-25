package sv.edu.udb.ucacfcconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.CatalogoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoDTO;
import sv.edu.udb.ucacfcconnect.dto.CursoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.PaginaDTO;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Curso;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.CursoRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CursoService {

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.ofEntries(
            Map.entry("idCurso", "idCurso"),
            Map.entry("titulo", "titulo"),
            Map.entry("duracionHoras", "duracionHoras"),
            Map.entry("cupoMaximo", "cupoMaximo"),
            Map.entry("costo", "costo"),
            Map.entry("fechaInicio", "fechaInicio"),
            Map.entry("fechaFin", "fechaFin"),
            Map.entry("horario", "horario"),
            Map.entry("activo", "activo"),
            Map.entry("categoria", "categoria.nombre"),
            Map.entry("modalidad", "modalidad.nombre")
    );

    private static final Map<String, DayOfWeek> DIAS_SEMANA = Map.of(
            "Lunes", DayOfWeek.MONDAY,
            "Martes", DayOfWeek.TUESDAY,
            "Miércoles", DayOfWeek.WEDNESDAY,
            "Jueves", DayOfWeek.THURSDAY,
            "Viernes", DayOfWeek.FRIDAY,
            "Sábado", DayOfWeek.SATURDAY,
            "Domingo", DayOfWeek.SUNDAY
    );

    private final CursoRepository cursoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ModalidadRepository modalidadRepository;

    public CursoService(
            CursoRepository cursoRepository,
            CategoriaRepository categoriaRepository,
            ModalidadRepository modalidadRepository
    ) {
        this.cursoRepository = cursoRepository;
        this.categoriaRepository = categoriaRepository;
        this.modalidadRepository = modalidadRepository;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CursoResponseDTO> listar(
            String texto,
            Long idCategoria,
            Long idModalidad,
            Boolean activo,
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

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direccion.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("La dirección debe ser 'asc' o 'desc'");
        }

        String textoNormalizado = normalizarFiltro(texto);
        PageRequest pageRequest = PageRequest.of(pagina, tamano, Sort.by(sortDirection, propiedad));
        Page<CursoResponseDTO> resultado = cursoRepository
                .buscar(textoNormalizado, idCategoria, idModalidad, activo, pageRequest)
                .map(this::aRespuesta);

        return PaginaDTO.desde(resultado);
    }

    @Transactional(readOnly = true)
    public CursoResponseDTO obtenerPorId(Long id) {
        return aRespuesta(buscarCurso(id));
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponseDTO> listarCategorias() {
        return categoriaRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre"))
                .stream()
                .map(categoria -> new CatalogoResponseDTO(
                        categoria.getIdCategoria(), categoria.getNombre(), categoria.getDescripcion()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponseDTO> listarModalidades() {
        return modalidadRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre"))
                .stream()
                .map(modalidad -> new CatalogoResponseDTO(
                        modalidad.getIdModalidad(), modalidad.getNombre(), modalidad.getDescripcion()
                ))
                .toList();
    }

    @Transactional
    public CursoResponseDTO crear(CursoDTO dto) {
        validarFechas(dto, null);
        validarHorarioYDuracion(dto);
        Curso curso = new Curso();
        asignarDatos(curso, dto);
        curso.setActivo(true);
        return aRespuesta(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponseDTO actualizar(Long id, CursoDTO dto) {
        Curso curso = buscarCurso(id);
        validarFechas(dto, curso.getFechaInicio());
        validarHorarioYDuracion(dto);
        asignarDatos(curso, dto);
        return aRespuesta(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponseDTO cambiarEstado(Long id, Boolean activo) {
        Curso curso = buscarCurso(id);
        curso.setActivo(activo);
        return aRespuesta(cursoRepository.save(curso));
    }

    @Transactional
    public void eliminar(Long id) {
        Curso curso = buscarCurso(id);
        try {
            cursoRepository.delete(curso);
            cursoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "No se puede eliminar el curso porque posee inscripciones, actividades u otros registros relacionados"
            );
        }
    }

    private Curso buscarCurso(Long id) {
        return cursoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso", id));
    }

    private void asignarDatos(Curso curso, CursoDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", dto.getIdCategoria()));
        Modalidad modalidad = modalidadRepository.findById(dto.getIdModalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("Modalidad", dto.getIdModalidad()));

        curso.setTitulo(dto.getTitulo().strip());
        curso.setDescripcion(dto.getDescripcion().strip());
        curso.setCupoMaximo(dto.getCupoMaximo());
        curso.setCosto(dto.getCosto());
        curso.setFechaInicio(dto.getFechaInicio());
        curso.setFechaFin(dto.getFechaFin());
        curso.setDuracionHoras(dto.getDuracionHoras());
        curso.setHorario(dto.getHorario().strip());
        curso.setCategoria(categoria);
        curso.setModalidad(modalidad);
    }

    private void validarFechas(CursoDTO dto, LocalDate fechaInicioActual) {
        if (dto.getFechaInicio() != null
                && dto.getFechaFin() != null
                && dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        if (dto.getFechaInicio() != null
                && dto.getFechaInicio().isBefore(LocalDate.now())
                && !dto.getFechaInicio().equals(fechaInicioActual)) {
            throw new ReglaNegocioException("La fecha de inicio no puede estar en el pasado");
        }
    }

    private void validarHorarioYDuracion(CursoDTO dto) {
        String horario = dto.getHorario();
        if (horario == null || horario.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar al menos un día y un rango de horas");
        }
        int separador = horario.lastIndexOf(" | ");
        if (separador < 1) {
            throw new ReglaNegocioException("El horario no tiene el formato esperado");
        }

        Set<DayOfWeek> dias = new HashSet<>();
        for (String nombreDia : horario.substring(0, separador).split(", ")) {
            DayOfWeek dia = DIAS_SEMANA.get(nombreDia);
            if (dia == null || !dias.add(dia)) {
                throw new ReglaNegocioException("El horario contiene días inválidos o repetidos");
            }
        }

        String[] horas = horario.substring(separador + 3).split("-", -1);
        if (horas.length != 2) {
            throw new ReglaNegocioException("El horario no tiene el formato esperado");
        }
        try {
            LocalTime inicio = LocalTime.parse(horas[0]);
            LocalTime fin = LocalTime.parse(horas[1]);
            if (!fin.isAfter(inicio)) {
                throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
            }
            validarDuracionProgramada(dto, dias, inicio, fin);
        } catch (DateTimeParseException ex) {
            throw new ReglaNegocioException("Las horas deben utilizar el formato HH:mm");
        }
    }

    private void validarDuracionProgramada(
            CursoDTO dto,
            Set<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        if (dto.getFechaInicio() == null || dto.getFechaFin() == null || dto.getDuracionHoras() == null) {
            return;
        }

        long sesiones = dto.getFechaInicio()
                .datesUntil(dto.getFechaFin().plusDays(1))
                .filter(fecha -> dias.contains(fecha.getDayOfWeek()))
                .count();
        if (sesiones == 0) {
            throw new ReglaNegocioException(
                    "Los días seleccionados no generan ninguna sesión entre las fechas del curso"
            );
        }

        long minutosPorSesion = Duration.between(horaInicio, horaFin).toMinutes();
        long minutosProgramados = sesiones * minutosPorSesion;
        long minutosDeclarados = dto.getDuracionHoras() * 60L;
        if (minutosProgramados != minutosDeclarados) {
            throw new ReglaNegocioException(
                    "La duración indicada es de " + formatearDuracion(minutosDeclarados)
                            + ", pero el horario programa " + formatearDuracion(minutosProgramados)
                            + " en " + sesiones + (sesiones == 1 ? " sesión" : " sesiones")
                            + " entre las fechas seleccionadas"
            );
        }
    }

    private String formatearDuracion(long minutos) {
        long horas = minutos / 60;
        long restantes = minutos % 60;
        String textoHoras = horas + (horas == 1 ? " hora" : " horas");
        if (restantes == 0) {
            return textoHoras;
        }
        return textoHoras + " y " + restantes + (restantes == 1 ? " minuto" : " minutos");
    }

    private String normalizarFiltro(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }

    private CursoResponseDTO aRespuesta(Curso curso) {
        Categoria categoria = curso.getCategoria();
        Modalidad modalidad = curso.getModalidad();
        return new CursoResponseDTO(
                curso.getIdCurso(),
                curso.getTitulo(),
                curso.getDescripcion(),
                curso.getDuracionHoras(),
                curso.getCupoMaximo(),
                curso.getCosto(),
                curso.getFechaInicio(),
                curso.getFechaFin(),
                curso.getHorario(),
                curso.getActivo(),
                categoria.getIdCategoria(),
                categoria.getNombre(),
                modalidad.getIdModalidad(),
                modalidad.getNombre()
        );
    }
}
