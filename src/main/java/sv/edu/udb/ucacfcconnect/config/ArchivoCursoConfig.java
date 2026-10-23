package sv.edu.udb.ucacfcconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ArchivoCursoConfig implements WebMvcConfigurer {
    private final String ubicacion;

    public ArchivoCursoConfig(@Value("${app.upload-dir:uploads}") String directorio) {
        String uri = Path.of(directorio).toAbsolutePath().normalize().toUri().toString();
        this.ubicacion = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/archivos/**").addResourceLocations(ubicacion);
    }
}
