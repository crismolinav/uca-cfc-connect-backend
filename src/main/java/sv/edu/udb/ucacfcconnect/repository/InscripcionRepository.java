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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscripcion i where i.id = :id")
    Optional<Inscripcion> bloquear(@Param("id") Long id);
}
