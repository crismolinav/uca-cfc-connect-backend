package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.edu.udb.ucacfcconnect.entity.Alquiler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {
    List<Alquiler> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

    @Query("""
            select count(a) > 0 from Alquiler a
            where a.espacio.id = :espacioId and a.fecha = :fecha and a.estado <> 'CANCELADO'
              and a.horaInicio < :fin and a.horaFin > :inicio
            """)
    boolean existeCruce(Long espacioId, LocalDate fecha, LocalTime inicio, LocalTime fin);
}
