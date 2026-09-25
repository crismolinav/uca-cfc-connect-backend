package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Cotizacion;

import java.util.Optional;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.curso", "detalles.espacio"})
    @Query("select distinct c from Cotizacion c where c.id = :id")
    Optional<Cotizacion> buscarPorId(@Param("id") Long id);

    @EntityGraph(attributePaths = "cliente")
    @Query(value = """
            select c from Cotizacion c
            where (:texto is null or lower(c.cliente.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(c.observaciones, '')) like lower(concat('%', :texto, '%')))
              and (:estado is null or c.estado = :estado)
              and (:idCliente is null or c.cliente.id = :idCliente)
            """,
            countQuery = """
            select count(c) from Cotizacion c
            where (:texto is null or lower(c.cliente.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(c.observaciones, '')) like lower(concat('%', :texto, '%')))
              and (:estado is null or c.estado = :estado)
              and (:idCliente is null or c.cliente.id = :idCliente)
            """)
    Page<Cotizacion> buscar(@Param("texto") String texto, @Param("estado") String estado,
                            @Param("idCliente") Long idCliente, Pageable pageable);
}
