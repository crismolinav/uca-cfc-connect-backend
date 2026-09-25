package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;

@Repository
public interface ModalidadRepository extends JpaRepository<Modalidad, Long> {
}
