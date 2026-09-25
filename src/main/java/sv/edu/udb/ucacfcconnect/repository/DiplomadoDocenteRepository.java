package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.DiplomadoDocente;

import java.util.List;

@Repository
public interface DiplomadoDocenteRepository extends JpaRepository<DiplomadoDocente, Long> {

    @EntityGraph(attributePaths = "docente")
    List<DiplomadoDocente> findByDiplomado_IdOrderByDocente_NombreAsc(Long idDiplomado);

    @EntityGraph(attributePaths = "diplomado")
    List<DiplomadoDocente> findByDocente_Id(Long idDocente);

    boolean existsByDocente_Id(Long idDocente);

    void deleteByDiplomado_IdAndDocente_Id(Long idDiplomado, Long idDocente);
}
