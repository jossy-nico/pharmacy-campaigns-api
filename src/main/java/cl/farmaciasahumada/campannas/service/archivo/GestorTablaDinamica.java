package cl.farmaciasahumada.campannas.service.archivo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

                        sincronizarColumna(
                                        nombreTabla,
                                        columna);
                }

                return ESQUEMA_DINAMICO
                                + "."
                                + nombreTabla;
        }

        public List<Map<String, Object>> obtenerSnapshot(
                        String nombreSolicitado,
                        Long archivoId) {

                if (archivoId == null) {
                        throw new IllegalArgumentException(
                                        "El id del archivo es obligatorio.");
                }

                String nombreTabla = normalizarNombreTabla(
                                nombreSolicitado);

                String sql = """
                                SELECT *
                                FROM %s.%s
                                WHERE _sys_archivo_id = ?
                                ORDER BY _sys_id
                                """
                                .formatted(
                                                ESQUEMA_DINAMICO,
                                                nombreTabla);

                return jdbcTemplate.queryForList(
                                sql,
                                archivoId);
        }

        @Transactional
        public int eliminarSnapshot(
                        String nombreSolicitado,
                        Long archivoId) {

                if (archivoId == null) {
                        throw new IllegalArgumentException(
                                        "El id del archivo es obligatorio para eliminar el snapshot.");
                }

                String nombreTabla = normalizarNombreTabla(
                                nombreSolicitado);

                String sql = """
                                DELETE FROM %s.%s
                                WHERE _sys_archivo_id = ?
                                """
                                .formatted(
                                                ESQUEMA_DINAMICO,
                                                nombreTabla);

                return jdbcTemplate.update(
                                sql,
                                archivoId);
        }

        @Transactional
        public String agregarSnapshot(
                        String nombreSolicitado,
                        DocumentoTabular documento,
                        EsquemaTabularInferido esquema,
                        Long archivoId) {

                if (archivoId == null) {
                        throw new IllegalArgumentException(
                                        "El id del archivo es obligatorio para guardar el snapshot.");
                }

                String tablaCompleta = crearOActualizarTabla(
                                nombreSolicitado,
                                esquema);

                String nombreTabla = normalizarNombreTabla(
                                nombreSolicitado);

                /*
                 * IMPORTANTE:
                 * aquí NO hacemos DELETE.
                 *
                 * Cada carga queda asociada a su archivo/version
                 * mediante _sys_archivo_id.
                 */
                for (FilaTabularCruda fila : documento.filas()) {

                        insertarFilaSnapshot(
                                        nombreTabla,
                                        fila,
                                        esquema.columnas(),
                                        archivoId);
                }

                return tablaCompleta;
        }

        private void insertarFilaSnapshot(
                        String nombreTabla,
                        FilaTabularCruda fila,
                        List<ColumnaInferida> columnas,
                        Long archivoId) {

                StringBuilder nombresColumnas = new StringBuilder(
                                "_sys_archivo_id, _sys_hoja, _sys_numero_fila");

                StringBuilder parametros = new StringBuilder(
                                "?, ?, ?");

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

                Object[] valores = new Object[columnas.size() + 3];

                valores[0] = archivoId;
                valores[1] = fila.nombreHoja();
                valores[2] = fila.numeroFila();

                for (int i = 0; i < columnas.size(); i++) {

                        ColumnaInferida columna = columnas.get(i);

                        String valorOriginal = fila.valores()
                                        .get(
                                                        columna.nombreCampo());

                        valores[i + 3] = convertirValor(
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

        private void sincronizarColumna(
                        String nombreTabla,
                        ColumnaInferida columna) {

                validarIdentificador(
                                columna.nombreCampo());

                String tipoEsperado = convertirTipoSql(
                                columna.tipoDato());

                String tipoActual = obtenerTipoColumna(
                                nombreTabla,
                                columna.nombreCampo());

                /*
                 * La columna todavía no existe:
                 * la creamos automáticamente.
                 */
                if (tipoActual == null) {

                        String sql = """
                                        ALTER TABLE %s.%s
                                        ADD COLUMN %s %s
                                        """
                                        .formatted(
                                                        ESQUEMA_DINAMICO,
                                                        nombreTabla,
                                                        columna.nombreCampo(),
                                                        tipoEsperado);

                        jdbcTemplate.execute(sql);

                        return;
                }

                /*
                 * La columna ya tiene el tipo correcto.
                 */
                if (tiposEquivalentes(
                                tipoActual,
                                tipoEsperado)) {

                        return;
                }

                /*
                 * Existe pero cambió su tipo.
                 * La evolución se procesa aparte.
                 */
                evolucionarTipoColumna(
                                nombreTabla,
                                columna,
                                tipoActual,
                                tipoEsperado);
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

        private String obtenerTipoColumna(
                        String nombreTabla,
                        String nombreColumna) {

                List<String> tipos = jdbcTemplate.queryForList(
                                """
                                                SELECT data_type
                                                FROM information_schema.columns
                                                WHERE table_schema = ?
                                                  AND table_name = ?
                                                  AND column_name = ?
                                                """,
                                String.class,
                                ESQUEMA_DINAMICO,
                                nombreTabla,
                                nombreColumna);

                if (tipos.isEmpty()) {
                        return null;
                }

                return tipos.get(0);
        }

        private boolean tiposEquivalentes(
                        String tipoActual,
                        String tipoEsperado) {

                String actual = tipoActual.toLowerCase(Locale.ROOT);

                String esperado = tipoEsperado.toLowerCase(Locale.ROOT);

                return switch (esperado) {

                        case "text" ->
                                actual.equals("text")
                                                || actual.equals("character varying");

                        case "bigint" ->
                                actual.equals("bigint");

                        case "numeric" ->
                                actual.equals("numeric")
                                                || actual.equals("decimal");

                        case "boolean" ->
                                actual.equals("boolean");

                        case "date" ->
                                actual.equals("date");

                        default ->
                                false;
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

        private void evolucionarTipoColumna(
                        String nombreTabla,
                        ColumnaInferida columna,
                        String tipoActual,
                        String tipoEsperado) {

                String nombreTemporal = construirNombreColumnaTemporal(
                                columna.nombreCampo());

                /*
                 * Seguridad:
                 * no continuamos si por algún motivo ya existe
                 * una columna temporal con ese nombre.
                 */
                if (obtenerTipoColumna(
                                nombreTabla,
                                nombreTemporal) != null) {

                        throw new IllegalStateException(
                                        "Ya existe una columna temporal para '"
                                                        + columna.nombreCampo()
                                                        + "'.");
                }

                /*
                 * 1. Creamos una columna temporal
                 * con el nuevo tipo.
                 */
                String crearTemporal = """
                                ALTER TABLE %s.%s
                                ADD COLUMN %s %s
                                """
                                .formatted(
                                                ESQUEMA_DINAMICO,
                                                nombreTabla,
                                                nombreTemporal,
                                                tipoEsperado);

                jdbcTemplate.execute(crearTemporal);

                /*
                 * 2. Obtenemos los valores actuales.
                 */
                String consulta = """
                                SELECT
                                    _sys_id,
                                    %s::text AS valor
                                FROM %s.%s
                                WHERE %s IS NOT NULL
                                """
                                .formatted(
                                                columna.nombreCampo(),
                                                ESQUEMA_DINAMICO,
                                                nombreTabla,
                                                columna.nombreCampo());

                List<ValorExistente> valores = jdbcTemplate.query(
                                consulta,
                                (rs, rowNum) -> new ValorExistente(
                                                rs.getLong("_sys_id"),
                                                rs.getString("valor")));

                /*
                 * 3. Convertimos utilizando exactamente
                 * las mismas reglas del motor genérico.
                 *
                 * Si una conversión falla, se lanza excepción
                 * y PostgreSQL revierte toda la transacción.
                 */
                String actualizar = """
                                UPDATE %s.%s
                                SET %s = ?
                                WHERE _sys_id = ?
                                """
                                .formatted(
                                                ESQUEMA_DINAMICO,
                                                nombreTabla,
                                                nombreTemporal);

                for (ValorExistente valor : valores) {

                        Object convertido = convertirValor(
                                        valor.valor(),
                                        columna);

                        jdbcTemplate.update(
                                        actualizar,
                                        convertido,
                                        valor.id());
                }

                /*
                 * 4. Todos los registros pudieron convertirse.
                 * Eliminamos la columna anterior.
                 */
                String eliminarAnterior = """
                                ALTER TABLE %s.%s
                                DROP COLUMN %s
                                """
                                .formatted(
                                                ESQUEMA_DINAMICO,
                                                nombreTabla,
                                                columna.nombreCampo());

                jdbcTemplate.execute(eliminarAnterior);

                /*
                 * 5. La columna temporal adopta
                 * el nombre original.
                 */
                String renombrar = """
                                ALTER TABLE %s.%s
                                RENAME COLUMN %s TO %s
                                """
                                .formatted(
                                                ESQUEMA_DINAMICO,
                                                nombreTabla,
                                                nombreTemporal,
                                                columna.nombreCampo());

                jdbcTemplate.execute(renombrar);
        }

        private String construirNombreColumnaTemporal(
                        String nombreColumna) {

                String base = nombreColumna;

                /*
                 * PostgreSQL admite identificadores de hasta
                 * 63 caracteres. Dejamos espacio para el sufijo.
                 */
                if (base.length() > 50) {
                        base = base.substring(0, 50);
                }

                String temporal = base + "_sys_tmp";

                validarIdentificador(temporal);

                return temporal;
        }

        private record ValorExistente(
                        Long id,
                        String valor) {
        }
}