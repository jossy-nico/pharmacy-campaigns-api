package cl.farmaciasahumada.campannas.service.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OcrService {

    private final ImagenPreprocesamientoService imagenPreprocesamientoService;
    private final String tessdataDir;

    public OcrService(
            ImagenPreprocesamientoService imagenPreprocesamientoService,
            @Value("${app.ocr.tessdata-dir}") String tessdataDir) {

        this.imagenPreprocesamientoService = imagenPreprocesamientoService;

        this.tessdataDir = tessdataDir;
    }

    /*
     * =========================================================
     * EXTRAER TEXTO DE UNA IMAGEN
     *
     * Ahora ejecutamos OCR sobre varias versiones de la misma
     * fotografía para mejorar la posibilidad de reconocer:
     *
     * - marcas
     * - nombres de productos
     * - texto pequeño
     * - texto afectado por reflejos
     * - envases claros u oscuros
     * =========================================================
     */

    public String extraerTexto(
            String rutaImagen) {

        validarRutaImagen(
                rutaImagen);

        validarTessdata();

        File imagen = new File(
                rutaImagen);

        /*
         * =====================================================
         * 1. GENERAR VARIANTES
         * =====================================================
         */

        List<BufferedImage> variantes = imagenPreprocesamientoService
                .preprocesarVariantes(
                        imagen);

        if (variantes == null
                || variantes.isEmpty()) {

            throw new IllegalStateException(
                    "No fue posible generar variantes para OCR.");
        }

        /*
         * LinkedHashSet:
         *
         * - evita repetir líneas idénticas
         * - conserva el orden en que fueron encontradas
         */
        Set<String> lineasEncontradas = new LinkedHashSet<>();

        int variantesProcesadas = 0;

        int variantesConTexto = 0;

        /*
         * =====================================================
         * 2. EJECUTAR OCR SOBRE CADA VARIANTE
         * =====================================================
         */

        for (BufferedImage variante : variantes) {

            if (variante == null) {
                continue;
            }

            variantesProcesadas++;

            try {

                ITesseract tesseract = crearTesseract();

                String texto = tesseract.doOCR(
                        variante);

                if (texto == null
                        || texto.isBlank()) {

                    continue;
                }

                variantesConTexto++;

                agregarLineas(
                        texto,
                        lineasEncontradas);

            } catch (TesseractException e) {

                /*
                 * Si una variante específica falla,
                 * permitimos que las demás sigan siendo
                 * procesadas.
                 */
            }
        }

        /*
         * =====================================================
         * 3. VALIDAR RESULTADO
         * =====================================================
         */

        if (variantesProcesadas == 0) {

            throw new IllegalStateException(
                    "No existen variantes válidas para procesar mediante OCR.");
        }

        if (variantesConTexto == 0) {

            return "";
        }

        /*
         * =====================================================
         * 4. COMBINAR RESULTADOS
         * =====================================================
         */

        return String.join(
                System.lineSeparator(),
                lineasEncontradas);
    }

    /*
     * =========================================================
     * EXTRAER TEXTO DESDE IMAGEN EN MEMORIA
     *
     * Se utiliza actualmente para:
     *
     * SIFT + RANSAC
     * ↓
     * recorte
     * ↓
     * byte[]
     * ↓
     * OCR
     * =========================================================
     */

    public String extraerTexto(
            byte[] imagenBytes) {

        if (imagenBytes == null
                || imagenBytes.length == 0) {

            throw new IllegalArgumentException(
                    "La imagen para OCR está vacía.");
        }

        Path archivoTemporal = null;

        try {

            archivoTemporal = Files.createTempFile(
                    "ocr-recorte-",
                    ".png");

            Files.write(
                    archivoTemporal,
                    imagenBytes);

            /*
             * Reutilizamos todo el flujo OCR.
             *
             * Esto significa que el recorte también será
             * procesado mediante las 4 variantes.
             */
            return extraerTexto(
                    archivoTemporal
                            .toAbsolutePath()
                            .toString());

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No fue posible preparar la imagen temporal para OCR.",
                    e);

        } finally {

            if (archivoTemporal != null) {

                try {

                    Files.deleteIfExists(
                            archivoTemporal);

                } catch (IOException e) {

                    /*
                     * No detenemos el proceso por un problema
                     * al eliminar el archivo temporal.
                     */
                }
            }
        }
    }

    /*
     * =========================================================
     * CONFIGURAR TESSERACT
     * =========================================================
     */

    private ITesseract crearTesseract() {

        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath(
                new File(
                        tessdataDir)
                        .getAbsolutePath());

        /*
         * Español + inglés.
         */
        tesseract.setLanguage(
                "spa+eng");

        /*
         * DPI asumido para mejorar reconocimiento
         * de texto pequeño.
         */
        tesseract.setVariable(
                "user_defined_dpi",
                "300");

        /*
         * Sparse Text.
         *
         * Adecuado para marcas y textos distribuidos
         * en diferentes partes de los envases.
         */
        tesseract.setPageSegMode(
                11);

        return tesseract;
    }

    /*
     * =========================================================
     * AGREGAR LÍNEAS OCR SIN DUPLICADOS EXACTOS
     * =========================================================
     */

    private void agregarLineas(
            String texto,
            Set<String> lineasEncontradas) {

        String textoNormalizado = texto.replace(
                "\r\n",
                "\n")
                .replace(
                        '\r',
                        '\n');

        String[] lineas = textoNormalizado.split(
                "\n");

        for (String linea : lineas) {

            if (linea == null) {
                continue;
            }

            String limpia = linea.trim();

            if (limpia.isBlank()) {
                continue;
            }

            lineasEncontradas.add(
                    limpia);
        }
    }

    /*
     * =========================================================
     * VALIDAR IMAGEN
     * =========================================================
     */

    private void validarRutaImagen(
            String rutaImagen) {

        if (rutaImagen == null
                || rutaImagen.isBlank()) {

            throw new IllegalArgumentException(
                    "La ruta de la imagen es obligatoria.");
        }

        File imagen = new File(
                rutaImagen);

        if (!imagen.exists()) {

            throw new IllegalArgumentException(
                    "La imagen no existe en la ruta indicada.");
        }

        if (!imagen.isFile()) {

            throw new IllegalArgumentException(
                    "La ruta indicada no corresponde a un archivo.");
        }
    }

    /*
     * =========================================================
     * VALIDAR TESSDATA
     * =========================================================
     */

    private void validarTessdata() {

        if (tessdataDir == null
                || tessdataDir.isBlank()) {

            throw new IllegalStateException(
                    "La ruta de tessdata no está configurada.");
        }

        File directorio = new File(
                tessdataDir);

        if (!directorio.exists()
                || !directorio.isDirectory()) {

            throw new IllegalStateException(
                    "No existe el directorio tessdata configurado: "
                            + directorio.getAbsolutePath());
        }

        File idiomaEspanol = new File(
                directorio,
                "spa.traineddata");

        File idiomaIngles = new File(
                directorio,
                "eng.traineddata");

        if (!idiomaEspanol.exists()) {

            throw new IllegalStateException(
                    "No existe spa.traineddata en tessdata.");
        }

        if (!idiomaIngles.exists()) {

            throw new IllegalStateException(
                    "No existe eng.traineddata en tessdata.");
        }
    }
}