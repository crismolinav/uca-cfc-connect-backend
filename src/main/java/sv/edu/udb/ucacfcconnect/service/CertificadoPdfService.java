package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import sv.edu.udb.ucacfcconnect.dto.CertificadoCursoDTO;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.DeflaterOutputStream;

@Service
public class CertificadoPdfService {
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "SV"));
    private static final float ANCHO = 842;
    private static final float ALTO = 595;

    public byte[] generar(CertificadoCursoDTO certificado) {
        try {
            ByteArrayOutputStream contenido = new ByteArrayOutputStream();
            rectangulo(contenido, 0, 0, ANCHO, ALTO, 1, 1, 1);
            esquinasAzules(contenido);
            linea(contenido, "0.078 0.337 0.655 RG 1.0 w 0 435 m 41 483 87 476 126 507 c 148 525 159 538 173 545 c S");
            linea(contenido, "0.078 0.337 0.655 RG 1.0 w 669 0 m 693 27 724 41 765 48 c 804 55 822 81 842 111 c S");

            centrar(contenido, "F2", 9, "UNIVERSIDAD CENTROAMERICANA · FORMACIÓN CONTINUA", 552, 0.055f, 0.157f, 0.325f);
            centrar(contenido, "F4", 43, "CERTIFICADO", 492, 0.055f, 0.157f, 0.325f);
            centrar(contenido, "F4", 18, "DE FINALIZACIÓN", 458, 0.055f, 0.157f, 0.325f);
            linea(contenido, "0.894 0.690 0.263 RG 1.1 w 314 440 m 528 440 l S");
            textoImagen(contenido, "UCA", 740, 452, 65, 89);

            centrar(contenido, "F2", 9, "OTORGADO A", 401, 0.055f, 0.157f, 0.325f);
            List<String> nombre = envolver(certificado.estudiante(), 24, 650);
            float tamanoNombre = nombre.size() > 2 ? 19 : nombre.size() == 2 ? 22 : 26;
            float yNombre = 360;
            for (String fila : nombre) {
                centrar(contenido, "F4", tamanoNombre, fila, yNombre, 0.055f, 0.157f, 0.325f);
                yNombre -= tamanoNombre + 5;
            }
            float yRegla = yNombre - 1;
            linea(contenido, String.format(Locale.ROOT, "0.055 0.157 0.325 RG 0.8 w 174 %.2f m 668 %.2f l S", yRegla, yRegla));

            centrar(contenido, "F1", 12, "Por haber completado satisfactoriamente el curso", yRegla - 31,
                    0.055f, 0.157f, 0.325f);
            List<String> curso = envolver(certificado.curso(), 18, 650);
            float yCurso = yRegla - 60;
            for (String fila : curso) {
                centrar(contenido, "F2", 18, fila, yCurso, 0.055f, 0.157f, 0.325f);
                yCurso -= 24;
            }

            textoCentrado(contenido, "F2", 8, 280, 221, "FECHA DE FINALIZACIÓN", 0.357f, 0.451f, 0.580f);
            textoCentrado(contenido, "F2", 11, 280, 202, certificado.fechaFinalizacion().format(FECHA), 0.055f, 0.157f, 0.325f);
            textoCentrado(contenido, "F2", 8, 562, 221, "CÓDIGO DE VERIFICACIÓN", 0.357f, 0.451f, 0.580f);
            textoCentrado(contenido, "F2", 11, 562, 202, certificado.codigo(), 0.055f, 0.157f, 0.325f);

            linea(contenido, "0.055 0.157 0.325 RG 0.8 w 117 112 m 331 112 l S");
            linea(contenido, "0.055 0.157 0.325 RG 0.8 w 511 112 m 725 112 l S");
            textoImagen(contenido, "MEDALLA", 386, 97, 70, 92);
            textoImagen(contenido, "FIRMA", 155, 115, 138, 68);
            textoImagen(contenido, "SELLO", 579, 116, 78, 78);
            String docentes = certificado.docentes() == null || certificado.docentes().isEmpty()
                    ? "Equipo académico" : String.join(", ", certificado.docentes());
            textoCentrado(contenido, "F2", 8, 224, 94, docentes, 0.055f, 0.157f, 0.325f);
            textoCentrado(contenido, "F1", 8, 224, 80, "DOCENTE", 0.357f, 0.451f, 0.580f);
            textoCentrado(contenido, "F2", 8, 618, 94, "Formación continua", 0.055f, 0.157f, 0.325f);
            textoCentrado(contenido, "F1", 8, 618, 80, "COORDINACIÓN ACADÉMICA", 0.357f, 0.451f, 0.580f);
            textoCentrado(contenido, "F1", 7, 421, 23, "UNIVERSIDAD CENTROAMERICANA · FORMACIÓN CONTINUA", 0.357f, 0.451f, 0.580f);

            byte[] stream = contenido.toByteArray();
            PdfImage logo = cargarImagen("/static/imagenes/uca.png");
            PdfImage medalla = cargarImagen("/static/imagenes/medalla-certificado.png");
            PdfImage sello = cargarImagen("/static/imagenes/sello-desde-1965.png");
            PdfImage firma = cargarImagen("/static/imagenes/firma-certificado.png");
            List<byte[]> objetos = List.of(
                    ascii("<< /Type /Catalog /Pages 2 0 R >>"),
                    ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                    ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + ANCHO + " " + ALTO
                            + "] /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R /F4 12 0 R >> "
                            + "/XObject << /UCA 8 0 R /MEDALLA 9 0 R /SELLO 10 0 R /FIRMA 11 0 R >> >> /Contents 7 0 R >>"),
                    ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"),
                    ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"),
                    ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Bold /Encoding /WinAnsiEncoding >>"),
                    streamObjeto(stream),
                    imagenObjeto(logo),
                    imagenObjeto(medalla),
                    imagenObjeto(sello),
                    imagenObjeto(firma),
                    ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Roman /Encoding /WinAnsiEncoding >>")
            );
            return documento(objetos);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el certificado PDF", e);
        }
    }

    public String nombreArchivo(CertificadoCursoDTO certificado) {
        return "certificado-" + slug(certificado.estudiante()) + "-" + slug(certificado.curso()) + ".pdf";
    }

    private String slug(String valor) {
        String nombre = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "").toLowerCase(Locale.ROOT);
        return nombre.isBlank() ? "curso" : nombre;
    }

    private byte[] documento(List<byte[]> objetos) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(ascii("%PDF-1.4\n"));
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            posiciones.add(pdf.size());
            pdf.write(ascii((i + 1) + " 0 obj\n"));
            pdf.write(objetos.get(i));
            pdf.write(ascii("\nendobj\n"));
        }
        int xref = pdf.size();
        pdf.write(ascii("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n"));
        for (Integer posicion : posiciones) pdf.write(ascii(String.format(Locale.ROOT, "%010d 00000 n \n", posicion)));
        pdf.write(ascii("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"));
        return pdf.toByteArray();
    }

    private byte[] streamObjeto(byte[] stream) throws IOException {
        ByteArrayOutputStream objeto = new ByteArrayOutputStream();
        objeto.write(ascii("<< /Length " + stream.length + " >>\nstream\n"));
        objeto.write(stream);
        objeto.write(ascii("\nendstream"));
        return objeto.toByteArray();
    }

    private void rectangulo(ByteArrayOutputStream salida, float x, float y, float ancho, float alto,
                            float rojo, float verde, float azul) throws IOException {
        linea(salida, String.format(Locale.ROOT, "%.3f %.3f %.3f rg %.2f %.2f %.2f %.2f re f",
                rojo, verde, azul, x, y, ancho, alto));
    }

    private void circulo(ByteArrayOutputStream salida, float x, float y, float radio,
                         float rojo, float verde, float azul) throws IOException {
        float curva = radio * .55228475f;
        linea(salida, String.format(Locale.ROOT,
                "%.3f %.3f %.3f rg %.2f %.2f m %.2f %.2f %.2f %.2f %.2f %.2f c %.2f %.2f %.2f %.2f %.2f %.2f c %.2f %.2f %.2f %.2f %.2f %.2f c %.2f %.2f %.2f %.2f %.2f %.2f c f",
                rojo, verde, azul, x + radio, y, x + radio, y + curva, x + curva, y + radio, x, y + radio,
                x - curva, y + radio, x - radio, y + curva, x - radio, y, x - radio, y - curva,
                x - curva, y - radio, x, y - radio, x + curva, y - radio, x + radio, y - curva, x + radio, y));
    }

    private void textoCentrado(ByteArrayOutputStream salida, String fuente, float tamano, float centroX,
                               float y, String valor, float rojo, float verde, float azul) throws IOException {
        String texto = compatible(valor);
        texto(salida, fuente, tamano, centroX - anchoFuente(texto, tamano, fuente) / 2, y, texto, rojo, verde, azul);
    }

    private void esquinasAzules(ByteArrayOutputStream salida) throws IOException {
        linea(salida, "0.055 0.157 0.325 rg 0 595 m 197 595 l 177 570 153 547 117 536 c 76 525 38 485 0 443 c h f");
        linea(salida, "0.055 0.157 0.325 rg 842 0 m 645 0 l 665 25 689 48 725 59 c 766 70 804 110 842 152 c h f");
        linea(salida, "0.078 0.337 0.655 rg 0 595 m 132 595 l 111 570 91 556 64 548 c 46 542 19 521 0 498 c h f");
        linea(salida, "0.078 0.337 0.655 rg 842 0 m 710 0 l 731 25 751 39 778 47 c 796 53 823 74 842 98 c h f");
    }

    private void textoImagen(ByteArrayOutputStream salida, String nombre, float x, float y,
                             float ancho, float alto) throws IOException {
        linea(salida, String.format(Locale.ROOT, "q %.2f 0 0 %.2f %.2f %.2f cm /%s Do Q",
                ancho, alto, x, y, nombre));
    }

    private PdfImage cargarImagen(String ruta) throws IOException {
        return cargarImagen(ruta, java.awt.Color.WHITE);
    }

    private PdfImage cargarImagen(String ruta, java.awt.Color fondo) throws IOException {
        try (var archivo = getClass().getResourceAsStream(ruta)) {
            if (archivo == null) throw new IOException("No se encontró la imagen del certificado");
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) throw new IOException("El logo de la UCA no es una imagen válida");
            int ancho = 450;
            int alto = Math.max(1, (int) Math.round(original.getHeight() * (ancho / (double) original.getWidth())));
            BufferedImage reducida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D grafico = reducida.createGraphics();
            grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            grafico.drawImage(original, 0, 0, ancho, alto, fondo, null);
            grafico.dispose();
            ByteArrayOutputStream pixeles = new ByteArrayOutputStream(ancho * alto * 3);
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    int rgb = reducida.getRGB(x, y);
                    pixeles.write((rgb >> 16) & 0xff);
                    pixeles.write((rgb >> 8) & 0xff);
                    pixeles.write(rgb & 0xff);
                }
            }
            ByteArrayOutputStream comprimidos = new ByteArrayOutputStream();
            try (DeflaterOutputStream compresor = new DeflaterOutputStream(comprimidos)) {
                pixeles.writeTo(compresor);
            }
            return new PdfImage(ancho, alto, comprimidos.toByteArray());
        }
    }

    private byte[] imagenObjeto(PdfImage imagen) throws IOException {
        ByteArrayOutputStream objeto = new ByteArrayOutputStream();
        objeto.write(ascii("<< /Type /XObject /Subtype /Image /Width " + imagen.ancho()
                + " /Height " + imagen.alto()
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
                + imagen.datos().length + " >>\nstream\n"));
        objeto.write(imagen.datos());
        objeto.write(ascii("\nendstream"));
        return objeto.toByteArray();
    }

    private void centrar(ByteArrayOutputStream salida, String fuente, float tamano, String valor,
                         float y, float rojo, float verde, float azul) throws IOException {
        String texto = compatible(valor);
        float x = (ANCHO - anchoFuente(texto, tamano, fuente)) / 2;
        texto(salida, fuente, tamano, x, y, texto, rojo, verde, azul);
    }

    private void texto(ByteArrayOutputStream salida, String fuente, float tamano, float x, float y,
                       String valor, float rojo, float verde, float azul) throws IOException {
        linea(salida, String.format(Locale.ROOT, "BT /%s %.1f Tf %.3f %.3f %.3f rg %.2f %.2f Td ",
                fuente, tamano, rojo, verde, azul, x, y));
        salida.write(literalPdf(compatible(valor)));
        linea(salida, " Tj ET");
    }

    private void linea(ByteArrayOutputStream salida, String comando) throws IOException {
        salida.write(ascii(comando + "\n"));
    }

    private List<String> envolver(String valor, float tamano, float anchoMaximo) {
        List<String> filas = new ArrayList<>();
        StringBuilder fila = new StringBuilder();
        for (String palabra : compatible(valor).trim().split("\\s+")) {
            String candidata = fila.isEmpty() ? palabra : fila + " " + palabra;
            if (!fila.isEmpty() && anchoTexto(candidata, tamano) > anchoMaximo) {
                filas.add(fila.toString());
                fila.setLength(0);
                fila.append(palabra);
            } else {
                if (!fila.isEmpty()) fila.append(' ');
                fila.append(palabra);
            }
        }
        if (!fila.isEmpty()) filas.add(fila.toString());
        return filas;
    }

    private float anchoTexto(String texto, float tamano) {
        return anchoFuente(texto, tamano, "F2");
    }

    private float anchoFuente(String texto, float tamano, String fuente) {
        boolean times = "F3".equals(fuente) || "F4".equals(fuente);
        boolean negrita = "F2".equals(fuente) || "F3".equals(fuente);
        String base = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        int unidades = 0;
        for (char letra : base.toCharArray()) {
            unidades += anchoGlifo(letra, times, negrita);
        }
        return unidades * tamano / 1000f;
    }

    private int anchoGlifo(char letra, boolean times, boolean negrita) {
        if (letra == ' ') return times ? 250 : 278;
        if (Character.isDigit(letra)) return times ? 500 : 556;
        if (Character.isUpperCase(letra)) {
            String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            String medidas = times
                    ? "722 667 667 722 611 556 722 722 333 389 722 611 889 722 722 556 722 667 556 611 722 722 944 722 722 611"
                    : "667 667 722 722 667 611 778 722 278 500 667 556 833 722 778 667 778 722 667 611 722 667 944 667 611 611";
            int posicion = letras.indexOf(letra);
            return posicion < 0 ? 600 : medida(medidas, posicion);
        }
        if (Character.isLowerCase(letra)) {
            String letras = "abcdefghijklmnopqrstuvwxyz";
            String medidas = times
                    ? "444 500 444 500 444 333 500 500 278 278 500 278 778 500 500 500 500 333 389 278 500 500 722 500 500 444"
                    : "556 556 500 556 556 278 556 556 222 222 500 222 833 556 556 556 556 333 500 278 556 500 722 500 500 500";
            int posicion = letras.indexOf(letra);
            return posicion < 0 ? 500 : medida(medidas, posicion);
        }
        return switch (letra) {
            case '.', ',' -> times ? 250 : 278;
            case ':', ';' -> times ? 333 : 278;
            case '!', '|' -> times ? 333 : 278;
            case '\'', '`' -> times ? 333 : 222;
            case '-', '–', '—' -> times ? 333 : 333;
            case '(', ')' -> times ? 333 : 333;
            default -> negrita ? 600 : 500;
        };
    }

    private int medida(String medidas, int posicion) {
        String[] valores = medidas.split(" ");
        return Integer.parseInt(valores[posicion]);
    }

    private byte[] literalPdf(String valor) throws IOException {
        ByteArrayOutputStream literal = new ByteArrayOutputStream();
        literal.write('(');
        for (byte letra : valor.getBytes(WIN_ANSI)) {
            int caracter = letra & 0xff;
            if (caracter == '(' || caracter == ')' || caracter == '\\') literal.write('\\');
            if (caracter < 32 || caracter == 127) literal.write(' ');
            else literal.write(caracter);
        }
        literal.write(')');
        return literal.toByteArray();
    }

    private String compatible(String valor) {
        if (valor == null) return "";
        String limpio = valor.replace('\u2013', '-').replace('\u2014', '-')
                .replace('\u2019', '\'').replace('\u201c', '"').replace('\u201d', '"').replace('\u00a0', ' ');
        return new String(limpio.getBytes(WIN_ANSI), WIN_ANSI);
    }

    private byte[] ascii(String texto) { return texto.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1); }

    private record PdfImage(int ancho, int alto, byte[] datos) {}
}
