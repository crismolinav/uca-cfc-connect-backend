package sv.edu.udb.ucacfcconnect.service;

import org.junit.jupiter.api.Test;
import sv.edu.udb.ucacfcconnect.dto.CertificadoCursoDTO;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificadoPdfServiceTest {
    @Test
    void generaPdfConRecursosYNombreDeArchivoSeguro() {
        CertificadoCursoDTO datos = new CertificadoCursoDTO(
                "José Pérez", "Introducción a Tecnología", List.of("María López"),
                LocalDate.of(2026, 10, 22), "UCA-CFC-2026-42");
        CertificadoPdfService servicio = new CertificadoPdfService();

        byte[] pdf = servicio.generar(datos);

        assertTrue(pdf.length > 1_000);
        assertTrue(new String(pdf, 0, 8, StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
        assertEquals("certificado-jose-perez-introduccion-a-tecnologia.pdf", servicio.nombreArchivo(datos));
    }
}
