package sv.edu.udb.ucacfcconnect.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Curso;

import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Curso c where c.idCurso = :id")
    Optional<Curso> bloquear(@Param("id") Long id);

    @EntityGraph(attributePaths = {"categoria", "modalidad"})
    @Query("select c from Curso c where c.idCurso = :id")
    Optional<Curso> buscarPorId(@Param("id") Long id);

    @EntityGraph(attributePaths = {"categoria", "modalidad"})
    @Query(value = """
            select c from Curso c
            where (:texto is null
                   or lower(c.titulo) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(c.descripcion, '')) like lower(concat('%', :texto, '%')))
              and (:idCategoria is null or c.categoria.idCategoria = :idCategoria)
              and (:idModalidad is null or c.modalidad.idModalidad = :idModalidad)
              and (:activo is null or c.activo = :activo)
            """,
            countQuery = """
            select count(c) from Curso c
            where (:texto is null
                   or lower(c.titulo) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(c.descripcion, '')) like lower(concat('%', :texto, '%')))
              and (:idCategoria is null or c.categoria.idCategoria = :idCategoria)
              and (:idModalidad is null or c.modalidad.idModalidad = :idModalidad)
              and (:activo is null or c.activo = :activo)
            """)
    Page<Curso> buscar(
            @Param("texto") String texto,
            @Param("idCategoria") Long idCategoria,
            @Param("idModalidad") Long idModalidad,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
