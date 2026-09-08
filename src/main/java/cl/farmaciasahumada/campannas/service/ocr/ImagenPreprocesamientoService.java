package cl.farmaciasahumada.campannas.service.ocr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class ImagenPreprocesamientoService {

    /*
     * Factor 2 para mejorar la lectura de textos pequeños
     * sin disparar excesivamente memoria y tiempo de OCR.
     */
    private static final int FACTOR_ESCALA = 2;

    /*
     * =========================================================
     * PREPROCESAMIENTO ACTUAL
     *
     * Se mantiene para no romper el OcrService existente.
     * =========================================================
     */

    public BufferedImage preprocesar(
            File archivoImagen) {

        BufferedImage original = cargarImagen(
                archivoImagen);

        BufferedImage escalada = escalar(
                original);

        BufferedImage grises = convertirEscalaGrises(
                escalada);

        return mejorarContraste(
                grises);
    }

    /*
     * =========================================================
     * GENERAR VARIANTES PARA OCR
     *
     * Más adelante OcrService ejecutará Tesseract sobre todas
     * estas variantes y combinará los resultados.
     * =========================================================
     */

    public List<BufferedImage> preprocesarVariantes(
            File archivoImagen) {

        BufferedImage original = cargarImagen(
                archivoImagen);

        BufferedImage escalada = escalar(
                original);

        BufferedImage grises = convertirEscalaGrises(
                escalada);

        /*
         * =====================================================
         * VARIANTE 1
         * Gris + contraste.
         * =====================================================
         */

        BufferedImage contraste = mejorarContraste(
                grises);

        /*
         * =====================================================
         * VARIANTE 2
         * Gris + contraste + enfoque.
         *
         * Puede ayudar especialmente en:
         * - nombres pequeños
         * - logos
         * - descriptores de productos
         * =====================================================
         */

        BufferedImage enfocada = enfocar(
                contraste);

        /*
         * =====================================================
         * VARIANTE 3
         * Binarización automática mediante Otsu.
         * =====================================================
         */

        BufferedImage otsu = binarizarOtsu(
                contraste);

        /*
         * =====================================================
         * VARIANTE 4
         * Umbral adaptativo.
         *
         * Pensado para fotografías con:
         * - reflejos
         * - sombras
         * - iluminación no uniforme
         * - acrílicos
         * =====================================================
         */

        BufferedImage adaptativa = binarizarAdaptativa(
                grises);

        List<BufferedImage> variantes = new ArrayList<>();

        variantes.add(
                contraste);

        variantes.add(
                enfocada);

        variantes.add(
                otsu);

        variantes.add(
                adaptativa);

        return variantes;
    }

    /*
     * =========================================================
     * CARGAR IMAGEN
     * =========================================================
     */

    private BufferedImage cargarImagen(
            File archivoImagen) {

        if (archivoImagen == null) {

            throw new IllegalArgumentException(
                    "La imagen es obligatoria.");
        }

        if (!archivoImagen.exists()
                || !archivoImagen.isFile()) {

            throw new IllegalArgumentException(
                    "La imagen indicada no existe.");
        }

        try {

            BufferedImage original = ImageIO.read(
                    archivoImagen);

            if (original == null) {

                throw new IllegalArgumentException(
                        "El archivo no corresponde a una imagen válida.");
            }

            return original;

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No fue posible leer la imagen.",
                    e);
        }
    }

    /*
     * =========================================================
     * ESCALADO
     * =========================================================
     */

    private BufferedImage escalar(
            BufferedImage original) {

        int nuevoAncho = original.getWidth()
                * FACTOR_ESCALA;

        int nuevoAlto = original.getHeight()
                * FACTOR_ESCALA;

        BufferedImage resultado = new BufferedImage(
                nuevoAncho,
                nuevoAlto,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = resultado.createGraphics();

        try {

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            graphics.drawImage(
                    original,
                    0,
                    0,
                    nuevoAncho,
                    nuevoAlto,
                    null);

        } finally {

            graphics.dispose();
        }

        return resultado;
    }

    /*
     * =========================================================
     * ESCALA DE GRISES
     * =========================================================
     */

    private BufferedImage convertirEscalaGrises(
            BufferedImage original) {

        BufferedImage resultado = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D graphics = resultado.createGraphics();

        try {

            graphics.drawImage(
                    original,
                    0,
                    0,
                    null);

        } finally {

            graphics.dispose();
        }

        return resultado;
    }

    /*
     * =========================================================
     * CONTRASTE
     * =========================================================
     */

    private BufferedImage mejorarContraste(
            BufferedImage imagen) {

        int ancho = imagen.getWidth();

        int alto = imagen.getHeight();

        BufferedImage resultado = new BufferedImage(
                ancho,
                alto,
                BufferedImage.TYPE_BYTE_GRAY);

        Raster origen = imagen.getRaster();

        WritableRaster destino = resultado.getRaster();

        for (int y = 0; y < alto; y++) {

            for (int x = 0; x < ancho; x++) {

                int gris = origen.getSample(
                        x,
                        y,
                        0);

                int ajustado = (int) ((gris - 128)
                        * 1.5
                        + 128);

                ajustado = Math.max(
                        0,
                        Math.min(
                                255,
                                ajustado));

                destino.setSample(
                        x,
                        y,
                        0,
                        ajustado);
            }
        }

        return resultado;
    }

    /*
     * =========================================================
     * ENFOQUE
     *
     * Kernel moderado para enfatizar bordes de letras.
     * =========================================================
     */

    private BufferedImage enfocar(
            BufferedImage imagen) {

        float[] matriz = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };

        Kernel kernel = new Kernel(
                3,
                3,
                matriz);

        ConvolveOp operacion = new ConvolveOp(
                kernel,
                ConvolveOp.EDGE_NO_OP,
                null);

        BufferedImage resultado = new BufferedImage(
                imagen.getWidth(),
                imagen.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        operacion.filter(
                imagen,
                resultado);

        return resultado;
    }

    /*
     * =========================================================
     * BINARIZACIÓN OTSU
     *
     * Calcula automáticamente un umbral global.
     * =========================================================
     */

    private BufferedImage binarizarOtsu(
            BufferedImage imagen) {

        int umbral = calcularUmbralOtsu(
                imagen);

        int ancho = imagen.getWidth();

        int alto = imagen.getHeight();

        BufferedImage resultado = new BufferedImage(
                ancho,
                alto,
                BufferedImage.TYPE_BYTE_BINARY);

        Raster origen = imagen.getRaster();

        WritableRaster destino = resultado.getRaster();

        for (int y = 0; y < alto; y++) {

            for (int x = 0; x < ancho; x++) {

                int gris = origen.getSample(
                        x,
                        y,
                        0);

                int valor = gris < umbral
                        ? 0
                        : 1;

                destino.setSample(
                        x,
                        y,
                        0,
                        valor);
            }
        }

        return resultado;
    }

    /*
     * =========================================================
     * CALCULAR UMBRAL OTSU
     * =========================================================
     */

    private int calcularUmbralOtsu(
            BufferedImage imagen) {

        int[] histograma = new int[256];

        Raster raster = imagen.getRaster();

        int ancho = imagen.getWidth();

        int alto = imagen.getHeight();

        for (int y = 0; y < alto; y++) {

            for (int x = 0; x < ancho; x++) {

                int gris = raster.getSample(
                        x,
                        y,
                        0);

                histograma[gris]++;
            }
        }

        int totalPixeles = ancho
                * alto;

        double sumaTotal = 0.0;

        for (int i = 0; i < 256; i++) {

            sumaTotal += i
                    * histograma[i];
        }

        double sumaFondo = 0.0;

        int pesoFondo = 0;

        double maximaVarianza = -1.0;

        int mejorUmbral = 128;

        for (int umbral = 0; umbral < 256; umbral++) {

            pesoFondo += histograma[umbral];

            if (pesoFondo == 0) {
                continue;
            }

            int pesoFrente = totalPixeles
                    - pesoFondo;

            if (pesoFrente == 0) {
                break;
            }

            sumaFondo += umbral
                    * histograma[umbral];

            double mediaFondo = sumaFondo
                    / pesoFondo;

            double mediaFrente = (sumaTotal
                    - sumaFondo)
                    / pesoFrente;

            double diferencia = mediaFondo
                    - mediaFrente;

            double varianzaEntreClases = (double) pesoFondo
                    * pesoFrente
                    * diferencia
                    * diferencia;

            if (varianzaEntreClases > maximaVarianza) {

                maximaVarianza = varianzaEntreClases;

                mejorUmbral = umbral;
            }
        }

        return mejorUmbral;
    }

    /*
     * =========================================================
     * BINARIZACIÓN ADAPTATIVA
     *
     * Calcula el promedio de una región alrededor de cada
     * píxel en vez de utilizar un único umbral para toda
     * la fotografía.
     * =========================================================
     */

    private BufferedImage binarizarAdaptativa(
            BufferedImage imagen) {

        int ancho = imagen.getWidth();

        int alto = imagen.getHeight();

        /*
         * Tamaño aproximado de ventana local.
         */
        int radio = 20;

        /*
         * Diferencia exigida respecto al promedio local.
         */
        int constante = 10;

        Raster origen = imagen.getRaster();

        /*
         * Integral image.
         *
         * Permite calcular el promedio de cada ventana
         * eficientemente.
         */
        long[][] integral = new long[alto + 1][ancho + 1];

        for (int y = 1; y <= alto; y++) {

            long sumaFila = 0;

            for (int x = 1; x <= ancho; x++) {

                int gris = origen.getSample(
                        x - 1,
                        y - 1,
                        0);

                sumaFila += gris;

                integral[y][x] = integral[y - 1][x]
                        + sumaFila;
            }
        }

        BufferedImage resultado = new BufferedImage(
                ancho,
                alto,
                BufferedImage.TYPE_BYTE_BINARY);

        WritableRaster destino = resultado.getRaster();

        for (int y = 0; y < alto; y++) {

            int y1 = Math.max(
                    0,
                    y - radio);

            int y2 = Math.min(
                    alto - 1,
                    y + radio);

            for (int x = 0; x < ancho; x++) {

                int x1 = Math.max(
                        0,
                        x - radio);

                int x2 = Math.min(
                        ancho - 1,
                        x + radio);

                long suma = integral[y2 + 1][x2 + 1]
                        - integral[y1][x2 + 1]
                        - integral[y2 + 1][x1]
                        + integral[y1][x1];

                int cantidad = (x2 - x1 + 1)
                        * (y2 - y1 + 1);

                double promedio = (double) suma
                        / cantidad;

                int gris = origen.getSample(
                        x,
                        y,
                        0);

                int valor = gris < promedio - constante
                        ? 0
                        : 1;

                destino.setSample(
                        x,
                        y,
                        0,
                        valor);
            }
        }

        return resultado;
    }
}