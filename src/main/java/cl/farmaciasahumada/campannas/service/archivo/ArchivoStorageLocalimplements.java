package cl.farmaciasahumada.campannas.service.archivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ArchivoStorageLocalimplements implements ArchivoStorageService {

    private final Path directorioBase =
            Paths.get("storage")
                    .toAbsolutePath()
                    .normalize();

    public ArchivoStorageLocalimplements() throws IOException {
        Files.createDirectories(directorioBase);
    }

    @Override
    public Path guardar(
            MultipartFile archivo,
            String nombreAlmacenado
    ) throws IOException {

        Path destino =
                directorioBase
                        .resolve(nombreAlmacenado)
                        .normalize();

        /*
         * Seguridad:
         * evita intentar guardar fuera de /storage
         */
        if (!destino.startsWith(directorioBase)) {
            throw new IllegalArgumentException(
                    "Ruta de almacenamiento inválida."
            );
        }

        Files.copy(
                archivo.getInputStream(),
                destino,
                StandardCopyOption.REPLACE_EXISTING
        );

        return destino;
    }

    @Override
    public byte[] leer(String ruta)
            throws IOException {

        Path archivo =
                Paths.get(ruta)
                        .toAbsolutePath()
                        .normalize();

        if (!archivo.startsWith(directorioBase)) {
            throw new IllegalArgumentException(
                    "Ruta de almacenamiento inválida."
            );
        }

        return Files.readAllBytes(archivo);
    }

    @Override
    public boolean existe(String ruta) {

        Path archivo =
                Paths.get(ruta)
                        .toAbsolutePath()
                        .normalize();

        return archivo.startsWith(directorioBase)
                && Files.exists(archivo);
    }
}
    
