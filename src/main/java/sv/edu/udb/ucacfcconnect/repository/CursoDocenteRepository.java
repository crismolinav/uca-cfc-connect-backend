package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.CursoDocente;

import java.util.List;

@Repository
public interface CursoDocenteRepository extends JpaRepository<CursoDocente, Long> {

    @EntityGraph(attributePaths = "docente")
    List<CursoDocente> findByCurso_IdCursoOrderByDocente_NombreAsc(Long idCurso);

    @EntityGraph(attributePaths = "curso")
    List<CursoDocente> findByDocente_Id(Long idDocente);

    boolean existsByDocente_Id(Long idDocente);

    void deleteByCurso_IdCursoAndDocente_Id(Long idCurso, Long idDocente);
}
