package cl.farmaciasahumada.campannas.service.vision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
public class VisionExhibidorService {

    /*
     * =========================================================
     * DETECTAR REGIONES CANDIDATAS
     * =========================================================
     */

    public List<RegionDetectada> detectarRegionesCandidatas(
            String rutaImagen) {

        if (rutaImagen == null
                || rutaImagen.isBlank()) {

            throw new IllegalArgumentException(
                    "La ruta de la imagen es obligatoria.");
        }

        Mat original = Imgcodecs.imread(
                rutaImagen);

        if (original.empty()) {

            throw new IllegalArgumentException(
                    "No fue posible leer la imagen indicada.");
        }

        Mat gris = new Mat();

        Mat suavizada = new Mat();

        Mat bordes = new Mat();

        Mat morfologia = new Mat();

        Mat jerarquia = new Mat();

        try {

            /*
             * =====================================================
             * 1. ESCALA DE GRISES
             * =====================================================
             */

            Imgproc.cvtColor(
                    original,
                    gris,
                    Imgproc.COLOR_BGR2GRAY);

            /*
             * =====================================================
             * 2. SUAVIZADO
             *
             * Reducimos el kernel respecto de la primera versión
             * para evitar unir demasiados elementos de la farmacia.
             * =====================================================
             */

            Imgproc.GaussianBlur(
                    gris,
                    suavizada,
                    new Size(
                            3,
                            3),
                    0);

            /*
             * =====================================================
             * 3. BORDES
             * =====================================================
             */

            Imgproc.Canny(
                    suavizada,
                    bordes,
                    40,
                    120);

            /*
             * =====================================================
             * 4. MORFOLOGÍA
             *
             * Kernel más pequeño para no convertir todo el
             * escenario en un único contorno gigante.
             * =====================================================
             */

            Mat kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    new Size(
                            3,
                            3));

            Imgproc.morphologyEx(
                    bordes,
                    morfologia,
                    Imgproc.MORPH_CLOSE,
                    kernel);

            kernel.release();

            /*
             * =====================================================
             * 5. CONTORNOS
             *
             * RETR_LIST:
             * buscamos también contornos internos.
             *
             * Esto es importante porque el acrílico es transparente
             * y muchas veces su estructura queda dentro de otros
             * contornos visuales.
             * =====================================================
             */

            List<MatOfPoint> contornos = new ArrayList<>();

            Imgproc.findContours(
                    morfologia,
                    contornos,
                    jerarquia,
                    Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_SIMPLE);

            double areaImagen = original.width()
                    * (double) original.height();

            List<RegionDetectada> candidatas = new ArrayList<>();

            for (MatOfPoint contorno : contornos) {

                try {

                    Rect rectangulo = Imgproc.boundingRect(
                            contorno);

                    if (rectangulo.width <= 0
                            || rectangulo.height <= 0) {

                        continue;
                    }

                    double areaRectangulo = rectangulo.width
                            * (double) rectangulo.height;

                    double proporcionArea = areaRectangulo
                            / areaImagen;

                    /*
                     * Ahora aceptamos regiones más pequeñas.
                     *
                     * 3% mínimo:
                     * permite detectar partes significativas.
                     *
                     * 85% máximo:
                     * evita seleccionar prácticamente
                     * toda la fotografía.
                     */
                    if (proporcionArea < 0.03
                            || proporcionArea > 0.85) {

                        continue;
                    }

                    double relacionAspecto = rectangulo.width
                            / (double) rectangulo.height;

                    /*
                     * Rango amplio porque tendremos:
                     * - acrílicos
                     * - peinetas
                     * - bajo mesón
                     * - otros exhibidores.
                     */
                    if (relacionAspecto < 0.20
                            || relacionAspecto > 3.00) {

                        continue;
                    }

                    double areaContorno = Imgproc.contourArea(
                            contorno);

                    double rectangularidad = areaRectangulo == 0
                            ? 0
                            : areaContorno
                                    / areaRectangulo;

                    /*
                     * Bajamos la exigencia porque un acrílico
                     * transparente genera contornos fragmentados.
                     */
                    if (rectangularidad < 0.05) {

                        continue;
                    }

                    boolean tocaBorde = tocaBordeImagen(
                            rectangulo,
                            original.width(),
                            original.height());

                    double puntaje = calcularPuntaje(
                            proporcionArea,
                            rectangularidad,
                            relacionAspecto,
                            tocaBorde);

                    candidatas.add(
                            new RegionDetectada(
                                    rectangulo.x,
                                    rectangulo.y,
                                    rectangulo.width,
                                    rectangulo.height,
                                    proporcionArea,
                                    rectangularidad,
                                    relacionAspecto,
                                    tocaBorde,
                                    puntaje));

                } finally {

                    contorno.release();
                }
            }

            /*
             * Ordenamos de mejor a peor.
             */
            candidatas.sort(
                    Comparator.comparingDouble(
                            RegionDetectada::puntaje)
                            .reversed());

            /*
             * Para la prueba no necesitamos cientos.
             * Nos quedamos con las 10 mejores.
             */
            if (candidatas.size() > 10) {

                return new ArrayList<>(
                        candidatas.subList(
                                0,
                                10));
            }

            return candidatas;

        } finally {

            original.release();
            gris.release();
            suavizada.release();
            bordes.release();
            morfologia.release();
            jerarquia.release();
        }
    }

    /*
     * =========================================================
     * DETECTAR MEJOR REGIÓN
     * =========================================================
     */

    public RegionDetectada detectarRegionPrincipal(
            String rutaImagen) {

        List<RegionDetectada> candidatas = detectarRegionesCandidatas(
                rutaImagen);

        if (candidatas.isEmpty()) {

            throw new IllegalStateException(
                    "No fue posible detectar una región candidata del exhibidor.");
        }

        return candidatas.get(0);
    }

    /*
     * =========================================================
     * SABER SI LA REGIÓN TOCA LOS BORDES
     * =========================================================
     */

    private boolean tocaBordeImagen(
            Rect rectangulo,
            int anchoImagen,
            int altoImagen) {

        int margen = 5;

        return rectangulo.x <= margen
                || rectangulo.y <= margen
                || rectangulo.x
                        + rectangulo.width >= anchoImagen - margen
                || rectangulo.y
                        + rectangulo.height >= altoImagen - margen;
    }

    /*
     * =========================================================
     * PUNTAJE
     * =========================================================
     */
    /*
     * =========================================================
     * GENERAR IMAGEN DE DIAGNÓSTICO
     *
     * Dibuja sobre la fotografía las regiones candidatas
     * detectadas por OpenCV.
     *
     * Esto es únicamente una herramienta de diagnóstico.
     * No modifica la fotografía original.
     * =========================================================
     */

    public byte[] generarImagenDiagnostico(
            String rutaImagen) {

        if (rutaImagen == null
                || rutaImagen.isBlank()) {

            throw new IllegalArgumentException(
                    "La ruta de la imagen es obligatoria.");
        }

        Mat imagen = Imgcodecs.imread(
                rutaImagen);

        if (imagen.empty()) {

            throw new IllegalArgumentException(
                    "No fue posible leer la imagen indicada.");
        }

        MatOfByte buffer = new MatOfByte();

        try {

            List<RegionDetectada> candidatas = detectarRegionesCandidatas(
                    rutaImagen);

            int posicion = 1;

            for (RegionDetectada region : candidatas) {

                Point esquinaSuperior = new Point(
                        region.x(),
                        region.y());

                Point esquinaInferior = new Point(
                        region.x() + region.ancho(),
                        region.y() + region.alto());

                /*
                 * Dibujar rectángulo de la región candidata.
                 */
                Imgproc.rectangle(
                        imagen,
                        esquinaSuperior,
                        esquinaInferior,
                        new Scalar(
                                0,
                                0,
                                255),
                        3);

                /*
                 * Etiqueta para identificar la candidata.
                 */
                String etiqueta = "#"
                        + posicion
                        + " P:"
                        + String.format(
                                java.util.Locale.US,
                                "%.3f",
                                region.puntaje());

                int textoY = Math.max(
                        region.y() - 10,
                        25);

                Imgproc.putText(
                        imagen,
                        etiqueta,
                        new Point(
                                region.x(),
                                textoY),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        new Scalar(
                                0,
                                0,
                                255),
                        2);

                posicion++;
            }

            boolean convertido = Imgcodecs.imencode(
                    ".png",
                    imagen,
                    buffer);

            if (!convertido) {

                throw new IllegalStateException(
                        "No fue posible generar la imagen de diagnóstico.");
            }

            return buffer.toArray();

        } finally {

            imagen.release();
            buffer.release();
        }
    }

    private double calcularPuntaje(
            double proporcionArea,
            double rectangularidad,
            double relacionAspecto,
            boolean tocaBorde) {

        /*
         * Preferimos regiones:
         *
         * - suficientemente grandes
         * - relativamente rectangulares
         * - con proporción razonable
         * - que no sean simplemente toda la fotografía
         */

        double puntajeArea = proporcionArea
                * 0.45;

        double puntajeRectangularidad = rectangularidad
                * 0.35;

        /*
         * Favorecemos formas cercanas a vertical/cuadrada,
         * pero sin bloquear otras configuraciones.
         */
        double diferenciaAspecto = Math.abs(
                1.0
                        - relacionAspecto);

        double puntajeAspecto = Math.max(
                0,
                1.0
                        - diferenciaAspecto)
                * 0.20;

        double penalizacionBorde = tocaBorde
                ? 0.10
                : 0.0;

        return puntajeArea
                + puntajeRectangularidad
                + puntajeAspecto
                - penalizacionBorde;
    }

    /*
     * =========================================================
     * RESULTADO
     * =========================================================
     */

    public record RegionDetectada(
            int x,
            int y,
            int ancho,
            int alto,
            double proporcionArea,
            double rectangularidad,
            double relacionAspecto,
            boolean tocaBorde,
            double puntaje) {
    }
}