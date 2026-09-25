package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Diplomado;

import java.util.Optional;

@Repository
public interface DiplomadoRepository extends JpaRepository<Diplomado, Long> {

    @EntityGraph(attributePaths = {"categoria", "modalidad"})
    @Query("select d from Diplomado d where d.id = :id")
    Optional<Diplomado> buscarPorId(@Param("id") Long id);

    @EntityGraph(attributePaths = {"categoria", "modalidad"})
    @Query(value = """
            select d from Diplomado d
            where (:texto is null
                   or lower(d.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(d.descripcion, '')) like lower(concat('%', :texto, '%')))
              and (:idCategoria is null or d.categoria.idCategoria = :idCategoria)
              and (:idModalidad is null or d.modalidad.idModalidad = :idModalidad)
              and (:activo is null or d.activo = :activo)
            """,
            countQuery = """
            select count(d) from Diplomado d
            where (:texto is null
                   or lower(d.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(d.descripcion, '')) like lower(concat('%', :texto, '%')))
              and (:idCategoria is null or d.categoria.idCategoria = :idCategoria)
              and (:idModalidad is null or d.modalidad.idModalidad = :idModalidad)
              and (:activo is null or d.activo = :activo)
            """)
    Page<Diplomado> buscar(
            @Param("texto") String texto,
            @Param("idCategoria") Long idCategoria,
            @Param("idModalidad") Long idModalidad,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
