package sv.edu.udb.ucacfcconnect.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.udb.ucacfcconnect.entity.Inscripcion;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByParticipanteIdAndCursoIdCurso(Long participanteId, Long cursoId);
    boolean existsByParticipanteIdAndDiplomadoId(Long participanteId, Long diplomadoId);
    long countByCursoIdCursoAndEstadoNot(Long cursoId, String estado);
    List<Inscripcion> findByParticipanteClienteIdOrderByFechaDescIdDesc(Long clienteId);

    @Query("""
            select i from Inscripcion i
            where i.estado = 'PENDIENTE' and i.total > 0
              and (:clienteId is null or i.participante.cliente.id = :clienteId)
              and not exists (select p.id from Pago p where p.inscripcion = i
                              and p.estado in ('PENDIENTE', 'CONFIRMADO'))
            order by i.id desc
            """)
    List<Inscripcion> pendientesDePago(@Param("clienteId") Long clienteId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"curso"})
    @Query("select i from Inscripcion i join i.participante p join p.cliente cl " +
            "where cl.usuario.id = :usuarioId and i.curso.idCurso = :cursoId " +
            "and upper(i.estado) <> 'CANCELADA' order by i.id desc")
    List<Inscripcion> buscarInscripcionCursoDeUsuario(@Param("usuarioId") Long usuarioId,
                                                       @Param("cursoId") Long cursoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscripcion i where i.id = :id")
    Optional<Inscripcion> bloquear(@Param("id") Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"participante", "participante.cliente", "participante.cliente.usuario", "curso", "curso.docentes", "curso.docentes.docente"})
    Optional<Inscripcion> findByIdAndParticipante_Cliente_Usuario_IdAndCurso_IdCurso(
            Long id, Long usuarioId, Long cursoId);
}
