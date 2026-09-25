package sv.edu.udb.ucacfcconnect.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.udb.ucacfcconnect.entity.Espacio;

import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espacio e where e.id = :id")
    Optional<Espacio> bloquear(@Param("id") Long id);
}
