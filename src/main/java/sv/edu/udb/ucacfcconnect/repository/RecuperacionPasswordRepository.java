package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import sv.edu.udb.ucacfcconnect.entity.RecuperacionPassword;

import java.util.Optional;

public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPassword, Long> {
    Optional<RecuperacionPassword> findTopByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecuperacionPassword> findByTokenHash(String tokenHash);
}
