package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Espacio;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    @Query(value = """
            select e from Espacio e
            where (:texto is null or lower(e.nombre) like lower(concat('%', :texto, '%'))
                   or lower(e.tipo) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(e.equipamiento, '')) like lower(concat('%', :texto, '%')))
              and (:disponible is null or e.disponible = :disponible)
              and (:capacidadMinima is null or e.capacidad >= :capacidadMinima)
            """,
            countQuery = """
            select count(e) from Espacio e
            where (:texto is null or lower(e.nombre) like lower(concat('%', :texto, '%'))
                   or lower(e.tipo) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(e.equipamiento, '')) like lower(concat('%', :texto, '%')))
              and (:disponible is null or e.disponible = :disponible)
              and (:capacidadMinima is null or e.capacidad >= :capacidadMinima)
            """)
    Page<Espacio> buscar(@Param("texto") String texto, @Param("disponible") Boolean disponible,
                         @Param("capacidadMinima") Integer capacidadMinima, Pageable pageable);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
