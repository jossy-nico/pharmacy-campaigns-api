package cl.farmaciasahumada.campannas.service.archivo;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public interface ArchivoStorageService {
    Path guardar(
        MultipartFile archivo,
        String nombreAlmacenado
    ) throws IOException;

    byte[] leer(String rute) throws IOException;

    boolean existe(String ruta);

}
