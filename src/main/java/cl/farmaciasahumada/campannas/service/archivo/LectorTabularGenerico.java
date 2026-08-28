package cl.farmaciasahumada.campannas.service.archivo;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
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

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicionColumna;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorTabularGenerico {
    private final ObjectMapper objectMapper;

    public LectorTabularGenerico(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public List<FilaTabular> leer(
        MultipartFile archivo,
        EsquemaArchivo esquema) throws IOException{
            ArchivoDefinicion definicion = esquema.definicion();

            if (!"TABULAR".equalsIgnoreCase(definicion.getCategoria())) {
                throw new IllegalArgumentException("La definición" + definicion.getCodigo()
            + "no corresponde a un archivo tabular.");
                
            }
            List<FilaTabular> resultado = new ArrayList<>();

            try (Workbook workbook =
                     WorkbookFactory.create(archivo.getInputStream())) {

            Sheet hoja = workbook.getSheetAt(0);

            int indiceEncabezado =
                    definicion.getFilaEncabezado() != null
                            ? definicion.getFilaEncabezado()
                            : 0;

            Row filaEncabezado = hoja.getRow(indiceEncabezado);

            if (filaEncabezado == null) {
                throw new IllegalArgumentException(
                        "No se encontró la fila de encabezados."
                );
            }

                DataFormatter formatter = new DataFormatter();
                FormulaEvaluator evaluator = 
                workbook.getCreationHelper().createFormulaEvaluator();

                Map<String, Integer> columnasExcel = obtenerColumnasExcel(filaEncabezado, formatter, evaluator);

                Map<ArchivoDefinicionColumna, Integer> columnasMapeadas =
                    mapearColumnas(
                            esquema.columnas(),
                            columnasExcel
                    );

            validarColumnasObligatorias(
                    esquema.columnas(),
                    columnasMapeadas
            );

            Set<Integer> indicesConocidos =
                    new HashSet<>(columnasMapeadas.values());

            for (int i = indiceEncabezado + 1;
                 i <= hoja.getLastRowNum();
                 i++) {

                Row fila = hoja.getRow(i);

                if (fila == null ||
                        filaEstaVacia(fila, formatter, evaluator)) {
                    continue;
                }

                Map<String, Object> datos =
                        new LinkedHashMap<>();

                Map<String, Object> datosAdicionales =
                        new LinkedHashMap<>();

                List<String> errores =
                        new ArrayList<>();

                List<String> partesClave =
                        new ArrayList<>();

                for (ArchivoDefinicionColumna columna :
                        esquema.columnas()) {

                    Integer indice =
                            columnasMapeadas.get(columna);

                    if (indice == null) {
                        continue;
                    }

                    Cell celda = fila.getCell(indice);

                    String valor =
                            obtenerTexto(
                                    celda,
                                    formatter,
                                    evaluator
                            );

                    if (Boolean.TRUE.equals(columna.getObligatoria())
                            && valor.isBlank()) {

                        errores.add(
                                "La columna '"
                                        + columna.getNombreColumna()
                                        + "' es obligatoria."
                        );
                    }

                    Object valorConvertido;

                    try {
                        valorConvertido =
                                convertirValor(
                                        valor,
                                        columna.getTipoDato()
                                );
                    } catch (IllegalArgumentException e) {

                        valorConvertido = valor;

                        errores.add(
                                "Valor inválido en columna '"
                                        + columna.getNombreColumna()
                                        + "': "
                                        + e.getMessage()
                        );
                    }

                    datos.put(
                            columna.getCampoDestino(),
                            valorConvertido
                    );

                    if (Boolean.TRUE.equals(
                            columna.getEsClaveNegocio())
                            && !valor.isBlank()) {

                        partesClave.add(valor);
                    }
                }

                /*
                 * Conservamos columnas que aparezcan en el Excel
                 * pero que todavía no estén configuradas.
                 */
                if (Boolean.TRUE.equals(
                        definicion.getConservarColumnasDesconocidas())) {

                    guardarColumnasDesconocidas(
                            fila,
                            filaEncabezado,
                            indicesConocidos,
                            datosAdicionales,
                            formatter,
                            evaluator
                    );
                }

                String claveNegocio =
                        partesClave.isEmpty()
                                ? null
                                : String.join("|", partesClave);

                if (claveNegocio == null ||
                        claveNegocio.isBlank()) {

                    errores.add(
                            "No fue posible determinar la clave de negocio."
                    );
                }

                /*
                 * i + 1 porque Excel muestra filas comenzando desde 1.
                 */
                resultado.add(
                        new FilaTabular(
                                i + 1,
                                claveNegocio,
                                datos,
                                datosAdicionales,
                                errores
                        )
                );
            }
        }

        return resultado;
    }

    private Map<String, Integer> obtenerColumnasExcel(
            Row encabezado,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {

        Map<String, Integer> columnas =
                new HashMap<>();

        for (Cell celda : encabezado) {

            String nombre =
                    obtenerTexto(
                            celda,
                            formatter,
                            evaluator
                    );

            if (!nombre.isBlank()) {
                columnas.put(
                        normalizar(nombre),
                        celda.getColumnIndex()
                );
            }
        }

        return columnas;
    }

    private Map<ArchivoDefinicionColumna, Integer>
    mapearColumnas(
            List<ArchivoDefinicionColumna> definiciones,
            Map<String, Integer> columnasExcel) {

        Map<ArchivoDefinicionColumna, Integer> resultado =
                new LinkedHashMap<>();

        for (ArchivoDefinicionColumna definicion :
                definiciones) {

            List<String> nombresPosibles =
                    new ArrayList<>();

            nombresPosibles.add(
                    definicion.getNombreColumna()
            );

            nombresPosibles.addAll(
                    obtenerAliases(definicion.getAliases())
            );

            for (String nombre : nombresPosibles) {

                Integer indice =
                        columnasExcel.get(
                                normalizar(nombre)
                        );

                if (indice != null) {

                    resultado.put(
                            definicion,
                            indice
                    );

                    break;
                }
            }
        }

        return resultado;
    }

    private void validarColumnasObligatorias(
            List<ArchivoDefinicionColumna> definiciones,
            Map<ArchivoDefinicionColumna, Integer> columnasMapeadas) {

        List<String> faltantes =
                new ArrayList<>();

        for (ArchivoDefinicionColumna columna :
                definiciones) {

            if (Boolean.TRUE.equals(columna.getObligatoria())
                    && !columnasMapeadas.containsKey(columna)) {

                faltantes.add(
                        columna.getNombreColumna()
                );
            }
        }

        if (!faltantes.isEmpty()) {

            throw new IllegalArgumentException(
                    "Faltan columnas obligatorias: "
                            + String.join(", ", faltantes)
            );
        }
    }

    private void guardarColumnasDesconocidas(
            Row fila,
            Row encabezado,
            Set<Integer> indicesConocidos,
            Map<String, Object> adicionales,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {

        for (Cell celdaEncabezado : encabezado) {

            int indice =
                    celdaEncabezado.getColumnIndex();

            if (indicesConocidos.contains(indice)) {
                continue;
            }

            String nombre =
                    obtenerTexto(
                            celdaEncabezado,
                            formatter,
                            evaluator
                    );

            if (nombre.isBlank()) {
                continue;
            }

            String valor =
                    obtenerTexto(
                            fila.getCell(indice),
                            formatter,
                            evaluator
                    );

            adicionales.put(
                    nombre,
                    valor
            );
        }
    }

    private List<String> obtenerAliases(
            String aliasesJson) {

        if (aliasesJson == null ||
                aliasesJson.isBlank()) {

            return List.of();
        }

        try {

            return objectMapper.readValue(
                    aliasesJson,
                    new TypeReference<List<String>>() {}
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "No fue posible leer los aliases configurados.",
                    e
            );
        }
    }

    private Object convertirValor(
            String valor,
            String tipoDato) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        if (tipoDato == null) {
            return valor;
        }

        return switch (tipoDato.toUpperCase()) {

            case "INTEGER" ->
                    Integer.valueOf(valor);

            case "LONG" ->
                    Long.valueOf(valor);

            case "DECIMAL" ->
                    new java.math.BigDecimal(
                            valor.replace(",", ".")
                    );

            case "BOOLEAN" ->
                    convertirBoolean(valor);

            case "STRING" ->
                    valor;

            default ->
                    valor;
        };
    }

    private Boolean convertirBoolean(String valor) {

        String normalizado =
                normalizar(valor);

        return switch (normalizado) {

            case "true", "si", "1", "yes" ->
                    true;

            case "false", "no", "0" ->
                    false;

            default ->
                    throw new IllegalArgumentException(
                            "se esperaba un valor booleano."
                    );
        };
    }

    private boolean filaEstaVacia(
            Row fila,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {

        for (Cell celda : fila) {

            if (!obtenerTexto(
                    celda,
                    formatter,
                    evaluator
            ).isBlank()) {

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

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String normalizado =
                Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                );

        return normalizado
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }
}