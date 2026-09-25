package sv.edu.udb.ucacfcconnect.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.*;
import sv.edu.udb.ucacfcconnect.entity.Alquiler;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Espacio;
import sv.edu.udb.ucacfcconnect.exception.ConflictException;
import sv.edu.udb.ucacfcconnect.exception.RecursoNoEncontradoException;
import sv.edu.udb.ucacfcconnect.exception.ReglaNegocioException;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;
import sv.edu.udb.ucacfcconnect.repository.AlquilerRepository;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.EspacioRepository;

import java.time.LocalDate;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AlquilerService {
    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "CONFIRMADO", "CANCELADO", "FINALIZADO");
    private static final Map<String, String> ORDEN_ESPACIOS = Map.of(
            "idEspacio", "id", "nombre", "nombre", "tipo", "tipo",
            "capacidad", "capacidad", "precio", "precio", "disponible", "disponible"
    );
    private static final Map<String, String> ORDEN_ALQUILERES = Map.of(
            "idAlquiler", "id", "fecha", "fecha", "horaInicio", "horaInicio",
            "estado", "estado", "cliente", "cliente.nombre", "espacio", "espacio.nombre"
    );

    private final EspacioRepository espacios;
    private final AlquilerRepository alquileres;
    private final ClienteRepository clientes;

    public AlquilerService(EspacioRepository espacios, AlquilerRepository alquileres, ClienteRepository clientes) {
        this.espacios = espacios;
        this.alquileres = alquileres;
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<EspacioResponseDTO> listarEspacios(String texto, Boolean disponible, Integer capacidadMinima,
                                                        int pagina, int tamano, String ordenarPor, String direccion) {
        String propiedad = validarOrden(ORDEN_ESPACIOS, ordenarPor);
        return PaginaDTO.desde(espacios.buscar(normalizar(texto), disponible, capacidadMinima,
                PageRequest.of(pagina, tamano, Sort.by(validarDireccion(direccion), propiedad)))
                .map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public EspacioResponseDTO obtenerEspacio(Long id) { return aRespuesta(buscarEspacio(id)); }

    @Transactional
    public EspacioResponseDTO crearEspacio(EspacioDTO dto) {
        validarNombreEspacio(dto.nombre(), null);
        Espacio espacio = new Espacio();
        aplicar(espacio, dto);
        return aRespuesta(espacios.save(espacio));
    }

    @Transactional
    public EspacioResponseDTO actualizarEspacio(Long id, EspacioDTO dto) {
        Espacio espacio = buscarEspacio(id);
        validarNombreEspacio(dto.nombre(), id);
        aplicar(espacio, dto);
        return aRespuesta(espacios.save(espacio));
    }

    @Transactional
    public void eliminarEspacio(Long id) {
        Espacio espacio = buscarEspacio(id);
        if (alquileres.existsByEspacio_Id(id)) {
            throw new ConflictException("No se puede eliminar el espacio porque posee alquileres relacionados");
        }
        try {
            espacios.delete(espacio);
            espacios.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se puede eliminar el espacio porque posee cotizaciones relacionadas");
        }
    }

    @Transactional(readOnly = true)
    public PaginaDTO<AlquilerResponseDTO> listarAlquileres(String texto, String estado, Long idEspacio,
                                                            LocalDate fecha, int pagina, int tamano,
                                                            String ordenarPor, String direccion) {
        String estadoNormalizado = estado == null || estado.isBlank() ? null : validarEstado(estado);
        String propiedad = validarOrden(ORDEN_ALQUILERES, ordenarPor);
        return PaginaDTO.desde(alquileres.buscar(normalizar(texto), estadoNormalizado, idEspacio, fecha,
                PageRequest.of(pagina, tamano, Sort.by(validarDireccion(direccion), propiedad)))
                .map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public AlquilerResponseDTO obtenerAlquiler(Long id) { return aRespuesta(buscarAlquiler(id)); }

    @Transactional
    public AlquilerResponseDTO crearAlquiler(AlquilerDTO dto) {
        Alquiler alquiler = new Alquiler();
        alquiler.setEstado("PENDIENTE");
        aplicar(alquiler, dto, null);
        return aRespuesta(alquileres.save(alquiler));
    }

    @Transactional(readOnly = true)
    public List<AlquilerResponseDTO> listarAlquileresDelUsuario(Long idUsuario) {
        return alquileres.findByCliente_Usuario_IdOrderByFechaDescHoraInicioDesc(idUsuario)
                .stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public AlquilerResponseDTO solicitarAlquiler(Long idUsuario, SolicitudAlquilerDTO dto) {
        Cliente cliente = clientes.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("Completa tu registro de cliente antes de reservar"));
        return crearAlquiler(new AlquilerDTO(cliente.getId(), dto.idEspacio(), dto.fecha(),
                dto.horaInicio(), dto.horaFin(), dto.motivo()));
    }

    @Transactional
    public AlquilerResponseDTO cancelarAlquilerDelUsuario(Long idUsuario, Long idAlquiler) {
        Alquiler alquiler = buscarAlquiler(idAlquiler);
        if (alquiler.getCliente().getUsuario() == null
                || !alquiler.getCliente().getUsuario().getId().equals(idUsuario)) {
            throw new RecursoNoEncontradoException("Alquiler", idAlquiler);
        }
        if (!Set.of("PENDIENTE", "CONFIRMADO").contains(alquiler.getEstado())) {
            throw new ConflictException("Solo se puede cancelar una reserva pendiente o confirmada");
        }
        alquiler.setEstado("CANCELADO");
        return aRespuesta(alquileres.save(alquiler));
    }

    @Transactional
    public AlquilerResponseDTO actualizarAlquiler(Long id, AlquilerDTO dto) {
        Alquiler alquiler = buscarAlquiler(id);
        if (!"PENDIENTE".equals(alquiler.getEstado())) {
            throw new ConflictException("Solo se puede editar un alquiler pendiente");
        }
        aplicar(alquiler, dto, id);
        return aRespuesta(alquileres.save(alquiler));
    }

    @Transactional
    public AlquilerResponseDTO cambiarEstado(Long id, String estado) {
        Alquiler alquiler = buscarAlquiler(id);
        String nuevoEstado = validarEstado(estado);
        if ("CONFIRMADO".equals(nuevoEstado)) {
            validarDisponibilidad(alquiler.getEspacio(), alquiler.getFecha(), alquiler.getHoraInicio(),
                    alquiler.getHoraFin(), alquiler.getId());
        }
        alquiler.setEstado(nuevoEstado);
        return aRespuesta(alquileres.save(alquiler));
    }

    @Transactional
    public void eliminarAlquiler(Long id) {
        Alquiler alquiler = buscarAlquiler(id);
        try {
            alquileres.delete(alquiler);
            alquileres.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se puede eliminar el alquiler porque posee registros relacionados");
        }
    }

    private void aplicar(Espacio espacio, EspacioDTO dto) {
        espacio.setNombre(dto.nombre().strip());
        espacio.setTipo(dto.tipo().strip());
        espacio.setCapacidad(dto.capacidad());
        espacio.setPrecio(dto.precio());
        espacio.setEquipamiento(normalizar(dto.equipamiento()));
        espacio.setDuracionMaximaHoras(dto.duracionMaximaHoras());
        espacio.setDisponible(dto.disponible());
    }

    private void aplicar(Alquiler alquiler, AlquilerDTO dto, Long idExcluir) {
        if (!dto.horaFin().isAfter(dto.horaInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }
        Cliente cliente = clientes.findById(dto.idCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", dto.idCliente()));
        Espacio espacio = buscarEspacio(dto.idEspacio());
        long minutosSolicitados = Duration.between(dto.horaInicio(), dto.horaFin()).toMinutes();
        long minutosPermitidos = espacio.getDuracionMaximaHoras().multiply(BigDecimal.valueOf(60)).longValue();
        if (minutosSolicitados > minutosPermitidos) {
            throw new ReglaNegocioException("La reserva no puede superar "
                    + espacio.getDuracionMaximaHoras().stripTrailingZeros().toPlainString()
                    + " horas para este espacio");
        }
        validarDisponibilidad(espacio, dto.fecha(), dto.horaInicio(), dto.horaFin(), idExcluir);
        alquiler.setCliente(cliente);
        alquiler.setEspacio(espacio);
        alquiler.setFecha(dto.fecha());
        alquiler.setHoraInicio(dto.horaInicio());
        alquiler.setHoraFin(dto.horaFin());
        alquiler.setMotivo(normalizar(dto.motivo()));
    }

    private void validarDisponibilidad(Espacio espacio, LocalDate fecha, java.time.LocalTime inicio,
                                       java.time.LocalTime fin, Long idExcluir) {
        if (!espacio.isDisponible()) throw new ConflictException("El espacio no está disponible para alquiler");
        if (alquileres.existeCruce(espacio.getId(), fecha, inicio, fin, idExcluir)) {
            throw new ConflictException("El espacio ya está reservado en ese horario");
        }
    }

    private void validarNombreEspacio(String nombre, Long idActual) {
        boolean existe = idActual == null ? espacios.existsByNombreIgnoreCase(nombre.strip())
                : espacios.existsByNombreIgnoreCaseAndIdNot(nombre.strip(), idActual);
        if (existe) throw new ConflictException("Ya existe un espacio con ese nombre");
    }

    private Espacio buscarEspacio(Long id) {
        return espacios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Espacio", id));
    }

    private Alquiler buscarAlquiler(Long id) {
        return alquileres.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Alquiler", id));
    }

    private String validarEstado(String estado) {
        String valor = estado.strip().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(valor)) throw new SolicitudInvalidaException("Estado de alquiler inválido");
        return valor;
    }

    private String validarOrden(Map<String, String> permitidos, String campo) {
        String propiedad = permitidos.get(campo);
        if (propiedad == null) throw new SolicitudInvalidaException("Campo de ordenamiento no permitido");
        return propiedad;
    }

    private Sort.Direction validarDireccion(String direccion) {
        try { return Sort.Direction.fromString(direccion); }
        catch (IllegalArgumentException ex) { throw new SolicitudInvalidaException("La dirección debe ser 'asc' o 'desc'"); }
    }

    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }

    private EspacioResponseDTO aRespuesta(Espacio e) {
        return new EspacioResponseDTO(e.getId(), e.getNombre(), e.getTipo(), e.getCapacidad(),
                e.getPrecio(), e.isDisponible(), e.getEquipamiento(), e.getDuracionMaximaHoras());
    }

    private AlquilerResponseDTO aRespuesta(Alquiler a) {
        return new AlquilerResponseDTO(a.getId(), a.getFecha(), a.getHoraInicio(), a.getHoraFin(),
                a.getMotivo(), a.getEstado(), a.getCliente().getId(), a.getCliente().getNombre(),
                a.getEspacio().getId(), a.getEspacio().getNombre());
    }
}
