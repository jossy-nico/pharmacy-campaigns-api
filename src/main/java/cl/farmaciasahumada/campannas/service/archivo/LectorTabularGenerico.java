package cl.farmaciasahumada.campannas.service.archivo;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LectorTabularGenerico {

        private static final int MAX_FILAS = 100_000;
        private static final int MAX_COLUMNAS = 100;
        private static final int MAX_FILAS_BUSQUEDA_ENCABEZADO = 25;

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
                                .formatCellValue(celda, evaluator)
                                .trim();
        }

        // valores iniciales
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

        // nuevo método
        public DocumentoTabular leerAutomatico(
                        MultipartFile archivo) throws IOException {

                validarArchivo(archivo);

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

                                Row encabezado = hoja.getRow(filaEncabezado);

                                List<ColumnaTabular> columnas = detectarColumnas(
                                                encabezado,
                                                formatter,
                                                evaluator);

                                if (columnas.isEmpty()) {
                                        continue;
                                }

                                if (columnas.size() > MAX_COLUMNAS) {
                                        throw new IllegalArgumentException(
                                                        "La hoja '"
                                                                        + hoja.getSheetName()
                                                                        + "' supera el máximo permitido de "
                                                                        + MAX_COLUMNAS
                                                                        + " columnas.");
                                }

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
                                                "No se encontraron hojas tabulares con información.");
                        }

                        return new DocumentoTabular(hojas);
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
                                        "No fue posible detectar la fila de encabezados.");
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

                        String nombreCampo = normalizarNombreCampo(nombreOriginal);

                        nombreCampo = hacerNombreUnico(
                                        nombreCampo,
                                        nombresUtilizados);

                        nombresUtilizados.add(nombreCampo);

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

                        if (filasProcesadas > MAX_FILAS) {
                                throw new IllegalArgumentException(
                                                "La hoja '"
                                                                + nombreHoja
                                                                + "' supera el máximo permitido de "
                                                                + MAX_FILAS
                                                                + " registros.");
                        }

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

        private String normalizarNombreCampo(String texto) {

                if (texto == null || texto.isBlank()) {
                        return "columna";
                }

                String normalizado = Normalizer.normalize(
                                texto,
                                Normalizer.Form.NFD);

                normalizado = normalizado
                                .replaceAll("\\p{M}", "")
                                .toLowerCase()
                                .trim()
                                .replaceAll("[^a-z0-9]+", "_")
                                .replaceAll("^_+|_+$", "")
                                .replaceAll("_+", "_");

                if (normalizado.isBlank()) {
                        return "columna";
                }

                if (Character.isDigit(normalizado.charAt(0))) {
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
                        candidato = nombre + "_" + contador;
                        contador++;
                } while (utilizados.contains(candidato));

                return candidato;
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

}