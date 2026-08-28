package cl.farmaciasahumada.campannas.service.archivo;

import java.util.List;

public record HojaTabular(
        String nombre,
        int filaEncabezado,
        List<ColumnaTabular> columnas,
        List<FilaTabularCruda> filas) {

    public HojaTabular {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la hoja es obligatorio.");
        }

        columnas = List.copyOf(columnas);
        filas = List.copyOf(filas);
    }
}