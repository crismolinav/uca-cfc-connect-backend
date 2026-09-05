package sv.edu.udb.ucacfcconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.udb.ucacfcconnect.entity.Curso;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    // Spring Boot te regala todos los métodos CRUD básicos (save, findById, findAll, etc.)
}