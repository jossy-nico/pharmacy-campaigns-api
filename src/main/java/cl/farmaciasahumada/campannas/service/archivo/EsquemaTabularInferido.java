package cl.farmaciasahumada.campannas.service.archivo;

import java.util.List;

public record EsquemaTabularInferido(
        String nombreHoja,
        int filaEncabezado,
        int cantidadRegistros,
        List<ColumnaInferida> columnas) {

    public EsquemaTabularInferido {
        columnas = List.copyOf(columnas);
    }
}