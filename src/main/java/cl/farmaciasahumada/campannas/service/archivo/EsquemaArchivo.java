package cl.farmaciasahumada.campannas.service.archivo;

import java.util.List;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicionColumna;

public record EsquemaArchivo(
        ArchivoDefinicion definicion,
        List<ArchivoDefinicionColumna> columnas
) {
}