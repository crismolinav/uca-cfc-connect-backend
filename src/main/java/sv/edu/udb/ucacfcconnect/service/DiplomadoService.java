package sv.edu.udb.ucacfcconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.ActividadDiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.ActividadResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.CatalogoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.DiplomadoDTO;
import sv.edu.udb.ucacfcconnect.dto.DiplomadoResponseDTO;
import sv.edu.udb.ucacfcconnect.dto.PaginaDTO;
import sv.edu.udb.ucacfcconnect.entity.Actividad;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Diplomado;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;
import sv.edu.udb.ucacfcconnect.repository.ActividadRepository;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.DiplomadoRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DiplomadoService {

    private static final String TIPO_DIPLOMADO = "DIPLOMADO";
    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.ofEntries(
            Map.entry("idDiplomado", "id"),
            Map.entry("nombre", "nombre"),
            Map.entry("duracionHoras", "duracionHoras"),
            Map.entry("costo", "costo"),
            Map.entry("fechaInicio", "fechaInicio"),
            Map.entry("fechaFin", "fechaFin"),
            Map.entry("activo", "activo"),
            Map.entry("categoria", "categoria.nombre"),
            Map.entry("modalidad", "modalidad.nombre")
    );

    private final DiplomadoRepository diplomadoRepository;
    private final ActividadRepository actividadRepository;
    private final CategoriaRepository categoriaRepository;
    private final ModalidadRepository modalidadRepository;

    public DiplomadoService(
            DiplomadoRepository diplomadoRepository,
            ActividadRepository actividadRepository,
            CategoriaRepository categoriaRepository,
            ModalidadRepository modalidadRepository
    ) {
        this.diplomadoRepository = diplomadoRepository;
        this.actividadRepository = actividadRepository;
        this.categoriaRepository = categoriaRepository;
        this.modalidadRepository = modalidadRepository;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<DiplomadoResponseDTO> listar(
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

        PageRequest pageRequest = PageRequest.of(pagina, tamano, Sort.by(sortDirection, propiedad));
        Page<DiplomadoResponseDTO> resultado = diplomadoRepository.buscar(
                normalizarFiltro(texto), idCategoria, idModalidad, activo, pageRequest
        ).map(this::aRespuesta);
        return PaginaDTO.desde(resultado);
    }

    @Transactional(readOnly = true)
    public DiplomadoResponseDTO obtenerPorId(Long id) {
        return aRespuesta(buscarDiplomado(id));
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponseDTO> listarCategorias() {
        return categoriaRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre")).stream()
                .map(categoria -> new CatalogoResponseDTO(
                        categoria.getIdCategoria(), categoria.getNombre(), categoria.getDescripcion()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponseDTO> listarModalidades() {
        return modalidadRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre")).stream()
                .map(modalidad -> new CatalogoResponseDTO(
                        modalidad.getIdModalidad(), modalidad.getNombre(), modalidad.getDescripcion()
                ))
                .toList();
    }

    @Transactional
    public DiplomadoResponseDTO crear(DiplomadoDTO dto) {
        validarFechas(dto, null);
        Diplomado diplomado = new Diplomado();
        asignarDatos(diplomado, dto);
        diplomado.setActivo(false);
        return aRespuesta(diplomadoRepository.save(diplomado));
    }

    @Transactional
    public DiplomadoResponseDTO actualizar(Long id, DiplomadoDTO dto) {
        Diplomado diplomado = buscarDiplomado(id);
        validarFechas(dto, diplomado.getFechaInicio());
        validarActividadesContraDatos(dto, diplomado, diplomado.isActivo());
        asignarDatos(diplomado, dto);
        return aRespuesta(diplomadoRepository.save(diplomado));
    }

    @Transactional
    public DiplomadoResponseDTO cambiarEstado(Long id, boolean activo) {
        Diplomado diplomado = buscarDiplomado(id);
        if (activo) {
            validarProgramacionCompleta(diplomado);
        }
        diplomado.setActivo(activo);
        return aRespuesta(diplomadoRepository.save(diplomado));
    }

    @Transactional
    public void eliminar(Long id) {
        Diplomado diplomado = buscarDiplomado(id);
        if (actividadRepository.existsByDiplomado_Id(id)) {
            throw new ConflictException("Elimine primero las sesiones programadas del diplomado");
        }
        try {
            diplomadoRepository.delete(diplomado);
            diplomadoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "No se puede eliminar el diplomado porque posee inscripciones u otros registros relacionados"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ActividadResponseDTO> listarActividades(Long idDiplomado) {
        buscarDiplomado(idDiplomado);
        return actividadRepository.findByDiplomado_IdOrderByFechaAscHoraInicioAsc(idDiplomado)
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public ActividadResponseDTO crearActividad(Long idDiplomado, ActividadDiplomadoDTO dto) {
        Diplomado diplomado = buscarDiplomado(idDiplomado);
        validarActividad(dto, diplomado, null);
        Actividad actividad = new Actividad();
        asignarDatos(actividad, dto, diplomado);
        Actividad guardada = actividadRepository.saveAndFlush(actividad);
        revisarPublicacionTrasCambio(diplomado);
        return aRespuesta(guardada);
    }

    @Transactional
    public ActividadResponseDTO actualizarActividad(
            Long idDiplomado,
            Long idActividad,
            ActividadDiplomadoDTO dto
    ) {
        Diplomado diplomado = buscarDiplomado(idDiplomado);
        Actividad actividad = buscarActividad(idDiplomado, idActividad);
        validarActividad(dto, diplomado, actividad);
        asignarDatos(actividad, dto, diplomado);
        Actividad guardada = actividadRepository.saveAndFlush(actividad);
        revisarPublicacionTrasCambio(diplomado);
        return aRespuesta(guardada);
    }

    @Transactional
    public void eliminarActividad(Long idDiplomado, Long idActividad) {
        Diplomado diplomado = buscarDiplomado(idDiplomado);
        Actividad actividad = buscarActividad(idDiplomado, idActividad);
        actividadRepository.delete(actividad);
        actividadRepository.flush();
        revisarPublicacionTrasCambio(diplomado);
    }

    private Diplomado buscarDiplomado(Long id) {
        return diplomadoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Diplomado", id));
    }

    private Actividad buscarActividad(Long idDiplomado, Long idActividad) {
        return actividadRepository.findByIdAndDiplomado_Id(idActividad, idDiplomado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad", idActividad));
    }

    private void asignarDatos(Diplomado diplomado, DiplomadoDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", dto.idCategoria()));
        Modalidad modalidad = modalidadRepository.findById(dto.idModalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("Modalidad", dto.idModalidad()));

        diplomado.setNombre(dto.nombre().strip());
        diplomado.setDescripcion(dto.descripcion().strip());
        diplomado.setDuracionHoras(dto.duracionHoras());
        diplomado.setCosto(dto.costo());
        diplomado.setFechaInicio(dto.fechaInicio());
        diplomado.setFechaFin(dto.fechaFin());
        diplomado.setCategoria(categoria);
        diplomado.setModalidad(modalidad);
    }

    private void asignarDatos(Actividad actividad, ActividadDiplomadoDTO dto, Diplomado diplomado) {
        actividad.setTitulo(dto.titulo().strip());
        actividad.setTipo(TIPO_DIPLOMADO);
        actividad.setFecha(dto.fecha());
        actividad.setHoraInicio(dto.horaInicio());
        actividad.setHoraFin(dto.horaFin());
        actividad.setCupo(dto.cupo());
        actividad.setDiplomado(diplomado);
        actividad.setCurso(null);
        actividad.setAlquiler(null);
        actividad.setSolicitudCatering(null);
    }

    private void validarFechas(DiplomadoDTO dto, LocalDate fechaInicioActual) {
        if (dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        if (dto.fechaInicio().isBefore(LocalDate.now()) && !dto.fechaInicio().equals(fechaInicioActual)) {
            throw new ReglaNegocioException("La fecha de inicio no puede estar en el pasado");
        }
    }

    private void validarActividad(ActividadDiplomadoDTO dto, Diplomado diplomado, Actividad actual) {
        if (!dto.horaFin().isAfter(dto.horaInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (dto.fecha().isBefore(diplomado.getFechaInicio()) || dto.fecha().isAfter(diplomado.getFechaFin())) {
            throw new ReglaNegocioException("La sesión debe estar dentro de las fechas del diplomado");
        }
        if (dto.fecha().isBefore(LocalDate.now()) && (actual == null || !dto.fecha().equals(actual.getFecha()))) {
            throw new ReglaNegocioException("La fecha de la sesión no puede estar en el pasado");
        }

        Long idExcluir = actual == null ? null : actual.getId();
        List<Actividad> conflictos = actividadRepository.buscarConflictos(
                dto.fecha(), dto.horaInicio(), dto.horaFin(), idExcluir
        );
        if (!conflictos.isEmpty()) {
            Actividad conflicto = conflictos.getFirst();
            throw new ConflictException(
                    "Conflicto de horario con «" + conflicto.getTitulo() + "» ("
                            + conflicto.getHoraInicio() + "-" + conflicto.getHoraFin() + ")"
            );
        }

        long minutosActuales = minutosProgramados(diplomado.getId(), idExcluir);
        long minutosNuevos = Duration.between(dto.horaInicio(), dto.horaFin()).toMinutes();
        if (minutosActuales + minutosNuevos > diplomado.getDuracionHoras() * 60L) {
            throw new ReglaNegocioException(
                    "La sesión supera la duración total del diplomado. Hay "
                            + formatearDuracion(minutosActuales) + " programadas de "
                            + diplomado.getDuracionHoras() + " horas"
            );
        }
    }

    private void validarActividadesContraDatos(DiplomadoDTO dto, Diplomado diplomado, boolean exigirTotal) {
        List<Actividad> actividades = actividadRepository.findByDiplomado_IdOrderByFechaAscHoraInicioAsc(
                diplomado.getId()
        );
        for (Actividad actividad : actividades) {
            if (actividad.getFecha().isBefore(dto.fechaInicio()) || actividad.getFecha().isAfter(dto.fechaFin())) {
                throw new ReglaNegocioException(
                        "Las nuevas fechas dejarían fuera la sesión «" + actividad.getTitulo() + "»"
                );
            }
        }
        long minutos = actividades.stream().mapToLong(this::duracionMinutos).sum();
        long declarados = dto.duracionHoras() * 60L;
        if (minutos > declarados || (exigirTotal && minutos != declarados)) {
            throw new ReglaNegocioException(
                    "La programación suma " + formatearDuracion(minutos)
                            + " y debe coincidir con las " + dto.duracionHoras() + " horas del diplomado"
            );
        }
    }

    private void validarProgramacionCompleta(Diplomado diplomado) {
        long minutos = minutosProgramados(diplomado.getId(), null);
        long declarados = diplomado.getDuracionHoras() * 60L;
        if (minutos == 0 || minutos != declarados) {
            throw new ReglaNegocioException(
                    "Para publicar el diplomado, sus sesiones deben sumar exactamente "
                            + diplomado.getDuracionHoras() + " horas. Actualmente suman "
                            + formatearDuracion(minutos)
            );
        }
    }

    private void revisarPublicacionTrasCambio(Diplomado diplomado) {
        if (diplomado.isActivo()
                && minutosProgramados(diplomado.getId(), null) != diplomado.getDuracionHoras() * 60L) {
            diplomado.setActivo(false);
            diplomadoRepository.save(diplomado);
        }
    }

    private long minutosProgramados(Long idDiplomado, Long idExcluir) {
        return actividadRepository.findByDiplomado_IdOrderByFechaAscHoraInicioAsc(idDiplomado).stream()
                .filter(actividad -> idExcluir == null || !actividad.getId().equals(idExcluir))
                .mapToLong(this::duracionMinutos)
                .sum();
    }

    private long duracionMinutos(Actividad actividad) {
        return Duration.between(actividad.getHoraInicio(), actividad.getHoraFin()).toMinutes();
    }

    private String formatearDuracion(long minutos) {
        long horas = minutos / 60;
        long restantes = minutos % 60;
        if (restantes == 0) {
            return horas + (horas == 1 ? " hora" : " horas");
        }
        return horas + (horas == 1 ? " hora" : " horas") + " y " + restantes + " minutos";
    }

    private String normalizarFiltro(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private DiplomadoResponseDTO aRespuesta(Diplomado diplomado) {
        return new DiplomadoResponseDTO(
                diplomado.getId(),
                diplomado.getNombre(),
                diplomado.getDescripcion(),
                diplomado.getDuracionHoras(),
                diplomado.getCosto(),
                diplomado.getFechaInicio(),
                diplomado.getFechaFin(),
                diplomado.isActivo(),
                diplomado.getCategoria().getIdCategoria(),
                diplomado.getCategoria().getNombre(),
                diplomado.getModalidad().getIdModalidad(),
                diplomado.getModalidad().getNombre()
        );
    }

    private ActividadResponseDTO aRespuesta(Actividad actividad) {
        return new ActividadResponseDTO(
                actividad.getId(),
                actividad.getTitulo(),
                actividad.getTipo(),
                actividad.getFecha(),
                actividad.getHoraInicio(),
                actividad.getHoraFin(),
                actividad.getCupo(),
                actividad.getDiplomado() == null ? null : actividad.getDiplomado().getId()
        );
    }
}
