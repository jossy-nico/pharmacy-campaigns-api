package cl.farmaciasahumada.campannas.service.archivo;

import java.util.LinkedHashMap;
import java.util.Map;

public record FilaTabularCruda(
        String nombreHoja,
        int numeroFila,
        Map<String, String> valores) {

    public FilaTabularCruda {
        valores = Map.copyOf(
                new LinkedHashMap<>(valores));
    }

    /*
     * Constructor temporal para mantener compatibilidad
     * con código que todavía no informa la hoja.
     */
    public FilaTabularCruda(
            int numeroFila,
            Map<String, String> valores) {

        this(
                null,
                numeroFila,
                valores);
    }
}