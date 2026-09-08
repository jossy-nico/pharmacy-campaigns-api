package cl.farmaciasahumada.campannas.service.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
public class CalidadImagenService {

    /*
     * =========================================================
     * ESTADOS DE CALIDAD
     * =========================================================
     */

    private static final String ESTADO_ACEPTABLE = "ACEPTABLE";

    private static final String ESTADO_REQUIERE_REVISION = "REQUIERE_REVISION";

    private static final String ESTADO_REQUIERE_NUEVA_FOTO = "REQUIERE_NUEVA_FOTO";

    /*
     * =========================================================
     * UMBRALES PROVISIONALES
     *
     * IMPORTANTE:
     * estos valores se irán calibrando utilizando
     * fotografías reales de Farmacias Ahumada.
     * =========================================================
     */

    private static final int ANCHO_MINIMO_CRITICO = 480;

    private static final int ALTO_MINIMO_CRITICO = 640;

    private static final int ANCHO_MINIMO_RECOMENDADO = 640;

    private static final int ALTO_MINIMO_RECOMENDADO = 800;

    private static final double NITIDEZ_CRITICA = 80.0;

    private static final double NITIDEZ_REVISION = 200.0;

    private static final double BRILLO_MINIMO_CRITICO = 55.0;

    private static final double BRILLO_MAXIMO_CRITICO = 215.0;

    private static final double BRILLO_MINIMO_REVISION = 80.0;

    private static final double BRILLO_MAXIMO_REVISION = 190.0;

    private static final double CONTRASTE_CRITICO = 18.0;

    private static final double CONTRASTE_REVISION = 30.0;

    private static final double SOBREEXPOSICION_CRITICA = 20.0;

    private static final double SOBREEXPOSICION_REVISION = 10.0;

    private static final double OSCURIDAD_CRITICA = 30.0;

    private static final double OSCURIDAD_REVISION = 20.0;

    /*
     * Los reflejos todavía NO provocan rechazo automático.
     *
     * La medición global puede incluir:
     * - luces del techo
     * - pantallas
     * - superficies blancas
     * - elementos externos al exhibidor.
     *
     * Más adelante los volveremos a medir dentro
     * del recorte real del exhibidor.
     */
    private static final double REFLEJO_ADVERTENCIA = 1.0;

    /*
     * =========================================================
     * ANALIZAR CALIDAD
     * =========================================================
     */

    public CalidadImagenResultado analizar(
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

        Mat laplaciano = new Mat();

        Mat hsv = new Mat();

        Mat mascaraClara = new Mat();

        Mat mascaraOscura = new Mat();

        Mat mascaraReflejo = new Mat();

        MatOfDouble mediaLaplaciano = new MatOfDouble();

        MatOfDouble desviacionLaplaciano = new MatOfDouble();

        MatOfDouble mediaGris = new MatOfDouble();

        MatOfDouble desviacionGris = new MatOfDouble();

        try {

            /*
             * =====================================================
             * RESOLUCIÓN
             * =====================================================
             */

            int ancho = original.width();

            int alto = original.height();

            long totalPixeles = (long) ancho
                    * alto;

            /*
             * =====================================================
             * ESCALA DE GRISES
             * =====================================================
             */

            Imgproc.cvtColor(
                    original,
                    gris,
                    Imgproc.COLOR_BGR2GRAY);

            /*
             * =====================================================
             * NITIDEZ
             *
             * Varianza del Laplaciano.
             * =====================================================
             */

            Imgproc.Laplacian(
                    gris,
                    laplaciano,
                    CvType.CV_64F);

            Core.meanStdDev(
                    laplaciano,
                    mediaLaplaciano,
                    desviacionLaplaciano);

            double desviacionLaplacianoValor = desviacionLaplaciano
                    .toArray()[0];

            double nitidez = desviacionLaplacianoValor
                    * desviacionLaplacianoValor;

            /*
             * =====================================================
             * BRILLO Y CONTRASTE
             * =====================================================
             */

            Core.meanStdDev(
                    gris,
                    mediaGris,
                    desviacionGris);

            double brilloPromedio = mediaGris
                    .toArray()[0];

            double contraste = desviacionGris
                    .toArray()[0];

            /*
             * =====================================================
             * SOBREEXPOSICIÓN
             * =====================================================
             */

            Imgproc.threshold(
                    gris,
                    mascaraClara,
                    245,
                    255,
                    Imgproc.THRESH_BINARY);

            double porcentajeSobreexpuesto = porcentaje(
                    Core.countNonZero(
                            mascaraClara),
                    totalPixeles);

            /*
             * =====================================================
             * SUBEXPOSICIÓN
             * =====================================================
             */

            Imgproc.threshold(
                    gris,
                    mascaraOscura,
                    25,
                    255,
                    Imgproc.THRESH_BINARY_INV);

            double porcentajeOscuro = porcentaje(
                    Core.countNonZero(
                            mascaraOscura),
                    totalPixeles);

            /*
             * =====================================================
             * REFLEJOS
             * =====================================================
             */

            Imgproc.cvtColor(
                    original,
                    hsv,
                    Imgproc.COLOR_BGR2HSV);

            Core.inRange(
                    hsv,
                    new Scalar(
                            0,
                            0,
                            245),
                    new Scalar(
                            180,
                            55,
                            255),
                    mascaraReflejo);

            double porcentajeReflejo = porcentaje(
                    Core.countNonZero(
                            mascaraReflejo),
                    totalPixeles);

            /*
             * =====================================================
             * EVALUACIÓN
             * =====================================================
             */

            List<String> advertencias = generarAdvertencias(
                    ancho,
                    alto,
                    nitidez,
                    brilloPromedio,
                    contraste,
                    porcentajeSobreexpuesto,
                    porcentajeOscuro,
                    porcentajeReflejo);

            String estadoCalidad = determinarEstadoCalidad(
                    ancho,
                    alto,
                    nitidez,
                    brilloPromedio,
                    contraste,
                    porcentajeSobreexpuesto,
                    porcentajeOscuro);

            return new CalidadImagenResultado(
                    ancho,
                    alto,
                    totalPixeles,
                    nitidez,
                    brilloPromedio,
                    contraste,
                    porcentajeSobreexpuesto,
                    porcentajeOscuro,
                    porcentajeReflejo,
                    estadoCalidad,
                    advertencias);

        } finally {

            original.release();
            gris.release();
            laplaciano.release();
            hsv.release();
            mascaraClara.release();
            mascaraOscura.release();
            mascaraReflejo.release();

            mediaLaplaciano.release();
            desviacionLaplaciano.release();
            mediaGris.release();
            desviacionGris.release();
        }
    }

    /*
     * =========================================================
     * DETERMINAR ESTADO
     * =========================================================
     */

    private String determinarEstadoCalidad(
            int ancho,
            int alto,
            double nitidez,
            double brilloPromedio,
            double contraste,
            double porcentajeSobreexpuesto,
            double porcentajeOscuro) {

        /*
         * =====================================================
         * CASOS CRÍTICOS
         * =====================================================
         */

        if (ancho < ANCHO_MINIMO_CRITICO
                || alto < ALTO_MINIMO_CRITICO) {

            return ESTADO_REQUIERE_NUEVA_FOTO;
        }

        if (nitidez < NITIDEZ_CRITICA) {

            return ESTADO_REQUIERE_NUEVA_FOTO;
        }

        if (brilloPromedio < BRILLO_MINIMO_CRITICO
                || brilloPromedio > BRILLO_MAXIMO_CRITICO) {

            return ESTADO_REQUIERE_NUEVA_FOTO;
        }

        if (contraste < CONTRASTE_CRITICO) {

            return ESTADO_REQUIERE_NUEVA_FOTO;
        }

        if (porcentajeSobreexpuesto > SOBREEXPOSICION_CRITICA) {

            return ESTADO_REQUIERE_NUEVA_FOTO;
        }

        if (porcentajeOscuro > OSCURIDAD_CRITICA) {

            return ESTADO_REQUIERE_NUEVA_FOTO;
        }

        /*
         * =====================================================
         * CASOS QUE REQUIEREN REVISIÓN
         * =====================================================
         */

        if (ancho < ANCHO_MINIMO_RECOMENDADO
                || alto < ALTO_MINIMO_RECOMENDADO) {

            return ESTADO_REQUIERE_REVISION;
        }

        if (nitidez < NITIDEZ_REVISION) {

            return ESTADO_REQUIERE_REVISION;
        }

        if (brilloPromedio < BRILLO_MINIMO_REVISION
                || brilloPromedio > BRILLO_MAXIMO_REVISION) {

            return ESTADO_REQUIERE_REVISION;
        }

        if (contraste < CONTRASTE_REVISION) {

            return ESTADO_REQUIERE_REVISION;
        }

        if (porcentajeSobreexpuesto > SOBREEXPOSICION_REVISION) {

            return ESTADO_REQUIERE_REVISION;
        }

        if (porcentajeOscuro > OSCURIDAD_REVISION) {

            return ESTADO_REQUIERE_REVISION;
        }

        return ESTADO_ACEPTABLE;
    }

    /*
     * =========================================================
     * GENERAR ADVERTENCIAS
     * =========================================================
     */

    private List<String> generarAdvertencias(
            int ancho,
            int alto,
            double nitidez,
            double brilloPromedio,
            double contraste,
            double porcentajeSobreexpuesto,
            double porcentajeOscuro,
            double porcentajeReflejo) {

        List<String> advertencias = new ArrayList<>();

        if (ancho < ANCHO_MINIMO_RECOMENDADO
                || alto < ALTO_MINIMO_RECOMENDADO) {

            advertencias.add(
                    "RESOLUCION_BAJA");
        }

        if (nitidez < NITIDEZ_REVISION) {

            advertencias.add(
                    "NITIDEZ_BAJA");
        }

        if (brilloPromedio < BRILLO_MINIMO_REVISION) {

            advertencias.add(
                    "IMAGEN_OSCURA");
        }

        if (brilloPromedio > BRILLO_MAXIMO_REVISION) {

            advertencias.add(
                    "IMAGEN_MUY_CLARA");
        }

        if (contraste < CONTRASTE_REVISION) {

            advertencias.add(
                    "CONTRASTE_BAJO");
        }

        if (porcentajeSobreexpuesto > SOBREEXPOSICION_REVISION) {

            advertencias.add(
                    "SOBREEXPOSICION");
        }

        if (porcentajeOscuro > OSCURIDAD_REVISION) {

            advertencias.add(
                    "ZONAS_MUY_OSCURAS");
        }

        if (porcentajeReflejo > REFLEJO_ADVERTENCIA) {

            advertencias.add(
                    "PRESENCIA_REFLEJOS");
        }

        return List.copyOf(
                advertencias);
    }

    /*
     * =========================================================
     * PORCENTAJE
     * =========================================================
     */

    private double porcentaje(
            long cantidad,
            long total) {

        if (total <= 0) {
            return 0;
        }

        return cantidad
                * 100.0
                / total;
    }

    /*
     * =========================================================
     * RESULTADO
     * =========================================================
     */

    public record CalidadImagenResultado(
            int ancho,
            int alto,
            long totalPixeles,
            double nitidez,
            double brilloPromedio,
            double contraste,
            double porcentajeSobreexpuesto,
            double porcentajeOscuro,
            double porcentajeReflejo,
            String estadoCalidad,
            List<String> advertencias) {
    }
}