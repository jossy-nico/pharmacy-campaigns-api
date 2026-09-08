package cl.farmaciasahumada.campannas.service.vision;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
public class AlineacionReferenciaService {

    /*
     * =========================================================
     * CONFIGURACIÓN
     * =========================================================
     */

    private static final double RATIO_COINCIDENCIA = 0.75;

    private static final int MIN_COINCIDENCIAS_VALIDAS = 12;

    private static final int MIN_COINCIDENCIAS_GEOMETRICAS = 8;

    private static final double MIN_PORCENTAJE_INLIERS = 35.0;

    private static final double UMBRAL_RANSAC_PIXELES = 5.0;
    /*
     * =========================================================
     * CONFIGURACIÓN PROVISIONAL PARA REGIÓN DE BÚSQUEDA
     *
     * Estos valores se calibrarán posteriormente con más
     * fotografías reales de zonales y farmacias.
     * =========================================================
     */

    private static final int MIN_INLIERS_REGION = 6;

    private static final double MARGEN_HORIZONTAL_FACTOR = 0.75;

    private static final double MARGEN_VERTICAL_FACTOR = 0.15;

    private static final double MARGEN_HORIZONTAL_IMAGEN = 0.12;

    private static final double MARGEN_VERTICAL_IMAGEN = 0.08;

    /*
     * =========================================================
     * ANALIZAR ALINEACIÓN
     * =========================================================
     */

    public ResultadoAlineacion analizar(
            String rutaReferencia,
            String rutaEvidencia) {

        validarRuta(
                rutaReferencia,
                "referencia oficial");

        validarRuta(
                rutaEvidencia,
                "evidencia");

        Mat referencia = Imgcodecs.imread(
                rutaReferencia);

        Mat evidencia = Imgcodecs.imread(
                rutaEvidencia);

        if (referencia.empty()) {

            throw new IllegalArgumentException(
                    "No fue posible leer la referencia oficial.");
        }

        if (evidencia.empty()) {

            referencia.release();

            throw new IllegalArgumentException(
                    "No fue posible leer la evidencia.");
        }

        Mat grisReferencia = new Mat();

        Mat grisEvidencia = new Mat();

        Mat descriptoresReferencia = new Mat();

        Mat descriptoresEvidencia = new Mat();

        Mat mascaraVacia = new Mat();

        Mat mascaraInliers = new Mat();

        Mat homografia = null;

        MatOfKeyPoint puntosReferencia = new MatOfKeyPoint();

        MatOfKeyPoint puntosEvidencia = new MatOfKeyPoint();

        MatOfPoint2f puntosReferenciaValidos = new MatOfPoint2f();

        MatOfPoint2f puntosEvidenciaValidos = new MatOfPoint2f();

        List<MatOfDMatch> coincidenciasKnn = new ArrayList<>();

        try {

            /*
             * =====================================================
             * 1. ESCALA DE GRISES
             * =====================================================
             */

            Imgproc.cvtColor(
                    referencia,
                    grisReferencia,
                    Imgproc.COLOR_BGR2GRAY);

            Imgproc.cvtColor(
                    evidencia,
                    grisEvidencia,
                    Imgproc.COLOR_BGR2GRAY);

            /*
             * =====================================================
             * 2. MEJORA DE CONTRASTE
             * =====================================================
             */

            Imgproc.equalizeHist(
                    grisReferencia,
                    grisReferencia);

            Imgproc.equalizeHist(
                    grisEvidencia,
                    grisEvidencia);

            /*
             * =====================================================
             * 3. SIFT
             *
             * Más robusto que ORB frente a:
             * - cambios de escala
             * - perspectiva
             * - rotación
             * - diferencias de iluminación
             * =====================================================
             */

            SIFT sift = SIFT.create();

            sift.detectAndCompute(
                    grisReferencia,
                    mascaraVacia,
                    puntosReferencia,
                    descriptoresReferencia);

            sift.detectAndCompute(
                    grisEvidencia,
                    mascaraVacia,
                    puntosEvidencia,
                    descriptoresEvidencia);

            KeyPoint[] keypointsReferencia = puntosReferencia.toArray();

            KeyPoint[] keypointsEvidencia = puntosEvidencia.toArray();

            int cantidadPuntosReferencia = keypointsReferencia.length;

            int cantidadPuntosEvidencia = keypointsEvidencia.length;

            if (descriptoresReferencia.empty()
                    || descriptoresEvidencia.empty()) {

                return construirResultadoSinHomografia(
                        cantidadPuntosReferencia,
                        cantidadPuntosEvidencia,
                        0,
                        0);
            }

            /*
             * =====================================================
             * 4. MATCHING
             *
             * SIFT genera descriptores de punto flotante,
             * por eso usamos NORM_L2.
             * =====================================================
             */

            BFMatcher matcher = BFMatcher.create(
                    Core.NORM_L2,
                    false);

            matcher.knnMatch(
                    descriptoresReferencia,
                    descriptoresEvidencia,
                    coincidenciasKnn,
                    2);

            int coincidenciasTotales = coincidenciasKnn.size();

            List<DMatch> coincidenciasValidas = new ArrayList<>();

            /*
             * =====================================================
             * 5. LOWE RATIO TEST
             * =====================================================
             */

            for (MatOfDMatch grupo : coincidenciasKnn) {

                DMatch[] matches = grupo.toArray();

                if (matches.length < 2) {
                    continue;
                }

                DMatch mejor = matches[0];

                DMatch segunda = matches[1];

                if (mejor.distance < RATIO_COINCIDENCIA
                        * segunda.distance) {

                    coincidenciasValidas.add(
                            mejor);
                }
            }

            int cantidadCoincidenciasValidas = coincidenciasValidas.size();

            double porcentajeCoincidencia = coincidenciasTotales == 0
                    ? 0.0
                    : cantidadCoincidenciasValidas
                            * 100.0
                            / coincidenciasTotales;

            if (cantidadCoincidenciasValidas < 4) {

                return new ResultadoAlineacion(
                        cantidadPuntosReferencia,
                        cantidadPuntosEvidencia,
                        coincidenciasTotales,
                        cantidadCoincidenciasValidas,
                        porcentajeCoincidencia,
                        0,
                        0.0,
                        false,
                        false);
            }

            /*
             * =====================================================
             * 6. PARES DE PUNTOS
             * =====================================================
             */

            List<Point> listaReferencia = new ArrayList<>();

            List<Point> listaEvidencia = new ArrayList<>();

            for (DMatch match : coincidenciasValidas) {

                if (match.queryIdx < 0
                        || match.queryIdx >= keypointsReferencia.length) {

                    continue;
                }

                if (match.trainIdx < 0
                        || match.trainIdx >= keypointsEvidencia.length) {

                    continue;
                }

                listaReferencia.add(
                        keypointsReferencia[match.queryIdx].pt);

                listaEvidencia.add(
                        keypointsEvidencia[match.trainIdx].pt);
            }

            puntosReferenciaValidos.fromList(
                    listaReferencia);

            puntosEvidenciaValidos.fromList(
                    listaEvidencia);

            if (listaReferencia.size() < 4
                    || listaEvidencia.size() < 4) {

                return new ResultadoAlineacion(
                        cantidadPuntosReferencia,
                        cantidadPuntosEvidencia,
                        coincidenciasTotales,
                        cantidadCoincidenciasValidas,
                        porcentajeCoincidencia,
                        0,
                        0.0,
                        false,
                        false);
            }

            /*
             * =====================================================
             * 7. HOMOGRAFÍA + RANSAC
             * =====================================================
             */

            homografia = Calib3d.findHomography(
                    puntosReferenciaValidos,
                    puntosEvidenciaValidos,
                    Calib3d.RANSAC,
                    UMBRAL_RANSAC_PIXELES,
                    mascaraInliers,
                    2000,
                    0.995);

            boolean homografiaCalculada = homografia != null
                    && !homografia.empty();

            int coincidenciasGeometricas = 0;

            if (homografiaCalculada
                    && !mascaraInliers.empty()) {

                coincidenciasGeometricas = Core.countNonZero(
                        mascaraInliers);
            }

            double porcentajeInliers = cantidadCoincidenciasValidas == 0
                    ? 0.0
                    : coincidenciasGeometricas
                            * 100.0
                            / cantidadCoincidenciasValidas;

            /*
             * =====================================================
             * 8. RESULTADO
             * =====================================================
             */

            boolean alineacionPosible = homografiaCalculada
                    && cantidadCoincidenciasValidas >= MIN_COINCIDENCIAS_VALIDAS
                    && coincidenciasGeometricas >= MIN_COINCIDENCIAS_GEOMETRICAS
                    && porcentajeInliers >= MIN_PORCENTAJE_INLIERS;

            return new ResultadoAlineacion(
                    cantidadPuntosReferencia,
                    cantidadPuntosEvidencia,
                    coincidenciasTotales,
                    cantidadCoincidenciasValidas,
                    porcentajeCoincidencia,
                    coincidenciasGeometricas,
                    porcentajeInliers,
                    homografiaCalculada,
                    alineacionPosible);

        } finally {

            for (MatOfDMatch grupo : coincidenciasKnn) {
                grupo.release();
            }

            referencia.release();
            evidencia.release();

            grisReferencia.release();
            grisEvidencia.release();

            descriptoresReferencia.release();
            descriptoresEvidencia.release();

            mascaraVacia.release();
            mascaraInliers.release();

            if (homografia != null) {
                homografia.release();
            }

            puntosReferencia.release();
            puntosEvidencia.release();

            puntosReferenciaValidos.release();
            puntosEvidenciaValidos.release();
        }
    }

    /*
     * =========================================================
     * RESULTADO SIN HOMOGRAFÍA
     * =========================================================
     */
    /*
     * =========================================================
     * GENERAR DIAGNÓSTICO VISUAL SIFT + RANSAC
     *
     * Genera una imagen compuesta:
     *
     * REFERENCIA_OFICIAL | EVIDENCIA
     *
     * Las líneas muestran únicamente las coincidencias que
     * RANSAC considera geométricamente coherentes.
     * =========================================================
     */

    public byte[] generarDiagnostico(
            String rutaReferencia,
            String rutaEvidencia) {

        validarRuta(
                rutaReferencia,
                "referencia oficial");

        validarRuta(
                rutaEvidencia,
                "evidencia");

        Mat referencia = Imgcodecs.imread(
                rutaReferencia);

        Mat evidencia = Imgcodecs.imread(
                rutaEvidencia);

        if (referencia.empty()) {

            throw new IllegalArgumentException(
                    "No fue posible leer la referencia oficial.");
        }

        if (evidencia.empty()) {

            referencia.release();

            throw new IllegalArgumentException(
                    "No fue posible leer la evidencia.");
        }

        Mat grisReferencia = new Mat();

        Mat grisEvidencia = new Mat();

        Mat descriptoresReferencia = new Mat();

        Mat descriptoresEvidencia = new Mat();

        Mat mascaraVacia = new Mat();

        Mat mascaraInliers = new Mat();

        Mat homografia = null;

        Mat diagnostico = null;

        MatOfByte buffer = new MatOfByte();

        MatOfKeyPoint puntosReferencia = new MatOfKeyPoint();

        MatOfKeyPoint puntosEvidencia = new MatOfKeyPoint();

        MatOfPoint2f puntosReferenciaValidos = new MatOfPoint2f();

        MatOfPoint2f puntosEvidenciaValidos = new MatOfPoint2f();

        List<MatOfDMatch> coincidenciasKnn = new ArrayList<>();

        try {

            /*
             * =====================================================
             * ESCALA DE GRISES
             * =====================================================
             */

            Imgproc.cvtColor(
                    referencia,
                    grisReferencia,
                    Imgproc.COLOR_BGR2GRAY);

            Imgproc.cvtColor(
                    evidencia,
                    grisEvidencia,
                    Imgproc.COLOR_BGR2GRAY);

            Imgproc.equalizeHist(
                    grisReferencia,
                    grisReferencia);

            Imgproc.equalizeHist(
                    grisEvidencia,
                    grisEvidencia);

            /*
             * =====================================================
             * SIFT
             * =====================================================
             */

            SIFT sift = SIFT.create();

            sift.detectAndCompute(
                    grisReferencia,
                    mascaraVacia,
                    puntosReferencia,
                    descriptoresReferencia);

            sift.detectAndCompute(
                    grisEvidencia,
                    mascaraVacia,
                    puntosEvidencia,
                    descriptoresEvidencia);

            if (descriptoresReferencia.empty()
                    || descriptoresEvidencia.empty()) {

                throw new IllegalStateException(
                        "No se encontraron suficientes características visuales.");
            }

            KeyPoint[] keypointsReferencia = puntosReferencia.toArray();

            KeyPoint[] keypointsEvidencia = puntosEvidencia.toArray();

            /*
             * =====================================================
             * MATCHING
             * =====================================================
             */

            BFMatcher matcher = BFMatcher.create(
                    Core.NORM_L2,
                    false);

            matcher.knnMatch(
                    descriptoresReferencia,
                    descriptoresEvidencia,
                    coincidenciasKnn,
                    2);

            List<DMatch> coincidenciasValidas = new ArrayList<>();

            for (MatOfDMatch grupo : coincidenciasKnn) {

                DMatch[] matches = grupo.toArray();

                if (matches.length < 2) {
                    continue;
                }

                DMatch mejor = matches[0];

                DMatch segunda = matches[1];

                if (mejor.distance < RATIO_COINCIDENCIA
                        * segunda.distance) {

                    coincidenciasValidas.add(
                            mejor);
                }
            }

            if (coincidenciasValidas.size() < 4) {

                throw new IllegalStateException(
                        "No existen suficientes coincidencias para generar el diagnóstico.");
            }

            /*
             * =====================================================
             * CONSTRUIR PUNTOS PARA HOMOGRAFÍA
             * =====================================================
             */

            List<Point> listaReferencia = new ArrayList<>();

            List<Point> listaEvidencia = new ArrayList<>();

            for (DMatch match : coincidenciasValidas) {

                listaReferencia.add(
                        keypointsReferencia[match.queryIdx].pt);

                listaEvidencia.add(
                        keypointsEvidencia[match.trainIdx].pt);
            }

            puntosReferenciaValidos.fromList(
                    listaReferencia);

            puntosEvidenciaValidos.fromList(
                    listaEvidencia);

            /*
             * =====================================================
             * RANSAC
             * =====================================================
             */

            homografia = Calib3d.findHomography(
                    puntosReferenciaValidos,
                    puntosEvidenciaValidos,
                    Calib3d.RANSAC,
                    UMBRAL_RANSAC_PIXELES,
                    mascaraInliers,
                    2000,
                    0.995);

            if (homografia == null
                    || homografia.empty()
                    || mascaraInliers.empty()) {

                throw new IllegalStateException(
                        "No fue posible calcular una homografía válida.");
            }

            /*
             * =====================================================
             * CREAR LIENZO
             *
             * Referencia a la izquierda.
             * Evidencia a la derecha.
             * =====================================================
             */

            int anchoReferencia = referencia.width();

            int anchoEvidencia = evidencia.width();

            int alto = Math.max(
                    referencia.height(),
                    evidencia.height());

            int anchoTotal = anchoReferencia
                    + anchoEvidencia;

            diagnostico = new Mat(
                    alto,
                    anchoTotal,
                    CvType.CV_8UC3,
                    new Scalar(
                            0,
                            0,
                            0));

            Mat zonaReferencia = diagnostico.submat(
                    new Rect(
                            0,
                            0,
                            referencia.width(),
                            referencia.height()));

            Mat zonaEvidencia = diagnostico.submat(
                    new Rect(
                            anchoReferencia,
                            0,
                            evidencia.width(),
                            evidencia.height()));

            referencia.copyTo(
                    zonaReferencia);

            evidencia.copyTo(
                    zonaEvidencia);

            zonaReferencia.release();
            zonaEvidencia.release();

            /*
             * =====================================================
             * DIBUJAR SOLO INLIERS
             * =====================================================
             */

            byte[] inliers = new byte[(int) mascaraInliers.total()];

            mascaraInliers.get(
                    0,
                    0,
                    inliers);

            int coincidenciasGeometricas = 0;
            /*
             * =====================================================
             * LÍMITES DE LOS PUNTOS RANSAC EN LA EVIDENCIA
             *
             * Esto nos permitirá calcular una región aproximada
             * donde se está concentrando el exhibidor.
             * =====================================================
             */

            double minXEvidencia = Double.MAX_VALUE;

            double minYEvidencia = Double.MAX_VALUE;

            double maxXEvidencia = Double.MIN_VALUE;

            double maxYEvidencia = Double.MIN_VALUE;

            for (int i = 0; i < coincidenciasValidas.size()
                    && i < inliers.length; i++) {

                if (inliers[i] == 0) {
                    continue;
                }

                coincidenciasGeometricas++;

                DMatch match = coincidenciasValidas.get(
                        i);

                Point puntoReferencia = keypointsReferencia[match.queryIdx].pt;

                Point puntoEvidenciaOriginal = keypointsEvidencia[match.trainIdx].pt;
                /*
                 * Guardamos los límites utilizando las coordenadas
                 * originales de la fotografía de evidencia.
                 */
                minXEvidencia = Math.min(
                        minXEvidencia,
                        puntoEvidenciaOriginal.x);

                minYEvidencia = Math.min(
                        minYEvidencia,
                        puntoEvidenciaOriginal.y);

                maxXEvidencia = Math.max(
                        maxXEvidencia,
                        puntoEvidenciaOriginal.x);

                maxYEvidencia = Math.max(
                        maxYEvidencia,
                        puntoEvidenciaOriginal.y);

                /*
                 * Como la evidencia está dibujada a la derecha,
                 * desplazamos su X por el ancho de la referencia.
                 */
                Point puntoEvidencia = new Point(
                        puntoEvidenciaOriginal.x
                                + anchoReferencia,
                        puntoEvidenciaOriginal.y);

                Imgproc.circle(
                        diagnostico,
                        puntoReferencia,
                        6,
                        new Scalar(
                                0,
                                255,
                                0),
                        2);

                Imgproc.circle(
                        diagnostico,
                        puntoEvidencia,
                        6,
                        new Scalar(
                                0,
                                255,
                                0),
                        2);

                Imgproc.line(
                        diagnostico,
                        puntoReferencia,
                        puntoEvidencia,
                        new Scalar(
                                0,
                                255,
                                0),
                        2);
            }
            /*
             * =====================================================
             * DIBUJAR REGIÓN APROXIMADA DE LOS INLIERS
             *
             * IMPORTANTE:
             * Todavía NO consideramos este rectángulo como el
             * recorte definitivo del exhibidor.
             *
             * Es únicamente diagnóstico visual.
             * =====================================================
             */

            if (coincidenciasGeometricas >= 4
                    && minXEvidencia != Double.MAX_VALUE
                    && minYEvidencia != Double.MAX_VALUE) {

                /*
                 * Coordenadas dentro de la imagen compuesta.
                 *
                 * Como la evidencia está ubicada a la derecha,
                 * debemos sumar el ancho de la referencia.
                 */
                Point esquinaSuperior = new Point(
                        minXEvidencia
                                + anchoReferencia,
                        minYEvidencia);

                Point esquinaInferior = new Point(
                        maxXEvidencia
                                + anchoReferencia,
                        maxYEvidencia);

                /*
                 * Rectángulo rojo:
                 * bounding box puro de los puntos RANSAC.
                 */
                Imgproc.rectangle(
                        diagnostico,
                        esquinaSuperior,
                        esquinaInferior,
                        new Scalar(
                                0,
                                0,
                                255),
                        4);

                /*
                 * Etiqueta.
                 */
                int textoRegionY = Math.max(
                        (int) minYEvidencia - 15,
                        30);

                Imgproc.putText(
                        diagnostico,
                        "REGION RANSAC",
                        new Point(
                                minXEvidencia
                                        + anchoReferencia,
                                textoRegionY),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.8,
                        new Scalar(
                                0,
                                0,
                                255),
                        2);
            }

            /*
             * =====================================================
             * INFORMACIÓN SOBRE LA IMAGEN
             * =====================================================
             */

            String texto = String.format(
                    Locale.US,
                    "SIFT validas: %d | RANSAC inliers: %d",
                    coincidenciasValidas.size(),
                    coincidenciasGeometricas);

            Imgproc.putText(
                    diagnostico,
                    texto,
                    new Point(
                            20,
                            40),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.9,
                    new Scalar(
                            0,
                            0,
                            255),
                    2);

            boolean generado = Imgcodecs.imencode(
                    ".png",
                    diagnostico,
                    buffer);

            if (!generado) {

                throw new IllegalStateException(
                        "No fue posible generar la imagen de diagnóstico.");
            }

            return buffer.toArray();

        } finally {

            for (MatOfDMatch grupo : coincidenciasKnn) {
                grupo.release();
            }

            referencia.release();
            evidencia.release();

            grisReferencia.release();
            grisEvidencia.release();

            descriptoresReferencia.release();
            descriptoresEvidencia.release();

            mascaraVacia.release();
            mascaraInliers.release();

            puntosReferencia.release();
            puntosEvidencia.release();

            puntosReferenciaValidos.release();
            puntosEvidenciaValidos.release();

            if (homografia != null) {
                homografia.release();
            }

            if (diagnostico != null) {
                diagnostico.release();
            }

            buffer.release();
        }
    }
    /*
     * =========================================================
     * DETECTAR REGIÓN DE BÚSQUEDA DEL EXHIBIDOR
     *
     * SIFT + RANSAC se utilizan únicamente para encontrar
     * aproximadamente dónde se encuentra el exhibidor.
     *
     * NO se utilizan para identificar productos.
     * =========================================================
     */

    public RegionBusqueda detectarRegionBusqueda(
            String rutaReferencia,
            String rutaEvidencia) {

        validarRuta(
                rutaReferencia,
                "referencia oficial");

        validarRuta(
                rutaEvidencia,
                "evidencia");

        Mat referencia = Imgcodecs.imread(
                rutaReferencia);

        Mat evidencia = Imgcodecs.imread(
                rutaEvidencia);

        if (referencia.empty()) {

            throw new IllegalArgumentException(
                    "No fue posible leer la referencia oficial.");
        }

        if (evidencia.empty()) {

            referencia.release();

            throw new IllegalArgumentException(
                    "No fue posible leer la evidencia.");
        }

        Mat grisReferencia = new Mat();

        Mat grisEvidencia = new Mat();

        Mat descriptoresReferencia = new Mat();

        Mat descriptoresEvidencia = new Mat();

        Mat mascaraVacia = new Mat();

        Mat mascaraInliers = new Mat();

        Mat homografia = null;

        MatOfKeyPoint puntosReferencia = new MatOfKeyPoint();

        MatOfKeyPoint puntosEvidencia = new MatOfKeyPoint();

        MatOfPoint2f puntosReferenciaValidos = new MatOfPoint2f();

        MatOfPoint2f puntosEvidenciaValidos = new MatOfPoint2f();

        List<MatOfDMatch> coincidenciasKnn = new ArrayList<>();

        try {

            /*
             * =====================================================
             * 1. ESCALA DE GRISES
             * =====================================================
             */

            Imgproc.cvtColor(
                    referencia,
                    grisReferencia,
                    Imgproc.COLOR_BGR2GRAY);

            Imgproc.cvtColor(
                    evidencia,
                    grisEvidencia,
                    Imgproc.COLOR_BGR2GRAY);

            /*
             * =====================================================
             * 2. CONTRASTE
             * =====================================================
             */

            Imgproc.equalizeHist(
                    grisReferencia,
                    grisReferencia);

            Imgproc.equalizeHist(
                    grisEvidencia,
                    grisEvidencia);

            /*
             * =====================================================
             * 3. SIFT
             * =====================================================
             */

            SIFT sift = SIFT.create();

            sift.detectAndCompute(
                    grisReferencia,
                    mascaraVacia,
                    puntosReferencia,
                    descriptoresReferencia);

            sift.detectAndCompute(
                    grisEvidencia,
                    mascaraVacia,
                    puntosEvidencia,
                    descriptoresEvidencia);

            if (descriptoresReferencia.empty()
                    || descriptoresEvidencia.empty()) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        0,
                        false);
            }

            KeyPoint[] keypointsReferencia = puntosReferencia.toArray();

            KeyPoint[] keypointsEvidencia = puntosEvidencia.toArray();

            /*
             * =====================================================
             * 4. MATCHING
             * =====================================================
             */

            BFMatcher matcher = BFMatcher.create(
                    Core.NORM_L2,
                    false);

            matcher.knnMatch(
                    descriptoresReferencia,
                    descriptoresEvidencia,
                    coincidenciasKnn,
                    2);

            List<DMatch> coincidenciasValidas = new ArrayList<>();

            for (MatOfDMatch grupo : coincidenciasKnn) {

                DMatch[] matches = grupo.toArray();

                if (matches.length < 2) {
                    continue;
                }

                DMatch mejor = matches[0];

                DMatch segunda = matches[1];

                if (mejor.distance < RATIO_COINCIDENCIA
                        * segunda.distance) {

                    coincidenciasValidas.add(
                            mejor);
                }
            }

            /*
             * Homografía requiere mínimo 4 puntos.
             */
            if (coincidenciasValidas.size() < 4) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        0,
                        false);
            }

            /*
             * =====================================================
             * 5. CONSTRUIR PARES DE PUNTOS
             * =====================================================
             */

            List<Point> listaReferencia = new ArrayList<>();

            List<Point> listaEvidencia = new ArrayList<>();

            for (DMatch match : coincidenciasValidas) {

                if (match.queryIdx < 0
                        || match.queryIdx >= keypointsReferencia.length) {
                    continue;
                }

                if (match.trainIdx < 0
                        || match.trainIdx >= keypointsEvidencia.length) {
                    continue;
                }

                listaReferencia.add(
                        keypointsReferencia[match.queryIdx].pt);

                listaEvidencia.add(
                        keypointsEvidencia[match.trainIdx].pt);
            }

            if (listaReferencia.size() < 4
                    || listaEvidencia.size() < 4) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        0,
                        false);
            }

            puntosReferenciaValidos.fromList(
                    listaReferencia);

            puntosEvidenciaValidos.fromList(
                    listaEvidencia);

            /*
             * =====================================================
             * 6. RANSAC
             * =====================================================
             */

            homografia = Calib3d.findHomography(
                    puntosReferenciaValidos,
                    puntosEvidenciaValidos,
                    Calib3d.RANSAC,
                    UMBRAL_RANSAC_PIXELES,
                    mascaraInliers,
                    2000,
                    0.995);

            if (homografia == null
                    || homografia.empty()
                    || mascaraInliers.empty()) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        0,
                        false);
            }

            /*
             * =====================================================
             * 7. OBTENER LOS PUNTOS QUE RANSAC ACEPTÓ
             * =====================================================
             */

            byte[] inliers = new byte[(int) mascaraInliers.total()];

            mascaraInliers.get(
                    0,
                    0,
                    inliers);

            double minX = Double.MAX_VALUE;

            double minY = Double.MAX_VALUE;

            double maxX = -Double.MAX_VALUE;

            double maxY = -Double.MAX_VALUE;

            int puntosGeometricos = 0;

            for (int i = 0; i < coincidenciasValidas.size()
                    && i < inliers.length; i++) {

                if (inliers[i] == 0) {
                    continue;
                }

                DMatch match = coincidenciasValidas.get(
                        i);

                Point punto = keypointsEvidencia[match.trainIdx].pt;

                minX = Math.min(
                        minX,
                        punto.x);

                minY = Math.min(
                        minY,
                        punto.y);

                maxX = Math.max(
                        maxX,
                        punto.x);

                maxY = Math.max(
                        maxY,
                        punto.y);

                puntosGeometricos++;
            }

            /*
             * Todavía no tenemos suficiente información
             * para generar una región confiable.
             */
            if (puntosGeometricos < MIN_INLIERS_REGION
                    || minX == Double.MAX_VALUE
                    || minY == Double.MAX_VALUE) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        puntosGeometricos,
                        false);
            }

            /*
             * =====================================================
             * 8. BOUNDING BOX ORIGINAL DE RANSAC
             * =====================================================
             */

            double anchoBase = maxX - minX;

            double altoBase = maxY - minY;

            if (anchoBase <= 1
                    || altoBase <= 1) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        puntosGeometricos,
                        false);
            }

            /*
             * =====================================================
             * 9. AMPLIAR LA REGIÓN
             *
             * El bounding box de RANSAC representa solamente
             * dónde existen coincidencias.
             *
             * Necesitamos incluir alrededor de ellas el resto
             * del exhibidor.
             * =====================================================
             */

            int margenX = (int) Math.ceil(
                    Math.max(
                            anchoBase
                                    * MARGEN_HORIZONTAL_FACTOR,
                            evidencia.width()
                                    * MARGEN_HORIZONTAL_IMAGEN));

            int margenY = (int) Math.ceil(
                    Math.max(
                            altoBase
                                    * MARGEN_VERTICAL_FACTOR,
                            evidencia.height()
                                    * MARGEN_VERTICAL_IMAGEN));

            int xInicial = (int) Math.floor(
                    minX)
                    - margenX;

            int yInicial = (int) Math.floor(
                    minY)
                    - margenY;

            int xFinal = (int) Math.ceil(
                    maxX)
                    + margenX;

            int yFinal = (int) Math.ceil(
                    maxY)
                    + margenY;

            /*
             * =====================================================
             * 10. EVITAR SALIRNOS DE LA FOTOGRAFÍA
             * =====================================================
             */

            xInicial = Math.max(
                    0,
                    xInicial);

            yInicial = Math.max(
                    0,
                    yInicial);

            xFinal = Math.min(
                    evidencia.width(),
                    xFinal);

            yFinal = Math.min(
                    evidencia.height(),
                    yFinal);

            int anchoRegion = xFinal
                    - xInicial;

            int altoRegion = yFinal
                    - yInicial;

            if (anchoRegion <= 0
                    || altoRegion <= 0) {

                return new RegionBusqueda(
                        0,
                        0,
                        0,
                        0,
                        puntosGeometricos,
                        false);
            }

            return new RegionBusqueda(
                    xInicial,
                    yInicial,
                    anchoRegion,
                    altoRegion,
                    puntosGeometricos,
                    true);

        } finally {

            for (MatOfDMatch grupo : coincidenciasKnn) {
                grupo.release();
            }

            referencia.release();
            evidencia.release();

            grisReferencia.release();
            grisEvidencia.release();

            descriptoresReferencia.release();
            descriptoresEvidencia.release();

            mascaraVacia.release();
            mascaraInliers.release();

            puntosReferencia.release();
            puntosEvidencia.release();

            puntosReferenciaValidos.release();
            puntosEvidenciaValidos.release();

            if (homografia != null) {
                homografia.release();
            }
        }
    }

    /*
     * =========================================================
     * GENERAR RECORTE DE LA REGIÓN DE BÚSQUEDA
     *
     * Utiliza la región encontrada mediante SIFT + RANSAC
     * y devuelve solamente esa zona de la evidencia.
     * =========================================================
     */

    public byte[] generarRecorteRegionBusqueda(
            String rutaReferencia,
            String rutaEvidencia) {

        RegionBusqueda region = detectarRegionBusqueda(
                rutaReferencia,
                rutaEvidencia);

        if (!region.regionDetectada()) {

            throw new IllegalStateException(
                    "No fue posible detectar una región de búsqueda confiable.");
        }

        Mat evidencia = Imgcodecs.imread(
                rutaEvidencia);

        if (evidencia.empty()) {

            throw new IllegalArgumentException(
                    "No fue posible leer la evidencia.");
        }

        Mat recorte = null;

        MatOfByte buffer = new MatOfByte();

        try {

            Rect rectangulo = new Rect(
                    region.x(),
                    region.y(),
                    region.ancho(),
                    region.alto());

            recorte = evidencia.submat(
                    rectangulo);

            boolean generado = Imgcodecs.imencode(
                    ".png",
                    recorte,
                    buffer);

            if (!generado) {

                throw new IllegalStateException(
                        "No fue posible generar el recorte de la evidencia.");
            }

            return buffer.toArray();

        } finally {

            if (recorte != null) {
                recorte.release();
            }

            evidencia.release();
            buffer.release();
        }
    }

    private ResultadoAlineacion construirResultadoSinHomografia(
            int puntosReferencia,
            int puntosEvidencia,
            int coincidenciasTotales,
            int coincidenciasValidas) {

        double porcentajeCoincidencia = coincidenciasTotales == 0
                ? 0.0
                : coincidenciasValidas
                        * 100.0
                        / coincidenciasTotales;

        return new ResultadoAlineacion(
                puntosReferencia,
                puntosEvidencia,
                coincidenciasTotales,
                coincidenciasValidas,
                porcentajeCoincidencia,
                0,
                0.0,
                false,
                false);
    }

    /*
     * =========================================================
     * VALIDAR RUTA
     * =========================================================
     */

    private void validarRuta(
            String ruta,
            String tipo) {

        if (ruta == null
                || ruta.isBlank()) {

            throw new IllegalArgumentException(
                    "La ruta de la "
                            + tipo
                            + " es obligatoria.");
        }
    }

    /*
     * =========================================================
     * RESULTADO
     * =========================================================
     */

    public record ResultadoAlineacion(
            int puntosReferencia,
            int puntosEvidencia,
            int coincidenciasTotales,
            int coincidenciasValidas,
            double porcentajeCoincidencia,
            int coincidenciasGeometricas,
            double porcentajeInliers,
            boolean homografiaCalculada,
            boolean alineacionPosible) {
    }
    /*
     * =========================================================
     * REGIÓN APROXIMADA DEL EXHIBIDOR
     * =========================================================
     */

    public record RegionBusqueda(
            int x,
            int y,
            int ancho,
            int alto,
            int puntosGeometricos,
            boolean regionDetectada) {
    }
}