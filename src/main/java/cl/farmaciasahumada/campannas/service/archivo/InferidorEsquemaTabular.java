package cl.farmaciasahumada.campannas.service.archivo;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class InferidorEsquemaTabular {

    /*
     * No necesitamos analizar 100.000 valores para determinar
     * el tipo probable de una columna.
     */
    private static final int MAX_VALORES_MUESTRA = 5_000;

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            // yyyy-MM-dd
            DateTimeFormatter.ISO_LOCAL_DATE
                    .withResolverStyle(ResolverStyle.STRICT),

            // Día / Mes / Año
            DateTimeFormatter.ofPattern("d/M/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Mes / Día / Año
            DateTimeFormatter.ofPattern("M/d/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Día / Mes / Año de 2 dígitos
            DateTimeFormatter.ofPattern("d/M/uu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Mes / Día / Año de 2 dígitos
            DateTimeFormatter.ofPattern("M/d/uu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Día-Mes-Año
            DateTimeFormatter.ofPattern("d-M-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Mes-Día-Año
            DateTimeFormatter.ofPattern("M-d-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Día-Mes-Año de 2 dígitos
            DateTimeFormatter.ofPattern("d-M-uu")
                    .withResolverStyle(ResolverStyle.STRICT),

            // Mes-Día-Año de 2 dígitos
            DateTimeFormatter.ofPattern("M-d-uu")
                    .withResolverStyle(ResolverStyle.STRICT));

    public EsquemaTabularInferido inferir(
            DocumentoTabular documento) {

        Objects.requireNonNull(
                documento,
                "El documento tabular es obligatorio.");

        if (documento.columnas().isEmpty()) {
            throw new IllegalArgumentException(
                    "El documento no contiene columnas.");
        }

        List<ColumnaInferida> columnasInferidas = documento.columnas()
                .stream()
                .map(columna -> inferirColumna(
                        columna,
                        documento.filas()))
                .toList();

        return new EsquemaTabularInferido(
                documento.nombreHoja(),
                documento.filaEncabezado(),
                documento.filas().size(),
                columnasInferidas);
    }

    private ColumnaInferida inferirColumna(
            ColumnaTabular columna,
            List<FilaTabularCruda> filas) {

        List<String> muestra = new ArrayList<>();

        boolean permiteNulos = filas.isEmpty();

        for (FilaTabularCruda fila : filas) {

            String valor = fila.valores()
                    .get(columna.nombreCampo());

            if (valor == null || valor.isBlank()) {
                permiteNulos = true;
                continue;
            }

            if (muestra.size() < MAX_VALORES_MUESTRA) {
                muestra.add(valor.trim());
            }
        }

        String formatoFecha = detectarFormatoFecha(muestra);

        TipoDatoTabular tipoDato = determinarTipo(
                columna,
                muestra,
                formatoFecha);

        return new ColumnaInferida(
                columna.nombreOriginal(),
                columna.nombreCampo(),
                tipoDato,
                permiteNulos,
                formatoFecha);

    }

    private TipoDatoTabular determinarTipo(
            ColumnaTabular columna,
            List<String> valores,
            String formatoFecha) {

        /*
         * Una columna completamente vacía se conserva como TEXT.
         * Es la opción menos destructiva.
         */
        if (valores.isEmpty()) {
            return TipoDatoTabular.TEXT;
        }

        /*
         * Códigos e identificadores se mantienen como texto.
         *
         * Ejemplo:
         * 001245 debe seguir siendo 001245 y no convertirse en 1245.
         */
        if (debePreservarseComoTexto(
                columna.nombreCampo(),
                valores)) {

            return TipoDatoTabular.TEXT;
        }

        if (todosBooleanos(valores)) {
            return TipoDatoTabular.BOOLEAN;
        }

        if (todosBigInt(valores)) {
            return TipoDatoTabular.BIGINT;
        }

        if (todosNumericos(valores)) {
            return TipoDatoTabular.DECIMAL;
        }

        if (formatoFecha != null) {
            return TipoDatoTabular.DATE;
        }

        return TipoDatoTabular.TEXT;
    }

    private boolean debePreservarseComoTexto(
            String nombreCampo,
            List<String> valores) {

        String nombre = normalizar(nombreCampo);

        /*
         * Son patrones genéricos de identificadores.
         * No pertenecen a Farmacia, Mascotas o Productos.
         */
        boolean pareceIdentificador = nombre.matches(
                ".*(^|_)(id|codigo|cod|sku|upc|ean|rut|folio|telefono|celular)($|_).*");

        if (pareceIdentificador) {
            return true;
        }

        /*
         * Si encontramos números con ceros a la izquierda,
         * no debemos convertirlos a número.
         */
        return valores.stream()
                .anyMatch(this::tieneCerosALaIzquierda);
    }

    private boolean tieneCerosALaIzquierda(
            String valor) {

        String limpio = valor.trim();

        return limpio.matches("\\d+")
                && limpio.length() > 1
                && limpio.startsWith("0");
    }

    private boolean todosBooleanos(
            List<String> valores) {

        return valores.stream()
                .allMatch(this::esBooleano);
    }

    private boolean esBooleano(
            String valor) {

        String normalizado = normalizar(valor);

        return switch (normalizado) {
            case "true",
                    "false",
                    "si",
                    "no",
                    "yes",
                    "verdadero",
                    "falso" ->
                true;

            default -> false;
        };
    }

    private boolean todosBigInt(
            List<String> valores) {

        return valores.stream()
                .allMatch(this::esBigInt);
    }

    private boolean esBigInt(
            String valor) {

        String limpio = valor.trim();

        if (!limpio.matches("[+-]?\\d+")) {
            return false;
        }

        try {
            Long.parseLong(limpio);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean todosNumericos(
            List<String> valores) {

        return valores.stream()
                .allMatch(this::esNumeroDecimal);
    }

    private boolean esNumeroDecimal(
            String valor) {

        String limpio = valor.trim()
                .replace(",", ".");

        if (!limpio.matches(
                "[+-]?\\d+(\\.\\d+)?")) {

            return false;
        }

        try {
            new BigDecimal(limpio);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String detectarFormatoFecha(
            List<String> valores) {

        if (valores == null || valores.isEmpty()) {
            return null;
        }

        List<String> nombresFormatos = List.of(
                "yyyy-MM-dd",
                "d/M/yyyy",
                "M/d/yyyy",
                "d/M/yy",
                "M/d/yy",
                "d-M-yyyy",
                "M-d-yyyy",
                "d-M-yy",
                "M-d-yy");

        List<String> formatosCompatibles = new ArrayList<>();

        for (int i = 0; i < FORMATOS_FECHA.size(); i++) {

            DateTimeFormatter formato = FORMATOS_FECHA.get(i);

            boolean todosValidos = valores.stream()
                    .allMatch(valor -> esFecha(
                            valor,
                            formato));

            if (todosValidos) {
                formatosCompatibles.add(
                        nombresFormatos.get(i));
            }
        }

        /*
         * Si más de un formato interpreta correctamente
         * todos los valores, la columna es ambigua.
         *
         * En ese caso preferimos TEXT antes que
         * interpretar una fecha de forma incorrecta.
         */
        if (formatosCompatibles.size() != 1) {
            return null;
        }

        return formatosCompatibles.get(0);
    }

    private boolean esFecha(
            String valor,
            DateTimeFormatter formato) {

        if (valor == null || valor.isBlank()) {
            return false;
        }

        try {

            LocalDate.parse(
                    valor.trim(),
                    formato);

            return true;

        } catch (DateTimeParseException e) {

            return false;
        }
    }

    private String normalizar(
            String texto) {

        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(
                texto,
                Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}