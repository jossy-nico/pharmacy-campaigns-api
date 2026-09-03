package cl.farmaciasahumada.campannas.service.archivo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LectorTabularGenerico {

        private static final int MAX_FILAS = 100_000;
        private static final int MAX_COLUMNAS = 100;
        private static final int MAX_FILAS_BUSQUEDA_ENCABEZADO = 25;

        /*
         * =========================================================
         * ENTRADA PRINCIPAL
         * =========================================================
         */

        public DocumentoTabular leerAutomatico(
                        MultipartFile archivo) throws IOException {

                validarArchivo(archivo);

                String extension = obtenerExtension(
                                archivo.getOriginalFilename());

                return switch (extension) {

                        case "xls", "xlsx" ->
                                leerExcel(archivo);

                        case "csv" ->
                                leerCsv(archivo);

                        default ->
                                throw new IllegalArgumentException(
                                                "Formato de archivo no soportado. "
                                                                + "Actualmente se permiten archivos "
                                                                + ".xls, .xlsx y .csv.");
                };
        }

        /*
         * =========================================================
         * EXCEL
         * =========================================================
         */

        private DocumentoTabular leerExcel(
                        MultipartFile archivo) throws IOException {

                try (Workbook workbook = WorkbookFactory.create(
                                archivo.getInputStream())) {

                        if (workbook.getNumberOfSheets() == 0) {

                                throw new IllegalArgumentException(
                                                "El archivo no contiene hojas.");
                        }

                        DataFormatter formatter = new DataFormatter();

                        FormulaEvaluator evaluator = workbook
                                        .getCreationHelper()
                                        .createFormulaEvaluator();

                        List<HojaTabular> hojas = new ArrayList<>();

                        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

                                Sheet hoja = workbook.getSheetAt(i);

                                /*
                                 * Ignoramos hojas completamente vacías.
                                 */
                                if (!hojaTieneContenido(
                                                hoja,
                                                formatter,
                                                evaluator)) {

                                        continue;
                                }

                                int filaEncabezado = detectarFilaEncabezado(
                                                hoja,
                                                formatter,
                                                evaluator);

                                Row encabezado = hoja.getRow(
                                                filaEncabezado);

                                List<ColumnaTabular> columnas = detectarColumnas(
                                                encabezado,
                                                formatter,
                                                evaluator);

                                if (columnas.isEmpty()) {
                                        continue;
                                }

                                validarCantidadColumnas(
                                                hoja.getSheetName(),
                                                columnas.size());

                                List<FilaTabularCruda> filas = leerFilasAutomaticas(
                                                hoja,
                                                hoja.getSheetName(),
                                                filaEncabezado,
                                                columnas,
                                                formatter,
                                                evaluator);

                                hojas.add(
                                                new HojaTabular(
                                                                hoja.getSheetName(),
                                                                filaEncabezado + 1,
                                                                columnas,
                                                                filas));
                        }

                        if (hojas.isEmpty()) {

                                throw new IllegalArgumentException(
                                                "No se encontraron hojas tabulares "
                                                                + "con información.");
                        }

                        return new DocumentoTabular(
                                        hojas);
                }
        }

        private int detectarFilaEncabezado(
                        Sheet hoja,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                int ultimaFilaBusqueda = Math.min(
                                hoja.getLastRowNum(),
                                MAX_FILAS_BUSQUEDA_ENCABEZADO - 1);

                int mejorFila = -1;
                int mayorCantidadColumnas = 0;

                for (int i = 0; i <= ultimaFilaBusqueda; i++) {

                        Row fila = hoja.getRow(i);

                        if (fila == null) {
                                continue;
                        }

                        int cantidadColumnas = contarCeldasConContenido(
                                        fila,
                                        formatter,
                                        evaluator);

                        if (cantidadColumnas > mayorCantidadColumnas) {

                                mayorCantidadColumnas = cantidadColumnas;

                                mejorFila = i;
                        }
                }

                if (mejorFila < 0) {

                        throw new IllegalArgumentException(
                                        "No fue posible detectar "
                                                        + "la fila de encabezados.");
                }

                return mejorFila;
        }

        private int contarCeldasConContenido(
                        Row fila,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                int cantidad = 0;

                for (Cell celda : fila) {

                        String valor = obtenerTexto(
                                        celda,
                                        formatter,
                                        evaluator);

                        if (!valor.isBlank()) {
                                cantidad++;
                        }
                }

                return cantidad;
        }

        private List<ColumnaTabular> detectarColumnas(
                        Row encabezado,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                List<ColumnaTabular> columnas = new ArrayList<>();

                Set<String> nombresUtilizados = new HashSet<>();

                for (Cell celda : encabezado) {

                        String nombreOriginal = obtenerTexto(
                                        celda,
                                        formatter,
                                        evaluator);

                        if (nombreOriginal.isBlank()) {
                                continue;
                        }

                        String nombreCampo = normalizarNombreCampo(
                                        nombreOriginal);

                        nombreCampo = hacerNombreUnico(
                                        nombreCampo,
                                        nombresUtilizados);

                        nombresUtilizados.add(
                                        nombreCampo);

                        columnas.add(
                                        new ColumnaTabular(
                                                        celda.getColumnIndex(),
                                                        nombreOriginal,
                                                        nombreCampo));
                }

                return columnas;
        }

        private List<FilaTabularCruda> leerFilasAutomaticas(
                        Sheet hoja,
                        String nombreHoja,
                        int filaEncabezado,
                        List<ColumnaTabular> columnas,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                List<FilaTabularCruda> filas = new ArrayList<>();

                int filasProcesadas = 0;

                for (int i = filaEncabezado + 1; i <= hoja.getLastRowNum(); i++) {

                        Row fila = hoja.getRow(i);

                        if (fila == null
                                        || filaEstaVacia(
                                                        fila,
                                                        formatter,
                                                        evaluator)) {

                                continue;
                        }

                        filasProcesadas++;

                        validarCantidadFilas(
                                        nombreHoja,
                                        filasProcesadas);

                        Map<String, String> valores = new LinkedHashMap<>();

                        for (ColumnaTabular columna : columnas) {

                                String valor = obtenerTexto(
                                                fila.getCell(
                                                                columna.indice()),
                                                formatter,
                                                evaluator);

                                valores.put(
                                                columna.nombreCampo(),
                                                valor);
                        }

                        filas.add(
                                        new FilaTabularCruda(
                                                        nombreHoja,
                                                        i + 1,
                                                        valores));
                }

                return filas;
        }

        private boolean filaEstaVacia(
                        Row fila,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                for (Cell celda : fila) {

                        if (!obtenerTexto(
                                        celda,
                                        formatter,
                                        evaluator).isBlank()) {

                                return false;
                        }
                }

                return true;
        }

        private String obtenerTexto(
                        Cell celda,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                if (celda == null) {
                        return "";
                }

                return formatter
                                .formatCellValue(
                                                celda,
                                                evaluator)
                                .trim();
        }

        private boolean hojaTieneContenido(
                        Sheet hoja,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

                for (int i = hoja.getFirstRowNum(); i <= hoja.getLastRowNum(); i++) {

                        Row fila = hoja.getRow(i);

                        if (fila != null
                                        && !filaEstaVacia(
                                                        fila,
                                                        formatter,
                                                        evaluator)) {

                                return true;
                        }
                }

                return false;
        }

        /*
         * =========================================================
         * CSV
         * =========================================================
         */

        private DocumentoTabular leerCsv(
                        MultipartFile archivo) throws IOException {

                byte[] contenidoBytes = archivo.getBytes();

                String contenido = decodificarCsv(
                                contenidoBytes);

                if (contenido.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El archivo CSV está vacío.");
                }

                char delimitador = detectarDelimitadorCsv(
                                contenido);

                List<List<String>> registros = parsearCsv(
                                contenido,
                                delimitador);

                if (registros.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "El archivo CSV no contiene registros.");
                }

                int filaEncabezado = detectarFilaEncabezadoCsv(
                                registros);

                List<String> encabezado = registros.get(
                                filaEncabezado);

                List<ColumnaTabular> columnas = detectarColumnasCsv(
                                encabezado);

                if (columnas.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "No fue posible detectar columnas "
                                                        + "en el archivo CSV.");
                }

                validarCantidadColumnas(
                                "CSV",
                                columnas.size());

                List<FilaTabularCruda> filas = leerFilasCsv(
                                registros,
                                filaEncabezado,
                                columnas);

                List<HojaTabular> hojas = new ArrayList<>();

                hojas.add(
                                new HojaTabular(
                                                "CSV",
                                                filaEncabezado + 1,
                                                columnas,
                                                filas));

                return new DocumentoTabular(
                                hojas);
        }

        /*
         * Detecta automáticamente si el CSV utiliza:
         *
         * ;
         * ,
         * TAB
         */
        private char detectarDelimitadorCsv(
                        String contenido) {

                char[] candidatos = {
                                ';',
                                ',',
                                '\t'
                };

                int[] puntajes = new int[candidatos.length];

                boolean entreComillas = false;
                int lineasAnalizadas = 0;

                for (int i = 0; i < contenido.length()
                                && lineasAnalizadas < MAX_FILAS_BUSQUEDA_ENCABEZADO; i++) {

                        char actual = contenido.charAt(i);

                        if (actual == '"') {

                                if (entreComillas
                                                && i + 1 < contenido.length()
                                                && contenido.charAt(i + 1) == '"') {

                                        i++;
                                        continue;
                                }

                                entreComillas = !entreComillas;

                                continue;
                        }

                        if (!entreComillas) {

                                for (int j = 0; j < candidatos.length; j++) {

                                        if (actual == candidatos[j]) {

                                                puntajes[j]++;
                                        }
                                }

                                if (actual == '\n') {
                                        lineasAnalizadas++;
                                }
                        }
                }

                int mejorIndice = 0;

                for (int i = 1; i < puntajes.length; i++) {

                        if (puntajes[i] > puntajes[mejorIndice]) {

                                mejorIndice = i;
                        }
                }

                /*
                 * Si no detectamos ningún separador,
                 * asumimos coma. Esto permite incluso
                 * archivos de una sola columna.
                 */
                if (puntajes[mejorIndice] == 0) {
                        return ',';
                }

                return candidatos[mejorIndice];
        }

        /*
         * Parser CSV sin dependencias externas.
         *
         * Soporta:
         * - campos entre comillas
         * - delimitadores dentro de comillas
         * - comillas escapadas ""
         * - saltos de línea dentro de campos entre comillas
         * - CRLF y LF
         */
        private List<List<String>> parsearCsv(
                        String contenido,
                        char delimitador) {

                List<List<String>> registros = new ArrayList<>();

                List<String> registroActual = new ArrayList<>();

                StringBuilder campoActual = new StringBuilder();

                boolean entreComillas = false;

                for (int i = 0; i < contenido.length(); i++) {

                        char actual = contenido.charAt(i);

                        if (actual == '"') {

                                if (entreComillas
                                                && i + 1 < contenido.length()
                                                && contenido.charAt(i + 1) == '"') {

                                        campoActual.append('"');
                                        i++;
                                        continue;
                                }

                                entreComillas = !entreComillas;

                                continue;
                        }

                        if (actual == delimitador
                                        && !entreComillas) {

                                registroActual.add(
                                                campoActual
                                                                .toString()
                                                                .trim());

                                campoActual.setLength(0);

                                continue;
                        }

                        if ((actual == '\n'
                                        || actual == '\r')
                                        && !entreComillas) {

                                /*
                                 * Evitamos procesar dos veces CRLF.
                                 */
                                if (actual == '\r'
                                                && i + 1 < contenido.length()
                                                && contenido.charAt(i + 1) == '\n') {

                                        i++;
                                }

                                registroActual.add(
                                                campoActual
                                                                .toString()
                                                                .trim());

                                campoActual.setLength(0);

                                registros.add(
                                                registroActual);

                                registroActual = new ArrayList<>();

                                continue;
                        }

                        campoActual.append(
                                        actual);
                }

                if (entreComillas) {

                        throw new IllegalArgumentException(
                                        "El archivo CSV contiene "
                                                        + "un campo con comillas sin cerrar.");
                }

                if (campoActual.length() > 0
                                || !registroActual.isEmpty()) {

                        registroActual.add(
                                        campoActual
                                                        .toString()
                                                        .trim());

                        registros.add(
                                        registroActual);
                }

                return registros;
        }

        private int detectarFilaEncabezadoCsv(
                        List<List<String>> registros) {

                int limite = Math.min(
                                registros.size(),
                                MAX_FILAS_BUSQUEDA_ENCABEZADO);

                int mejorFila = -1;
                int mayorCantidadColumnas = 0;

                for (int i = 0; i < limite; i++) {

                        List<String> fila = registros.get(i);

                        if (filaCsvEstaVacia(fila)) {
                                continue;
                        }

                        int cantidadColumnas = 0;

                        for (String valor : fila) {

                                if (valor != null
                                                && !valor.isBlank()) {

                                        cantidadColumnas++;
                                }
                        }

                        if (cantidadColumnas > mayorCantidadColumnas) {

                                mayorCantidadColumnas = cantidadColumnas;

                                mejorFila = i;
                        }
                }

                if (mejorFila < 0) {

                        throw new IllegalArgumentException(
                                        "No fue posible detectar "
                                                        + "la fila de encabezados del CSV.");
                }

                return mejorFila;
        }

        private List<ColumnaTabular> detectarColumnasCsv(
                        List<String> encabezado) {

                List<ColumnaTabular> columnas = new ArrayList<>();

                Set<String> nombresUtilizados = new HashSet<>();

                for (int i = 0; i < encabezado.size(); i++) {

                        String nombreOriginal = encabezado.get(i);

                        if (nombreOriginal == null
                                        || nombreOriginal.isBlank()) {

                                continue;
                        }

                        nombreOriginal = nombreOriginal.trim();

                        String nombreCampo = normalizarNombreCampo(
                                        nombreOriginal);

                        nombreCampo = hacerNombreUnico(
                                        nombreCampo,
                                        nombresUtilizados);

                        nombresUtilizados.add(
                                        nombreCampo);

                        columnas.add(
                                        new ColumnaTabular(
                                                        i,
                                                        nombreOriginal,
                                                        nombreCampo));
                }

                return columnas;
        }

        private List<FilaTabularCruda> leerFilasCsv(
                        List<List<String>> registros,
                        int filaEncabezado,
                        List<ColumnaTabular> columnas) {

                List<FilaTabularCruda> filas = new ArrayList<>();

                int filasProcesadas = 0;

                for (int i = filaEncabezado + 1; i < registros.size(); i++) {

                        List<String> fila = registros.get(i);

                        if (filaCsvEstaVacia(fila)) {
                                continue;
                        }

                        filasProcesadas++;

                        validarCantidadFilas(
                                        "CSV",
                                        filasProcesadas);

                        Map<String, String> valores = new LinkedHashMap<>();

                        for (ColumnaTabular columna : columnas) {

                                String valor = "";

                                if (columna.indice() < fila.size()) {

                                        String contenido = fila.get(
                                                        columna.indice());

                                        if (contenido != null) {

                                                valor = contenido.trim();
                                        }
                                }

                                valores.put(
                                                columna.nombreCampo(),
                                                valor);
                        }

                        filas.add(
                                        new FilaTabularCruda(
                                                        "CSV",
                                                        i + 1,
                                                        valores));
                }

                return filas;
        }

        private boolean filaCsvEstaVacia(
                        List<String> fila) {

                if (fila == null
                                || fila.isEmpty()) {

                        return true;
                }

                for (String valor : fila) {

                        if (valor != null
                                        && !valor.isBlank()) {

                                return false;
                        }
                }

                return true;
        }

        /*
         * =========================================================
         * CODIFICACIÓN CSV
         * =========================================================
         */

        private String decodificarCsv(
                        byte[] contenido) {

                try {

                        String texto = StandardCharsets.UTF_8
                                        .newDecoder()
                                        .onMalformedInput(
                                                        CodingErrorAction.REPORT)
                                        .onUnmappableCharacter(
                                                        CodingErrorAction.REPORT)
                                        .decode(
                                                        ByteBuffer.wrap(
                                                                        contenido))
                                        .toString();

                        return eliminarBom(
                                        texto);

                } catch (CharacterCodingException e) {

                        /*
                         * Algunos CSV exportados desde Excel en Windows
                         * pueden venir en Windows-1252.
                         */
                        String texto = new String(
                                        contenido,
                                        Charset.forName(
                                                        "windows-1252"));

                        return eliminarBom(
                                        texto);
                }
        }

        private String eliminarBom(
                        String texto) {

                if (texto != null
                                && !texto.isEmpty()
                                && texto.charAt(0) == '\uFEFF') {

                        return texto.substring(1);
                }

                return texto;
        }

        /*
         * =========================================================
         * VALIDACIONES GENERALES
         * =========================================================
         */

        private void validarArchivo(
                        MultipartFile archivo) {

                if (archivo == null) {

                        throw new IllegalArgumentException(
                                        "El archivo es obligatorio.");
                }

                if (archivo.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "El archivo está vacío.");
                }
        }

        private void validarCantidadColumnas(
                        String nombre,
                        int cantidadColumnas) {

                if (cantidadColumnas > MAX_COLUMNAS) {

                        throw new IllegalArgumentException(
                                        "La estructura '"
                                                        + nombre
                                                        + "' supera el máximo permitido de "
                                                        + MAX_COLUMNAS
                                                        + " columnas.");
                }
        }

        private void validarCantidadFilas(
                        String nombre,
                        int filasProcesadas) {

                if (filasProcesadas > MAX_FILAS) {

                        throw new IllegalArgumentException(
                                        "La estructura '"
                                                        + nombre
                                                        + "' supera el máximo permitido de "
                                                        + MAX_FILAS
                                                        + " registros.");
                }
        }

        private String obtenerExtension(
                        String nombreArchivo) {

                if (nombreArchivo == null
                                || nombreArchivo.isBlank()) {

                        throw new IllegalArgumentException(
                                        "No fue posible determinar "
                                                        + "el nombre del archivo.");
                }

                int posicionPunto = nombreArchivo.lastIndexOf('.');

                if (posicionPunto < 0
                                || posicionPunto == nombreArchivo.length() - 1) {

                        throw new IllegalArgumentException(
                                        "El archivo no posee "
                                                        + "una extensión válida.");
                }

                return nombreArchivo
                                .substring(
                                                posicionPunto + 1)
                                .trim()
                                .toLowerCase(
                                                Locale.ROOT);
        }

        /*
         * =========================================================
         * NORMALIZACIÓN DE COLUMNAS
         * =========================================================
         */

        private String normalizarNombreCampo(
                        String texto) {

                if (texto == null
                                || texto.isBlank()) {

                        return "columna";
                }

                String normalizado = Normalizer.normalize(
                                texto,
                                Normalizer.Form.NFD);

                normalizado = normalizado
                                .replaceAll(
                                                "\\p{M}",
                                                "")
                                .toLowerCase(
                                                Locale.ROOT)
                                .trim()
                                .replaceAll(
                                                "[^a-z0-9]+",
                                                "_")
                                .replaceAll(
                                                "^_+|_+$",
                                                "")
                                .replaceAll(
                                                "_+",
                                                "_");

                if (normalizado.isBlank()) {
                        return "columna";
                }

                if (Character.isDigit(
                                normalizado.charAt(0))) {

                        normalizado = "col_" + normalizado;
                }

                return normalizado;
        }

        private String hacerNombreUnico(
                        String nombre,
                        Set<String> utilizados) {

                if (!utilizados.contains(nombre)) {
                        return nombre;
                }

                int contador = 2;
                String candidato;

                do {

                        candidato = nombre
                                        + "_"
                                        + contador;

                        contador++;

                } while (utilizados.contains(
                                candidato));

                return candidato;
        }
}