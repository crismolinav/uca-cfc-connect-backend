package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.ucacfcconnect.entity.Inscripcion;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByParticipanteIdAndCursoIdCurso(Long participanteId, Long cursoId);
    boolean existsByParticipanteIdAndDiplomadoId(Long participanteId, Long diplomadoId);
    long countByCursoIdCursoAndEstadoNot(Long cursoId, String estado);
}
