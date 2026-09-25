package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Alquiler;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {
    @EntityGraph(attributePaths = {"cliente", "espacio"})
    @Query("select a from Alquiler a where a.id = :id")
    Optional<Alquiler> buscarPorId(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cliente", "espacio"})
    @Query(value = """
            select a from Alquiler a
            where (:texto is null or lower(a.cliente.nombre) like lower(concat('%', :texto, '%'))
                   or lower(a.espacio.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(a.motivo, '')) like lower(concat('%', :texto, '%')))
              and (:estado is null or a.estado = :estado)
              and (:idEspacio is null or a.espacio.id = :idEspacio)
              and (:fecha is null or a.fecha = :fecha)
            """,
            countQuery = """
            select count(a) from Alquiler a
            where (:texto is null or lower(a.cliente.nombre) like lower(concat('%', :texto, '%'))
                   or lower(a.espacio.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(a.motivo, '')) like lower(concat('%', :texto, '%')))
              and (:estado is null or a.estado = :estado)
              and (:idEspacio is null or a.espacio.id = :idEspacio)
              and (:fecha is null or a.fecha = :fecha)
            """)
    Page<Alquiler> buscar(@Param("texto") String texto, @Param("estado") String estado,
                          @Param("idEspacio") Long idEspacio, @Param("fecha") LocalDate fecha,
                          Pageable pageable);

    @Query("""
            select (count(a) > 0) from Alquiler a
            where a.espacio.id = :idEspacio and a.fecha = :fecha
              and a.estado not in ('CANCELADO', 'RECHAZADO')
              and a.horaInicio < :horaFin and a.horaFin > :horaInicio
              and (:idExcluir is null or a.id <> :idExcluir)
            """)
    boolean existeCruce(@Param("idEspacio") Long idEspacio, @Param("fecha") LocalDate fecha,
                        @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
                        @Param("idExcluir") Long idExcluir);
    boolean existsByEspacio_Id(Long idEspacio);

    @EntityGraph(attributePaths = {"cliente", "espacio"})
    List<Alquiler> findByCliente_Usuario_IdOrderByFechaDescHoraInicioDesc(Long idUsuario);
}
