package sv.edu.udb.ucacfcconnect.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.util.List;

@Component
public class CursoCatalogoInitializer implements ApplicationRunner {

    private static final List<CatalogoInicial> CATEGORIAS = List.of(
            new CatalogoInicial("Tecnología", "Cursos de informática, datos y transformación digital"),
            new CatalogoInicial("Administración", "Cursos de gestión, finanzas y desarrollo empresarial"),
            new CatalogoInicial("Idiomas", "Cursos de idiomas para contextos profesionales")
    );

    private static final List<CatalogoInicial> MODALIDADES = List.of(
            new CatalogoInicial("Presencial", "Sesiones impartidas en las instalaciones del centro"),
            new CatalogoInicial("Virtual", "Sesiones impartidas mediante una plataforma en línea"),
            new CatalogoInicial("Híbrida", "Combinación de sesiones presenciales y virtuales")
    );

    private final CategoriaRepository categoriaRepository;
    private final ModalidadRepository modalidadRepository;

    public CursoCatalogoInitializer(
            CategoriaRepository categoriaRepository,
            ModalidadRepository modalidadRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.modalidadRepository = modalidadRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        CATEGORIAS.forEach(this::crearCategoriaSiNoExiste);
        MODALIDADES.forEach(this::crearModalidadSiNoExiste);
    }

    private void crearCategoriaSiNoExiste(CatalogoInicial dato) {
        if (categoriaRepository.findByNombreIgnoreCase(dato.nombre()).isPresent()) {
            return;
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(dato.nombre());
        categoria.setDescripcion(dato.descripcion());
        categoriaRepository.save(categoria);
    }

    private void crearModalidadSiNoExiste(CatalogoInicial dato) {
        if (modalidadRepository.findByNombreIgnoreCase(dato.nombre()).isPresent()) {
            return;
        }
        Modalidad modalidad = new Modalidad();
        modalidad.setNombre(dato.nombre());
        modalidad.setDescripcion(dato.descripcion());
        modalidadRepository.save(modalidad);
    }

    private record CatalogoInicial(String nombre, String descripcion) {
    }
}
