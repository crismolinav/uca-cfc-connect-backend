package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.udb.ucacfcconnect.entity.Cliente;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByDuiNit(String duiNit);
    Optional<Cliente> findByDuiNit(String duiNit);
    Optional<Cliente> findByUsuarioId(Long usuarioId);

    @Query("""
            select c from Cliente c
            where lower(c.nombre) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.correo, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.duiNit, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.empresa, '')) like lower(concat('%', :q, '%'))
            """)
    Page<Cliente> buscar(@Param("q") String q, Pageable pageable);
}
