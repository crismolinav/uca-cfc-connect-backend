package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.ucacfcconnect.entity.Participante;
import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    Optional<Participante> findFirstByClienteIdAndCorreoIgnoreCase(Long clienteId, String correo);
    Optional<Participante> findFirstByClienteIdOrderByIdAsc(Long clienteId);
    boolean existsByClienteId(Long clienteId);
}
