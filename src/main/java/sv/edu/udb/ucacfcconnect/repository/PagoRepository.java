package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.ucacfcconnect.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByAlquilerIdAndEstado(Long alquilerId, String estado);
}
