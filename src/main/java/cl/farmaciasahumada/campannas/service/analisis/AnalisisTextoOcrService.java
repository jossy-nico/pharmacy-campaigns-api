package cl.farmaciasahumada.campannas.service.analisis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class AnalisisTextoOcrService {

    /*
     * =========================================================
     * PALABRAS POCO ÚTILES PARA IDENTIFICAR PRODUCTOS
     *
     * No eliminamos marcas ni descriptores comerciales.
     * Solo palabras gramaticales muy comunes.
     * =========================================================
     */

    private static final Set<String> PALABRAS_VACIAS = Set.of(
            "EL",
            "LA",
            "LOS",
            "LAS",
            "DE",
            "DEL",
            "Y",
            "O",
            "EN",
            "CON",
            "POR",
            "PARA",
            "UN",
            "UNA",
            "UNO",
            "AL");

    /*
     * =========================================================
     * NORMALIZAR RESULTADO COMPLETO DEL OCR
     * =========================================================
     */

    public ResultadoNormalizacion normalizar(
            String textoOcr) {

        if (textoOcr == null
                || textoOcr.isBlank()) {

            return new ResultadoNormalizacion(
                    "",
                    List.of(),
                    List.of(),
                    0);
        }

        String textoUnificado = textoOcr
                .replace(
                        "\r\n",
                        "\n")
                .replace(
                        '\r',
                        '\n');

        String[] lineasOriginales = textoUnificado.split(
                "\n");

        LinkedHashSet<String> lineasUtiles = new LinkedHashSet<>();

        LinkedHashSet<String> tokensUtiles = new LinkedHashSet<>();

        for (String lineaOriginal : lineasOriginales) {

            if (lineaOriginal == null
                    || lineaOriginal.isBlank()) {

                continue;
            }

            String lineaNormalizada = normalizarBase(
                    lineaOriginal);

            if (lineaNormalizada.isBlank()) {
                continue;
            }

            String[] tokens = lineaNormalizada.split(
                    "\\s+");

            List<String> tokensLinea = new ArrayList<>();

            for (String token : tokens) {

                if (!esTokenUtil(
                        token)) {

                    continue;
                }

                tokensLinea.add(
                        token);

                tokensUtiles.add(
                        token);
            }

            if (tokensLinea.isEmpty()) {
                continue;
            }

            lineasUtiles.add(
                    String.join(
                            " ",
                            tokensLinea));
        }

        String textoNormalizado = String.join(
                System.lineSeparator(),
                lineasUtiles);

        return new ResultadoNormalizacion(
                textoNormalizado,
                new ArrayList<>(
                        lineasUtiles),
                new ArrayList<>(
                        tokensUtiles),
                tokensUtiles.size());
    }

    /*
     * =========================================================
     * NORMALIZAR UN VALOR ESPERADO
     *
     * Se utilizará posteriormente con:
     *
     * - Marca
     * - Descriptor
     * - Ubicación
     * - Tipo Exhibición
     * =========================================================
     */

    public String normalizarTermino(
            String valor) {

        if (valor == null
                || valor.isBlank()) {

            return "";
        }

        return normalizarBase(
                valor);
    }

    /*
     * =========================================================
     * OBTENER TOKENS SIGNIFICATIVOS DE UN TEXTO
     * =========================================================
     */

    public List<String> obtenerTokensSignificativos(
            String texto) {

        String normalizado = normalizarTermino(
                texto);

        if (normalizado.isBlank()) {

            return List.of();
        }

        LinkedHashSet<String> resultado = new LinkedHashSet<>();

        String[] tokens = normalizado.split(
                "\\s+");

        for (String token : tokens) {

            if (esTokenUtil(
                    token)) {

                resultado.add(
                        token);
            }
        }

        return new ArrayList<>(
                resultado);
    }

    /*
     * =========================================================
     * BUSCAR MEJOR COINCIDENCIA DE UN TOKEN
     *
     * Ejemplo:
     *
     * esperado:
     * PIELARMINA
     *
     * OCR:
     * OPIELARMING
     *
     * Devuelve el token más parecido y su similitud.
     * =========================================================
     */

    public CoincidenciaToken buscarMejorCoincidencia(
            String esperado,
            List<String> tokensOcr) {

        String esperadoNormalizado = normalizarTermino(
                esperado);

        if (esperadoNormalizado.isBlank()
                || tokensOcr == null
                || tokensOcr.isEmpty()) {

            return new CoincidenciaToken(
                    esperadoNormalizado,
                    null,
                    0.0,
                    false);
        }

        /*
         * Para este método esperamos un término individual.
         * Si llega una frase utilizamos sus tokens por separado
         * en evaluarTermino().
         */
        if (esperadoNormalizado.contains(
                " ")) {

            throw new IllegalArgumentException(
                    "buscarMejorCoincidencia requiere un solo término.");
        }

        String mejorToken = null;

        double mejorSimilitud = 0.0;

        for (String tokenOcr : tokensOcr) {

            if (tokenOcr == null
                    || tokenOcr.isBlank()) {

                continue;
            }

            String observado = normalizarTermino(
                    tokenOcr);

            if (observado.isBlank()) {
                continue;
            }

            double similitud = calcularSimilitud(
                    esperadoNormalizado,
                    observado);

            if (similitud > mejorSimilitud) {

                mejorSimilitud = similitud;

                mejorToken = observado;
            }

            /*
             * Coincidencia exacta:
             * no existe nada mejor que 1.0.
             */
            if (mejorSimilitud >= 1.0) {
                break;
            }
        }

        boolean coincide = mejorToken != null
                && mejorSimilitud >= obtenerUmbral(
                        esperadoNormalizado);

        return new CoincidenciaToken(
                esperadoNormalizado,
                mejorToken,
                mejorSimilitud,
                coincide);
    }

    /*
     * =========================================================
     * EVALUAR UNA MARCA O DESCRIPTOR COMPLETO
     *
     * Permite trabajar también con:
     *
     * LA ROCHE POSAY
     * PROTECTOR SOLAR FACIAL
     * LACTOVIT LACTOUREA
     *
     * Cada palabra esperada se busca por separado.
     * =========================================================
     */

    public ResultadoCoincidencia evaluarTermino(
            String terminoEsperado,
            List<String> tokensOcr) {

        List<String> tokensEsperados = obtenerTokensSignificativos(
                terminoEsperado);

        if (tokensEsperados.isEmpty()
                || tokensOcr == null
                || tokensOcr.isEmpty()) {

            return new ResultadoCoincidencia(
                    normalizarTermino(
                            terminoEsperado),
                    0,
                    0,
                    0.0,
                    false,
                    List.of());
        }

        List<CoincidenciaToken> detalles = new ArrayList<>();

        int coincidentes = 0;

        double sumaSimilitudes = 0.0;

        for (String esperado : tokensEsperados) {

            CoincidenciaToken coincidencia = buscarMejorCoincidencia(
                    esperado,
                    tokensOcr);

            detalles.add(
                    coincidencia);

            sumaSimilitudes += coincidencia.similitud();

            if (coincidencia.coincide()) {

                coincidentes++;
            }
        }

        double cobertura = tokensEsperados.isEmpty()
                ? 0.0
                : coincidentes
                        * 100.0
                        / tokensEsperados.size();

        double similitudPromedio = tokensEsperados.isEmpty()
                ? 0.0
                : sumaSimilitudes
                        / tokensEsperados.size();

        /*
         * Esto NO significa todavía
         * "producto presente".
         *
         * Solamente significa que el término esperado
         * tiene evidencia OCR suficiente.
         */
        boolean terminoReconocido;

        if (tokensEsperados.size() == 1) {

            terminoReconocido = coincidentes == 1;

        } else {

            /*
             * Para frases exigimos al menos la mitad
             * de sus palabras significativas.
             */
            terminoReconocido = cobertura >= 50.0
                    && similitudPromedio >= 0.70;
        }

        return new ResultadoCoincidencia(
                normalizarTermino(
                        terminoEsperado),
                tokensEsperados.size(),
                coincidentes,
                cobertura,
                terminoReconocido,
                detalles);
    }

    /*
     * =========================================================
     * SIMILITUD LEVENSHTEIN
     *
     * 1.0 = idéntico
     * 0.0 = completamente distinto
     * =========================================================
     */

    public double calcularSimilitud(
            String valorA,
            String valorB) {

        String a = normalizarTermino(
                valorA)
                .replace(
                        " ",
                        "");

        String b = normalizarTermino(
                valorB)
                .replace(
                        " ",
                        "");

        if (a.isBlank()
                || b.isBlank()) {

            return 0.0;
        }

        if (a.equals(
                b)) {

            return 1.0;
        }

        int distancia = distanciaLevenshtein(
                a,
                b);

        int longitudMaxima = Math.max(
                a.length(),
                b.length());

        if (longitudMaxima == 0) {

            return 1.0;
        }

        return 1.0
                - ((double) distancia
                        / longitudMaxima);
    }

    /*
     * =========================================================
     * NORMALIZACIÓN BASE
     * =========================================================
     */

    private String normalizarBase(
            String texto) {

        String mayusculas = texto
                .toUpperCase(
                        Locale.ROOT)
                .trim();

        /*
         * Quita tildes:
         *
         * PROTECCIÓN
         * ↓
         * PROTECCION
         */
        String sinAcentos = Normalizer.normalize(
                mayusculas,
                Normalizer.Form.NFD)
                .replaceAll(
                        "\\p{M}+",
                        "");

        /*
         * Conservamos únicamente letras y números.
         *
         * $
         * |
         * [
         * ]
         * comillas
         * símbolos OCR
         *
         * pasan a ser espacios.
         */
        return sinAcentos
                .replaceAll(
                        "[^A-Z0-9]+",
                        " ")
                .replaceAll(
                        "\\s+",
                        " ")
                .trim();
    }

    /*
     * =========================================================
     * VALIDAR TOKEN
     * =========================================================
     */

    private boolean esTokenUtil(
            String token) {

        if (token == null
                || token.isBlank()) {

            return false;
        }

        String limpio = token.trim();

        /*
         * Números solos:
         *
         * 3999
         * 21990
         * 5849
         *
         * son normalmente precios o ruido.
         */
        if (limpio.matches(
                "\\d+")) {

            return false;
        }

        /*
         * Una sola letra suele ser basura OCR.
         */
        if (limpio.length() == 1) {

            return false;
        }

        /*
         * Necesitamos al menos una letra.
         *
         * Esto permite conservar:
         *
         * Q10
         * SPF50
         * CERA
         * SUNWORK
         */
        if (!limpio.matches(
                ".*[A-Z].*")) {

            return false;
        }

        if (PALABRAS_VACIAS.contains(
                limpio)) {

            return false;
        }

        return true;
    }

    /*
     * =========================================================
     * UMBRAL DINÁMICO
     *
     * Cuanto más corta es una palabra,
     * menos errores podemos permitir porque aumenta
     * considerablemente el riesgo de falso positivo.
     * =========================================================
     */

    private double obtenerUmbral(
            String esperado) {

        int longitud = esperado.length();

        if (longitud <= 4) {

            return 0.90;
        }

        if (longitud <= 6) {

            return 0.84;
        }

        if (longitud <= 9) {

            return 0.78;
        }

        return 0.76;
    }

    /*
     * =========================================================
     * LEVENSHTEIN
     * =========================================================
     */

    private int distanciaLevenshtein(
            String a,
            String b) {

        int[] anterior = new int[b.length() + 1];

        int[] actual = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {

            anterior[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {

            actual[0] = i;

            for (int j = 1; j <= b.length(); j++) {

                int costo = a.charAt(
                        i - 1) == b.charAt(
                                j - 1)
                                        ? 0
                                        : 1;

                actual[j] = Math.min(
                        Math.min(
                                actual[j - 1]
                                        + 1,
                                anterior[j]
                                        + 1),
                        anterior[j - 1]
                                + costo);
            }

            int[] temporal = anterior;

            anterior = actual;

            actual = temporal;
        }

        return anterior[b.length()];
    }

    /*
     * =========================================================
     * RESULTADOS
     * =========================================================
     */

    public record ResultadoNormalizacion(
            String textoNormalizado,
            List<String> lineas,
            List<String> tokens,
            int cantidadTokens) {
    }

    public record CoincidenciaToken(
            String esperado,
            String observado,
            double similitud,
            boolean coincide) {
    }

    public record ResultadoCoincidencia(
            String terminoEsperado,
            int tokensEsperados,
            int tokensCoincidentes,
            double cobertura,
            boolean terminoReconocido,
            List<CoincidenciaToken> detalles) {
    }
}