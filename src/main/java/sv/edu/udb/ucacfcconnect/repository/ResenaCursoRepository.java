package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.udb.ucacfcconnect.entity.ResenaCurso;

import java.util.List;
import java.util.Optional;

public interface ResenaCursoRepository extends JpaRepository<ResenaCurso, Long> {
    boolean existsByInscripcion_Id(Long idInscripcion);
    @Query("select case when count(r) > 0 then true else false end from ResenaCurso r where r.inscripcion.participante.cliente.usuario.id = :usuarioId and r.inscripcion.curso.idCurso = :idCurso")
    boolean existeResenaUsuarioCurso(@Param("usuarioId") Long usuarioId, @Param("idCurso") Long idCurso);
    long countByInscripcion_Curso_IdCurso(Long idCurso);
    Optional<ResenaCurso> findByInscripcion_Id(Long idInscripcion);
    List<ResenaCurso> findByInscripcion_Curso_IdCursoOrderByFechaCreacionDesc(Long idCurso);
    List<ResenaCurso> findByDocente_IdAndCalificacionDocenteIsNotNullOrderByFechaCreacionDesc(Long idDocente);

    @Query("select avg(r.calificacionCurso) from ResenaCurso r where r.inscripcion.curso.idCurso = :idCurso")
    Double promedioCurso(Long idCurso);

    @Query("select avg(r.calificacionDocente) from ResenaCurso r where r.docente.id = :idDocente and r.calificacionDocente is not null")
    Double promedioDocente(Long idDocente);

    long countByDocente_IdAndCalificacionDocenteIsNotNull(Long idDocente);
}
