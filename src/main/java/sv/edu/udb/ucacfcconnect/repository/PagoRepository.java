package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import sv.edu.udb.ucacfcconnect.entity.Pago;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByAlquilerIdAndEstado(Long alquilerId, String estado);
    boolean existsByInscripcionIdAndEstadoIn(Long inscripcionId, List<String> estados);
    java.util.Optional<Pago> findByReferencia(String referencia);
    List<Pago> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);

    @Query("""
            select p from Pago p left join p.inscripcion i
            where p.estado = 'PENDIENTE' and (i is null or i.estado = 'PENDIENTE')
            order by p.id desc
            """)
    List<Pago> pendientesRecepcion();

    @Query("""
            select p from Pago p join p.cliente c
            where (:desde is null or p.fecha >= :desde)
              and (:hasta is null or p.fecha <= :hasta)
              and (:estado is null or p.estado = :estado)
              and (:metodo is null or p.metodo = :metodo)
              and (:clienteId is null or c.id = :clienteId)
              and (:q = '' or lower(c.nombre) like lower(concat('%', :q, '%'))
                   or lower(coalesce(c.carnet, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.referencia, '')) like lower(concat('%', :q, '%')))
            """)
    Page<Pago> historialRecepcion(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                 @Param("estado") String estado, @Param("metodo") String metodo,
                                 @Param("clienteId") Long clienteId, @Param("q") String q, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    java.util.Optional<Pago> bloquear(@Param("id") Long id);
}
