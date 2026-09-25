package sv.edu.udb.ucacfcconnect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.udb.ucacfcconnect.entity.Categoria;
import sv.edu.udb.ucacfcconnect.entity.Modalidad;
import sv.edu.udb.ucacfcconnect.repository.CategoriaRepository;
import sv.edu.udb.ucacfcconnect.repository.ModalidadRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoCatalogoInitializerTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ModalidadRepository modalidadRepository;

    @Test
    void creaLosCatalogosInicialesCuandoNoExisten() {
        when(categoriaRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(modalidadRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.empty());

        new CursoCatalogoInitializer(categoriaRepository, modalidadRepository).run(null);

        verify(categoriaRepository, times(3)).save(any(Categoria.class));
        verify(modalidadRepository, times(3)).save(any(Modalidad.class));
    }

    @Test
    void noDuplicaCatalogosExistentes() {
        when(categoriaRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.of(new Categoria()));
        when(modalidadRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.of(new Modalidad()));

        new CursoCatalogoInitializer(categoriaRepository, modalidadRepository).run(null);

        verify(categoriaRepository, never()).save(any());
        verify(modalidadRepository, never()).save(any());
    }
}
