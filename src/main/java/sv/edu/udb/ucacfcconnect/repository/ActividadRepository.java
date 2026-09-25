package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Actividad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    @EntityGraph(attributePaths = "diplomado")
    List<Actividad> findByDiplomado_IdOrderByFechaAscHoraInicioAsc(Long idDiplomado);

    @EntityGraph(attributePaths = "diplomado")
    Optional<Actividad> findByIdAndDiplomado_Id(Long idActividad, Long idDiplomado);

    boolean existsByDiplomado_Id(Long idDiplomado);

    @Query("""
            select a from Actividad a
            where a.fecha = :fecha
              and a.horaInicio < :horaFin
              and a.horaFin > :horaInicio
              and (:idExcluir is null or a.id <> :idExcluir)
            order by a.horaInicio
            """)
    List<Actividad> buscarConflictos(
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("idExcluir") Long idExcluir
    );
}
