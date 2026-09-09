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
         * ESTADOS
         * =========================================================
         */

        private static final String ESTADO_ACEPTABLE = "ACEPTABLE";

        private static final String ESTADO_REQUIERE_REVISION = "REQUIERE_REVISION";

        private static final String ESTADO_REQUIERE_NUEVA_FOTO = "REQUIERE_NUEVA_FOTO";

        /*
         * =========================================================
         * RESOLUCIÓN
         * =========================================================
         */

        private static final int ANCHO_MINIMO_CRITICO = 480;

        private static final int ALTO_MINIMO_CRITICO = 640;

        private static final int ANCHO_MINIMO_RECOMENDADO = 640;

        private static final int ALTO_MINIMO_RECOMENDADO = 800;

        /*
         * =========================================================
         * NITIDEZ
         * =========================================================
         */

        private static final double NITIDEZ_CRITICA = 80.0;

        private static final double NITIDEZ_REVISION = 200.0;

        /*
         * =========================================================
         * BRILLO
         * =========================================================
         */

        private static final double BRILLO_MINIMO_CRITICO = 55.0;

        private static final double BRILLO_MAXIMO_CRITICO = 215.0;

        private static final double BRILLO_MINIMO_REVISION = 80.0;

        private static final double BRILLO_MAXIMO_REVISION = 190.0;

        /*
         * Detección explícita de fotografías
         * negras o prácticamente negras.
         */
        private static final double BRILLO_IMAGEN_NEGRA = 12.0;

        private static final double BRILLO_IMAGEN_CASI_NEGRA = 45.0;

        /*
         * =========================================================
         * CONTRASTE
         * =========================================================
         */

        private static final double CONTRASTE_CRITICO = 18.0;

        private static final double CONTRASTE_REVISION = 30.0;

        /*
         * =========================================================
         * EXPOSICIÓN
         * =========================================================
         */

        private static final double SOBREEXPOSICION_CRITICA = 20.0;

        private static final double SOBREEXPOSICION_REVISION = 10.0;

        private static final double OSCURIDAD_CRITICA = 30.0;

        private static final double OSCURIDAD_REVISION = 20.0;

        private static final double OSCURIDAD_IMAGEN_NEGRA = 98.0;

        private static final double OSCURIDAD_IMAGEN_CASI_NEGRA = 85.0;

        /*
         * =========================================================
         * INFORMACIÓN VISUAL
         *
         * Estos umbrales son iniciales.
         * Se calibrarán con fotos reales de Frogmi.
         * =========================================================
         */

        /*
         * Entropía de una imagen de 8 bits:
         *
         * 0 = prácticamente un único tono
         * 8 = gran diversidad visual
         */
        private static final double ENTROPIA_MINIMA = 4.5;

        /*
         * Porcentaje mínimo aproximado de píxeles
         * considerados borde mediante Canny.
         *
         * Las fotos normales que ya probamos tienen
         * una densidad muy superior a este valor.
         */
        private static final double DENSIDAD_BORDES_MINIMA = 0.60;

        /*
         * Diferencia entre el promedio del canal de color
         * más alto y el más bajo.
         *
         * Se utiliza únicamente como señal complementaria.
         */
        private static final double DOMINANCIA_COLOR_EXTREMA = 55.0;

        /*
         * =========================================================
         * REFLEJOS
         * =========================================================
         */

        private static final double REFLEJO_ADVERTENCIA = 1.0;

        /*
         * =========================================================
         * ANALIZAR
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

                Mat bordes = new Mat();

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
                         * 1. RESOLUCIÓN
                         * =====================================================
                         */

                        int ancho = original.width();

                        int alto = original.height();

                        long totalPixeles = (long) ancho
                                        * alto;

                        /*
                         * =====================================================
                         * 2. ESCALA DE GRISES
                         * =====================================================
                         */

                        Imgproc.cvtColor(
                                        original,
                                        gris,
                                        Imgproc.COLOR_BGR2GRAY);

                        /*
                         * =====================================================
                         * 3. NITIDEZ
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
                         * 4. BRILLO + CONTRASTE
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
                         * 5. SOBREEXPOSICIÓN
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
                         * 6. OSCURIDAD
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
                         * 7. HSV / REFLEJOS
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
                         * Saturación promedio.
                         *
                         * Una foto cubierta por un color puede presentar
                         * una saturación importante.
                         */
                        Scalar mediaHsv = Core.mean(
                                        hsv);

                        double saturacionPromedio = mediaHsv.val[1];

                        /*
                         * =====================================================
                         * 8. DENSIDAD DE BORDES
                         *
                         * Una fotografía real de un exhibidor tiene:
                         *
                         * - cajas
                         * - letras
                         * - envases
                         * - precios
                         * - estantes
                         *
                         * Una fotografía del lente tapado normalmente
                         * tiene muy pocos bordes.
                         * =====================================================
                         */

                        Imgproc.Canny(
                                        gris,
                                        bordes,
                                        50,
                                        150);

                        double densidadBordes = porcentaje(
                                        Core.countNonZero(
                                                        bordes),
                                        totalPixeles);

                        /*
                         * =====================================================
                         * 9. ENTROPÍA VISUAL
                         * =====================================================
                         */

                        double entropiaVisual = calcularEntropia(
                                        gris);

                        /*
                         * =====================================================
                         * 10. DOMINANCIA DE COLOR
                         *
                         * OpenCV utiliza BGR.
                         * =====================================================
                         */

                        Scalar promedioColor = Core.mean(
                                        original);

                        double azul = promedioColor.val[0];

                        double verde = promedioColor.val[1];

                        double rojo = promedioColor.val[2];

                        double canalMayor = Math.max(
                                        rojo,
                                        Math.max(
                                                        verde,
                                                        azul));

                        double canalMenor = Math.min(
                                        rojo,
                                        Math.min(
                                                        verde,
                                                        azul));

                        double dominanciaColor = canalMayor
                                        - canalMenor;

                        /*
                         * =====================================================
                         * 11. CLASIFICACIONES ESPECIALES
                         * =====================================================
                         */

                        boolean imagenNegra = brilloPromedio <= BRILLO_IMAGEN_NEGRA
                                        || porcentajeOscuro >= OSCURIDAD_IMAGEN_NEGRA;

                        boolean imagenCasiNegra = !imagenNegra
                                        && (brilloPromedio <= BRILLO_IMAGEN_CASI_NEGRA
                                                        || porcentajeOscuro >= OSCURIDAD_IMAGEN_CASI_NEGRA);

                        /*
                         * Exigimos ambas condiciones.
                         *
                         * Una imagen puede tener pocos bordes
                         * pero todavía contener información,
                         * o tener poca entropía por algún motivo
                         * sin necesariamente ser inválida.
                         */
                        boolean fondoUniforme = densidadBordes < DENSIDAD_BORDES_MINIMA
                                        && entropiaVisual < ENTROPIA_MINIMA;

                        boolean colorDominanteExtremo = dominanciaColor >= DOMINANCIA_COLOR_EXTREMA;

                        /*
                         * Lente posiblemente cubierto por:
                         *
                         * - dedo
                         * - funda
                         * - superficie
                         * - bolsillo
                         *
                         * No afirmamos categóricamente que el lente
                         * esté tapado; lo tratamos como indicio.
                         */
                        boolean posibleLenteObstruida = fondoUniforme
                                        && (colorDominanteExtremo
                                                        || brilloPromedio < BRILLO_MINIMO_CRITICO);

                        boolean informacionVisualInsuficiente = imagenNegra
                                        || imagenCasiNegra
                                        || fondoUniforme;

                        /*
                         * =====================================================
                         * 12. ADVERTENCIAS
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
                                        porcentajeReflejo,
                                        imagenNegra,
                                        imagenCasiNegra,
                                        fondoUniforme,
                                        colorDominanteExtremo,
                                        posibleLenteObstruida,
                                        informacionVisualInsuficiente);

                        /*
                         * =====================================================
                         * 13. ESTADO
                         * =====================================================
                         */

                        String estadoCalidad = determinarEstadoCalidad(
                                        ancho,
                                        alto,
                                        nitidez,
                                        brilloPromedio,
                                        contraste,
                                        porcentajeSobreexpuesto,
                                        porcentajeOscuro,
                                        informacionVisualInsuficiente);

                        /*
                         * REQUIERE_REVISION sigue pudiendo pasar a IA.
                         *
                         * Solamente REQUIERE_NUEVA_FOTO bloquea
                         * completamente el procesamiento.
                         */
                        boolean requiereNuevaFoto = ESTADO_REQUIERE_NUEVA_FOTO
                                        .equals(
                                                        estadoCalidad);

                        boolean fotoValidaParaAnalisis = !requiereNuevaFoto;

                        /*
                         * =====================================================
                         * 14. MOTIVO PRINCIPAL
                         * =====================================================
                         */

                        String motivoPrincipal = determinarMotivoPrincipal(
                                        estadoCalidad,
                                        ancho,
                                        alto,
                                        nitidez,
                                        brilloPromedio,
                                        contraste,
                                        porcentajeSobreexpuesto,
                                        porcentajeOscuro,
                                        imagenNegra,
                                        imagenCasiNegra,
                                        fondoUniforme);

                        String mensaje = generarMensaje(
                                        estadoCalidad,
                                        motivoPrincipal,
                                        posibleLenteObstruida);

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
                                        entropiaVisual,
                                        densidadBordes,
                                        saturacionPromedio,
                                        dominanciaColor,
                                        estadoCalidad,
                                        fotoValidaParaAnalisis,
                                        requiereNuevaFoto,
                                        motivoPrincipal,
                                        mensaje,
                                        advertencias);

                } finally {

                        original.release();
                        gris.release();
                        laplaciano.release();
                        hsv.release();
                        bordes.release();

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
                        double porcentajeOscuro,
                        boolean informacionVisualInsuficiente) {

                /*
                 * Una foto sin información visual nunca
                 * debe llegar al detector de productos.
                 */
                if (informacionVisualInsuficiente) {

                        return ESTADO_REQUIERE_NUEVA_FOTO;
                }

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
                 * REVISIÓN
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
         * MOTIVO PRINCIPAL
         * =========================================================
         */

        private String determinarMotivoPrincipal(
                        String estadoCalidad,
                        int ancho,
                        int alto,
                        double nitidez,
                        double brilloPromedio,
                        double contraste,
                        double porcentajeSobreexpuesto,
                        double porcentajeOscuro,
                        boolean imagenNegra,
                        boolean imagenCasiNegra,
                        boolean fondoUniforme) {

                if (imagenNegra) {
                        return "IMAGEN_NEGRA";
                }

                if (imagenCasiNegra) {
                        return "IMAGEN_CASI_NEGRA";
                }

                if (fondoUniforme) {
                        return "INFORMACION_VISUAL_INSUFICIENTE";
                }

                if (ancho < ANCHO_MINIMO_CRITICO
                                || alto < ALTO_MINIMO_CRITICO) {

                        return "RESOLUCION_INSUFICIENTE";
                }

                if (nitidez < NITIDEZ_CRITICA) {

                        return "IMAGEN_BORROSA";
                }

                if (brilloPromedio > BRILLO_MAXIMO_CRITICO) {

                        return "IMAGEN_SOBREEXPUESTA";
                }

                if (brilloPromedio < BRILLO_MINIMO_CRITICO) {

                        return "IMAGEN_OSCURA";
                }

                if (contraste < CONTRASTE_CRITICO) {

                        return "CONTRASTE_INSUFICIENTE";
                }

                if (porcentajeSobreexpuesto > SOBREEXPOSICION_CRITICA) {

                        return "SOBREEXPOSICION";
                }

                if (porcentajeOscuro > OSCURIDAD_CRITICA) {

                        return "ZONAS_MUY_OSCURAS";
                }

                if (ESTADO_REQUIERE_REVISION.equals(
                                estadoCalidad)) {

                        return "CALIDAD_REQUIERE_REVISION";
                }

                return "OK";
        }

        /*
         * =========================================================
         * MENSAJE PARA BACK/FRONT
         * =========================================================
         */

        private String generarMensaje(
                        String estadoCalidad,
                        String motivoPrincipal,
                        boolean posibleLenteObstruida) {

                if (ESTADO_ACEPTABLE.equals(
                                estadoCalidad)) {

                        return "La fotografía posee calidad suficiente para continuar con el análisis.";
                }

                if (ESTADO_REQUIERE_REVISION.equals(
                                estadoCalidad)) {

                        return "La fotografía presenta observaciones de calidad, pero puede continuar con el análisis.";
                }

                return switch (motivoPrincipal) {

                        case "IMAGEN_NEGRA" ->
                                "La fotografía es prácticamente negra y no permite analizar el exhibidor.";

                        case "IMAGEN_CASI_NEGRA" ->
                                "La fotografía es demasiado oscura y no contiene información suficiente para analizar los productos.";

                        case "INFORMACION_VISUAL_INSUFICIENTE" -> {

                                if (posibleLenteObstruida) {

                                        yield "La fotografía presenta un fondo prácticamente uniforme y no contiene información visual suficiente para reconocer el exhibidor o sus productos. El lente podría estar obstruido.";
                                }

                                yield "La fotografía no contiene información visual suficiente para reconocer el exhibidor o sus productos.";
                        }

                        case "RESOLUCION_INSUFICIENTE" ->
                                "La resolución de la fotografía es insuficiente para realizar un análisis confiable.";

                        case "IMAGEN_BORROSA" ->
                                "La fotografía presenta un nivel de desenfoque que impide realizar un análisis confiable.";

                        case "IMAGEN_SOBREEXPUESTA",
                                        "SOBREEXPOSICION" ->
                                "La fotografía presenta una sobreexposición que impide analizar correctamente los productos.";

                        case "IMAGEN_OSCURA",
                                        "ZONAS_MUY_OSCURAS" ->
                                "La fotografía es demasiado oscura para realizar un análisis confiable.";

                        case "CONTRASTE_INSUFICIENTE" ->
                                "La fotografía presenta contraste insuficiente para realizar un análisis confiable.";

                        default ->
                                "La fotografía no posee calidad suficiente para continuar con el análisis y se requiere una nueva evidencia.";
                };
        }

        /*
         * =========================================================
         * ADVERTENCIAS
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
                        double porcentajeReflejo,
                        boolean imagenNegra,
                        boolean imagenCasiNegra,
                        boolean fondoUniforme,
                        boolean colorDominanteExtremo,
                        boolean posibleLenteObstruida,
                        boolean informacionVisualInsuficiente) {

                List<String> advertencias = new ArrayList<>();

                if (imagenNegra) {

                        advertencias.add(
                                        "IMAGEN_NEGRA");

                } else if (imagenCasiNegra) {

                        advertencias.add(
                                        "IMAGEN_CASI_NEGRA");
                }

                if (fondoUniforme) {

                        advertencias.add(
                                        "FONDO_UNIFORME");
                }

                if (fondoUniforme
                                && colorDominanteExtremo) {

                        advertencias.add(
                                        "COLOR_DOMINANTE_EXTREMO");
                }

                if (posibleLenteObstruida) {

                        advertencias.add(
                                        "POSIBLE_LENTE_OBSTRUIDA");
                }

                if (informacionVisualInsuficiente) {

                        advertencias.add(
                                        "INFORMACION_VISUAL_INSUFICIENTE");
                }

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
         * ENTROPÍA
         * =========================================================
         */

        private double calcularEntropia(
                        Mat gris) {

                if (gris == null
                                || gris.empty()) {

                        return 0.0;
                }

                long[] histograma = new long[256];

                int ancho = gris.cols();

                int alto = gris.rows();

                byte[] fila = new byte[ancho];

                long total = 0;

                for (int y = 0; y < alto; y++) {

                        gris.get(
                                        y,
                                        0,
                                        fila);

                        for (int x = 0; x < ancho; x++) {

                                int valor = fila[x]
                                                & 0xFF;

                                histograma[valor]++;

                                total++;
                        }
                }

                if (total == 0) {

                        return 0.0;
                }

                double entropia = 0.0;

                for (long frecuencia : histograma) {

                        if (frecuencia == 0) {
                                continue;
                        }

                        double probabilidad = (double) frecuencia
                                        / total;

                        entropia -= probabilidad
                                        * (Math.log(
                                                        probabilidad)
                                                        / Math.log(
                                                                        2.0));
                }

                return entropia;
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

                        return 0.0;
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

                        double entropiaVisual,
                        double densidadBordes,
                        double saturacionPromedio,
                        double dominanciaColor,

                        String estadoCalidad,

                        boolean fotoValidaParaAnalisis,
                        boolean requiereNuevaFoto,

                        String motivoPrincipal,
                        String mensaje,

                        List<String> advertencias) {
        }
}