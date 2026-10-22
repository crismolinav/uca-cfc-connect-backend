package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.ucacfcconnect.entity.Pago;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByAlquilerIdAndEstado(Long alquilerId, String estado);
    boolean existsByInscripcionIdAndEstadoIn(Long inscripcionId, List<String> estados);
    List<Pago> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);
}
