package sv.edu.udb.ucacfcconnect.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.udb.ucacfcconnect.entity.SolicitudCatering;

import java.util.Optional;

public interface SolicitudCateringRepository extends JpaRepository<SolicitudCatering, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudCatering s where s.id = :id")
    Optional<SolicitudCatering> bloquear(@Param("id") Long id);
}
