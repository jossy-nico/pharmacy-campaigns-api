package cl.farmaciasahumada.campannas.service.archivo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GestorTablaDinamica {

    private static final String ESQUEMA_DINAMICO = "datos_dinamicos";

    private static final Pattern IDENTIFICADOR_VALIDO = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    private final JdbcTemplate jdbcTemplate;

    public GestorTablaDinamica(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String crearOActualizarTabla(
            String nombreSolicitado,
            EsquemaTabularInferido esquema) {

        if (esquema == null) {
            throw new IllegalArgumentException(
                    "El esquema tabular es obligatorio.");
        }

        String nombreTabla = normalizarNombreTabla(
                nombreSolicitado);

        /*
         * Este schema será el espacio exclusivo
         * para datasets creados dinámicamente.
         */
        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS "
                        + ESQUEMA_DINAMICO);

        /*
         * Evita que dos cargas simultáneas intenten
         * modificar la misma tabla al mismo tiempo.
         */
        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                Object.class,
                ESQUEMA_DINAMICO + "." + nombreTabla);

        crearTablaSiNoExiste(
                nombreTabla);

        for (ColumnaInferida columna : esquema.columnas()) {

            agregarColumnaSiNoExiste(
                    nombreTabla,
                    columna);
        }

        return ESQUEMA_DINAMICO
                + "."
                + nombreTabla;
    }

    @Transactional
    public String crearOActualizarYCargar(
            String nombreSolicitado,
            DocumentoTabular documento,
            EsquemaTabularInferido esquema) {

        String tablaCompleta = crearOActualizarTabla(
                nombreSolicitado,
                esquema);

        String nombreTabla = normalizarNombreTabla(
                nombreSolicitado);

        /*
         * La tabla dinámica representa el estado actual
         * del archivo.
         *
         * Si se vuelve a subir el mismo dataset,
         * reemplazamos los registros actuales.
         *
         * El histórico del archivo lo conservará
         * posteriormente nuestro motor de archivos.
         */
        jdbcTemplate.execute(
                "DELETE FROM "
                        + ESQUEMA_DINAMICO
                        + "."
                        + nombreTabla);

        Map<String, TipoDatoTabular> tipos = new LinkedHashMap<>();

        for (ColumnaInferida columna : esquema.columnas()) {

            tipos.put(
                    columna.nombreCampo(),
                    columna.tipoDato());
        }

        for (FilaTabularCruda fila : documento.filas()) {

            insertarFila(
                    nombreTabla,
                    fila,
                    esquema.columnas());
        }

        return tablaCompleta;
    }

    private void insertarFila(
            String nombreTabla,
            FilaTabularCruda fila,
            List<ColumnaInferida> columnas) {

        StringBuilder nombresColumnas = new StringBuilder(
                "_sys_hoja, _sys_numero_fila");

        StringBuilder parametros = new StringBuilder("?, ?");

        for (ColumnaInferida columna : columnas) {

            validarIdentificador(
                    columna.nombreCampo());

            nombresColumnas
                    .append(", ")
                    .append(columna.nombreCampo());

            parametros.append(", ?");
        }

        String sql = """
                INSERT INTO %s.%s (%s)
                VALUES (%s)
                """
                .formatted(
                        ESQUEMA_DINAMICO,
                        nombreTabla,
                        nombresColumnas,
                        parametros);

        Object[] valores = new Object[columnas.size() + 2];

        valores[0] = fila.nombreHoja();

        valores[1] = fila.numeroFila();

        for (int i = 0; i < columnas.size(); i++) {

            ColumnaInferida columna = columnas.get(i);

            String valorOriginal = fila.valores()
                    .get(
                            columna.nombreCampo());

            valores[i + 2] = convertirValor(
                    valorOriginal,
                    columna);
        }

        jdbcTemplate.update(
                sql,
                valores);
    }

    private void crearTablaSiNoExiste(
            String nombreTabla) {

        String sql = """
                CREATE TABLE IF NOT EXISTS %s.%s (
                    _sys_id BIGSERIAL PRIMARY KEY,
                    _sys_archivo_id BIGINT,
                    _sys_hoja TEXT,
                    _sys_numero_fila INTEGER,
                    _sys_fecha_carga TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
                .formatted(
                        ESQUEMA_DINAMICO,
                        nombreTabla);

        jdbcTemplate.execute(sql);
    }

    private void agregarColumnaSiNoExiste(
            String nombreTabla,
            ColumnaInferida columna) {

        validarIdentificador(
                columna.nombreCampo());

        String tipoSql = convertirTipoSql(
                columna.tipoDato());

        String sql = """
                ALTER TABLE %s.%s
                ADD COLUMN IF NOT EXISTS %s %s
                """
                .formatted(
                        ESQUEMA_DINAMICO,
                        nombreTabla,
                        columna.nombreCampo(),
                        tipoSql);

        jdbcTemplate.execute(sql);
    }

    private String convertirTipoSql(
            TipoDatoTabular tipoDato) {

        return switch (tipoDato) {

            case TEXT ->
                "TEXT";

            case BIGINT ->
                "BIGINT";

            case DECIMAL ->
                "NUMERIC";

            case BOOLEAN ->
                "BOOLEAN";

            case DATE ->
                "DATE";
        };
    }

    private String normalizarNombreTabla(
            String nombre) {

        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de la tabla es obligatorio.");
        }

        String normalizado = Normalizer.normalize(
                nombre,
                Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .replaceAll("_+", "_");

        if (normalizado.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la tabla no es válido.");
        }

        if (Character.isDigit(
                normalizado.charAt(0))) {

            normalizado = "tabla_" + normalizado;
        }

        validarIdentificador(
                normalizado);

        return normalizado;
    }

    private void validarIdentificador(
            String identificador) {

        if (identificador == null
                || !IDENTIFICADOR_VALIDO
                        .matcher(identificador)
                        .matches()) {

            throw new IllegalArgumentException(
                    "Identificador SQL no válido: "
                            + identificador);
        }
    }

    private Object convertirValor(
            String valor,
            ColumnaInferida columna) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();

        return switch (columna.tipoDato()) {

            case TEXT ->
                limpio;

            case BIGINT ->
                Long.valueOf(limpio);

            case DECIMAL ->
                new BigDecimal(
                        limpio.replace(",", "."));

            case BOOLEAN ->
                convertirBooleano(limpio);

            case DATE ->
                convertirFecha(
                        limpio,
                        columna.formatoFecha());
        };
    }

    private Boolean convertirBooleano(
            String valor) {

        String normalizado = Normalizer.normalize(
                valor,
                Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();

        return switch (normalizado) {

            case "true",
                    "si",
                    "yes",
                    "verdadero" ->
                true;

            case "false",
                    "no",
                    "falso" ->
                false;

            default ->
                throw new IllegalArgumentException(
                        "Valor booleano no válido: "
                                + valor);
        };
    }

    private Date convertirFecha(
            String valor,
            String formatoFecha) {

        if (formatoFecha == null
                || formatoFecha.isBlank()) {

            throw new IllegalArgumentException(
                    "No se pudo determinar el formato de la fecha: "
                            + valor);
        }

        try {

            DateTimeFormatter formatter = crearFormatterFecha(
                    formatoFecha);

            LocalDate fecha = LocalDate.parse(
                    valor,
                    formatter);

            return Date.valueOf(fecha);

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Fecha no válida '"
                            + valor
                            + "' para el formato "
                            + formatoFecha,
                    e);
        }
    }

    private DateTimeFormatter crearFormatterFecha(
            String formatoFecha) {

        return switch (formatoFecha) {

            case "M/d/yy" ->
                new java.time.format.DateTimeFormatterBuilder()
                        .appendPattern("M/d/")
                        .appendValueReduced(
                                java.time.temporal.ChronoField.YEAR,
                                2,
                                2,
                                1950)
                        .toFormatter(Locale.ROOT)
                        .withResolverStyle(
                                java.time.format.ResolverStyle.STRICT);

            case "d/M/yy" ->
                new java.time.format.DateTimeFormatterBuilder()
                        .appendPattern("d/M/")
                        .appendValueReduced(
                                java.time.temporal.ChronoField.YEAR,
                                2,
                                2,
                                1950)
                        .toFormatter(Locale.ROOT)
                        .withResolverStyle(
                                java.time.format.ResolverStyle.STRICT);

            case "M-d-yy" ->
                new java.time.format.DateTimeFormatterBuilder()
                        .appendPattern("M-d-")
                        .appendValueReduced(
                                java.time.temporal.ChronoField.YEAR,
                                2,
                                2,
                                1950)
                        .toFormatter(Locale.ROOT)
                        .withResolverStyle(
                                java.time.format.ResolverStyle.STRICT);

            case "d-M-yy" ->
                new java.time.format.DateTimeFormatterBuilder()
                        .appendPattern("d-M-")
                        .appendValueReduced(
                                java.time.temporal.ChronoField.YEAR,
                                2,
                                2,
                                1950)
                        .toFormatter(Locale.ROOT)
                        .withResolverStyle(
                                java.time.format.ResolverStyle.STRICT);

            default ->
                DateTimeFormatter
                        .ofPattern(
                                formatoFecha.replace(
                                        "yyyy",
                                        "uuuu"))
                        .withResolverStyle(
                                java.time.format.ResolverStyle.STRICT);
        };
    }
}