package cl.farmaciasahumada.campannas.service.archivo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DocumentoTabular(
        List<HojaTabular> hojas) {

    public DocumentoTabular {

        if (hojas == null || hojas.isEmpty()) {
            throw new IllegalArgumentException(
                    "El documento debe contener al menos una hoja.");
        }

        hojas = List.copyOf(hojas);
    }

    /*
     * Constructor temporal de compatibilidad.
     *
     * Permite que el código actual que todavía trabaja
     * con una sola hoja continúe compilando.
     */
    public DocumentoTabular(
            String nombreHoja,
            int filaEncabezado,
            List<ColumnaTabular> columnas,
            List<FilaTabularCruda> filas) {

        this(
                List.of(
                        new HojaTabular(
                                nombreHoja,
                                filaEncabezado,
                                columnas,
                                filas)));
    }

    /*
     * Devuelve todas las filas del Excel,
     * independientemente de la hoja de origen.
     */
    public List<FilaTabularCruda> filas() {

        return hojas.stream()
                .flatMap(hoja -> hoja.filas().stream())
                .toList();
    }

    /*
     * Obtiene el conjunto total de columnas
     * encontradas en todas las hojas.
     *
     * Si una columna aparece en varias hojas,
     * solo se conserva una vez.
     */
    public List<ColumnaTabular> columnas() {

        Map<String, ColumnaTabular> columnasUnicas = new LinkedHashMap<>();

        for (HojaTabular hoja : hojas) {

            for (ColumnaTabular columna : hoja.columnas()) {

                columnasUnicas.putIfAbsent(
                        columna.nombreCampo(),
                        columna);
            }
        }

        return new ArrayList<>(
                columnasUnicas.values());
    }

    /*
     * Mantiene compatibilidad temporal
     * con InferidorEsquemaTabular.
     */
    public String nombreHoja() {

        if (hojas.size() == 1) {
            return hojas.get(0).nombre();
        }

        return "MULTIPLES_HOJAS";
    }

    /*
     * Mantiene compatibilidad temporal.
     * Cada hoja conserva internamente su verdadero encabezado.
     */
    public int filaEncabezado() {

        return hojas.get(0)
                .filaEncabezado();
    }

    public int cantidadHojas() {

        return hojas.size();
    }

    public int cantidadRegistros() {

        return hojas.stream()
                .mapToInt(hoja -> hoja.filas().size())
                .sum();
    }
}