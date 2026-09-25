package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Docente;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    @Query(value = """
            select d from Docente d
            where (:texto is null
                   or lower(d.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(d.especialidad, '')) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(d.correo, '')) like lower(concat('%', :texto, '%')))
            """,
            countQuery = """
            select count(d) from Docente d
            where (:texto is null
                   or lower(d.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(d.especialidad, '')) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(d.correo, '')) like lower(concat('%', :texto, '%')))
            """)
    Page<Docente> buscar(@Param("texto") String texto, Pageable pageable);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);
}
