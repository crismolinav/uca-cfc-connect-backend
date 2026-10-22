package sv.edu.udb.ucacfcconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sv.edu.udb.ucacfcconnect.dto.ArchivoCursoDTO;
import sv.edu.udb.ucacfcconnect.exception.SolicitudInvalidaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ArchivoCursoService {
    private static final long TAMANO_MAXIMO = 50L * 1024 * 1024;
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.ofEntries(
            Map.entry("png", "IMAGEN"), Map.entry("jpg", "IMAGEN"), Map.entry("jpeg", "IMAGEN"),
            Map.entry("webp", "IMAGEN"), Map.entry("gif", "IMAGEN"),
            Map.entry("mp4", "VIDEO"), Map.entry("webm", "VIDEO"),
            Map.entry("pdf", "PRESENTACION"), Map.entry("ppt", "PRESENTACION"), Map.entry("pptx", "PRESENTACION"),
            Map.entry("doc", "DOCUMENTO"), Map.entry("docx", "DOCUMENTO"),
            Map.entry("xls", "DOCUMENTO"), Map.entry("xlsx", "DOCUMENTO"), Map.entry("txt", "DOCUMENTO")
    );

    private final Path directorio;

    public ArchivoCursoService(@Value("${app.upload-dir:uploads}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public ArchivoCursoDTO guardar(Long idCurso, MultipartFile archivo, boolean portada) {
        if (archivo == null || archivo.isEmpty()) throw new SolicitudInvalidaException("Selecciona un archivo para subir");
        if (archivo.getSize() > TAMANO_MAXIMO) throw new SolicitudInvalidaException("El archivo no puede superar los 50 MB");
        String original = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        String nombre = Path.of(original).getFileName().toString();
        int separador = nombre.lastIndexOf('.');
        if (separador <= 0 || separador == nombre.length() - 1)
            throw new SolicitudInvalidaException("El archivo debe tener una extensión válida");
        String extension = nombre.substring(separador + 1).toLowerCase(Locale.ROOT);
        String tipo = TIPOS_PERMITIDOS.get(extension);
        if (tipo == null) throw new SolicitudInvalidaException("Tipo de archivo no permitido");
        if (portada && !"IMAGEN".equals(tipo)) throw new SolicitudInvalidaException("La portada debe ser una imagen PNG, JPG, WEBP o GIF");
        String nombreGuardado = UUID.randomUUID() + "." + extension;
        Path carpeta = directorio.resolve("cursos").resolve(String.valueOf(idCurso)).normalize();
        if (!carpeta.startsWith(directorio)) throw new SolicitudInvalidaException("Ruta de archivo inválida");
        try {
            Files.createDirectories(carpeta);
            Files.copy(archivo.getInputStream(), carpeta.resolve(nombreGuardado));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo del curso", e);
        }
        return new ArchivoCursoDTO("/archivos/cursos/" + idCurso + "/" + nombreGuardado, nombre, tipo);
    }
}
