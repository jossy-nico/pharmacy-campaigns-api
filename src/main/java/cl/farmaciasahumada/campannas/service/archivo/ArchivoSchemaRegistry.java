package cl.farmaciasahumada.campannas.service.archivo;

import java.util.List;

import org.springframework.stereotype.Component;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicionColumna;
import cl.farmaciasahumada.campannas.repository.ArchivoDefinicionColumnaRepository;
import cl.farmaciasahumada.campannas.repository.ArchivoDefinicionRepository;

@Component
public class ArchivoSchemaRegistry {
    private final ArchivoDefinicionRepository definicionRepository;
    private final ArchivoDefinicionColumnaRepository columnaRepository;

    public ArchivoSchemaRegistry(ArchivoDefinicionRepository definicionRepository, ArchivoDefinicionColumnaRepository columnaRepository){

            this.definicionRepository = definicionRepository;
            this.columnaRepository = columnaRepository;

    }

    public EsquemaArchivo obtener(String codigo){
        ArchivoDefinicion definicion = definicionRepository.findByCodigoAndActivoTrue(codigo)
        .orElseThrow(() -> new IllegalArgumentException("No existe una definición activa para: " + codigo));

        List<ArchivoDefinicionColumna> columnas = columnaRepository.findByDefinicionIdAndActivoTrueOrderByOrdenAsc(definicion.getId());

        return new EsquemaArchivo(definicion, columnas);
    }

}
