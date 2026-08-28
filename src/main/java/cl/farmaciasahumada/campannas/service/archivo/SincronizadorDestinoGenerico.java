package cl.farmaciasahumada.campannas.service.archivo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicionColumna;

@Component
public class SincronizadorDestinoGenerico {

    private static final Pattern IDENTIFICADOR_SQL =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;

    public SincronizadorDestinoGenerico(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public void sincronizar(
            EsquemaArchivo esquema,
            List<FilaTabular> filas) {

        ArchivoDefinicion definicion =
                esquema.definicion();

        String tabla =
                definicion.getTablaDestino();

        /*
         * Si no existe tabla de destino,
         * el archivo funciona solamente
         * mediante registro_generico.
         */
        if (tabla == null || tabla.isBlank()) {
            return;
        }

        validarIdentificador(tabla);

        List<ArchivoDefinicionColumna> columnas =
                esquema.columnas();

        List<String> clavesNegocio =
                columnas.stream()
                        .filter(c ->
                                Boolean.TRUE.equals(
                                        c.getEsClaveNegocio()
                                )
                        )
                        .map(
                                ArchivoDefinicionColumna::getCampoDestino
                        )
                        .toList();

        if (clavesNegocio.isEmpty()) {
            throw new IllegalArgumentException(
                    "La definición "
                            + definicion.getCodigo()
                            + " no tiene clave de negocio configurada."
            );
        }

        /*
         * Por ahora el motor soporta una clave de negocio
         * para el destino relacional.
         *
         * FARMACIAS utiliza codigo_farmacia.
         */
        if (clavesNegocio.size() != 1) {
            throw new IllegalArgumentException(
                    "El destino relacional requiere actualmente "
                            + "una única clave de negocio."
            );
        }

        String columnaClave =
                clavesNegocio.get(0);

        validarIdentificador(
                columnaClave
        );

        List<String> columnasDestino =
                columnas.stream()
                        .map(
                                ArchivoDefinicionColumna::getCampoDestino
                        )
                        .toList();

        columnasDestino.forEach(
                this::validarIdentificador
        );

        List<Object> clavesRecibidas =
                new ArrayList<>();

        for (FilaTabular fila : filas) {

            if (!fila.esValida()) {
                continue;
            }

            Map<String, Object> datos =
                    new LinkedHashMap<>();

            for (String columna :
                    columnasDestino) {

                if (fila.datos()
                        .containsKey(columna)) {

                    datos.put(
                            columna,
                            fila.datos().get(columna)
                    );
                }
            }

            Object valorClave =
                    datos.get(columnaClave);

            if (valorClave == null) {
                continue;
            }

            clavesRecibidas.add(
                    valorClave
            );

            upsert(
                    tabla,
                    columnaClave,
                    datos
            );
        }

        /*
         * Si el archivo representa el estado maestro completo,
         * inactivamos registros que desaparecieron del Excel.
         */
        if ("UPSERT_INACTIVAR".equalsIgnoreCase(
                definicion.getModoSincronizacion())) {

            inactivarAusentes(
                    tabla,
                    columnaClave,
                    clavesRecibidas
            );
        }
    }

    private void upsert(
            String tabla,
            String columnaClave,
            Map<String, Object> datos) {

        if (datos.isEmpty()) {
            return;
        }

        List<String> columnas =
                new ArrayList<>(
                        datos.keySet()
                );

        /*
         * farmacia posee activo.
         * Si existe, cualquier registro recibido
         * vuelve a quedar activo.
         */
        if (existeColumna(tabla, "activo")
                && !datos.containsKey("activo")) {

            columnas.add("activo");
            datos.put("activo", true);
        }

        String nombresColumnas =
                String.join(
                        ", ",
                        columnas
                );

        String placeholders =
                String.join(
                        ", ",
                        columnas.stream()
                                .map(c -> "?")
                                .toList()
                );

        List<String> columnasActualizables =
                columnas.stream()
                        .filter(c ->
                                !c.equals(
                                        columnaClave
                                )
                        )
                        .toList();

        String actualizaciones =
                String.join(
                        ", ",
                        columnasActualizables
                                .stream()
                                .map(c ->
                                        c
                                                + " = EXCLUDED."
                                                + c
                                )
                                .toList()
                );

        String sql =
                "INSERT INTO "
                        + tabla
                        + " ("
                        + nombresColumnas
                        + ") VALUES ("
                        + placeholders
                        + ") "
                        + "ON CONFLICT ("
                        + columnaClave
                        + ") DO UPDATE SET "
                        + actualizaciones;

        Object[] valores =
                columnas.stream()
                        .map(datos::get)
                        .toArray();

        jdbcTemplate.update(
                sql,
                valores
        );
    }

    private void inactivarAusentes(
            String tabla,
            String columnaClave,
            List<Object> clavesRecibidas) {

        /*
         * Solo intentamos inactivar si la tabla
         * realmente posee la columna activo.
         */
        if (!existeColumna(
                tabla,
                "activo")) {

            return;
        }

        if (clavesRecibidas.isEmpty()) {

            /*
             * Protección:
             * jamás inactivamos toda una tabla
             * si el archivo no produjo ninguna
             * clave válida.
             */
            throw new IllegalArgumentException(
                    "No existen registros válidos para sincronizar."
            );
        }

        String placeholders =
                String.join(
                        ", ",
                        clavesRecibidas.stream()
                                .map(c -> "?")
                                .toList()
                );

        String sql =
                "UPDATE "
                        + tabla
                        + " SET activo = FALSE "
                        + "WHERE "
                        + columnaClave
                        + " NOT IN ("
                        + placeholders
                        + ")";

        jdbcTemplate.update(
                sql,
                clavesRecibidas.toArray()
        );
    }

    private boolean existeColumna(
            String tabla,
            String columna) {

        validarIdentificador(tabla);
        validarIdentificador(columna);

        Integer cantidad =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = ?
                          AND column_name = ?
                        """,
                        Integer.class,
                        tabla,
                        columna
                );

        return cantidad != null
                && cantidad > 0;
    }

    private void validarIdentificador(
            String identificador) {

        if (identificador == null
                || !IDENTIFICADOR_SQL
                        .matcher(identificador)
                        .matches()) {

            throw new IllegalArgumentException(
                    "Identificador SQL inválido: "
                            + identificador
            );
        }
    }
}