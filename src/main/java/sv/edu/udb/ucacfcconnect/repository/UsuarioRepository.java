package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import sv.edu.udb.ucacfcconnect.entity.Usuario;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByIdAndActivoTrue(Long id);
    List<Usuario> findAllByRolNombreIgnoreCaseOrderByNombreAsc(String rol);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    @Override
    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findById(Long id);
}
