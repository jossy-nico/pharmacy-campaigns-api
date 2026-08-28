package cl.farmaciasahumada.campannas.service.archivo;

import java.util.List;
import java.util.Map;

public record FilaTabular(
    Integer numeroFila,
    String claveNegocio,
    Map<String, Object> datos,
    Map<String, Object> datosAdicionales,
    List<String> errores
) {
    public Boolean esValida(){
        return errores == null || errores.isEmpty();
    }
}
