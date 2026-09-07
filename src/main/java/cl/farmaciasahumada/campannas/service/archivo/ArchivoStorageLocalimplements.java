package cl.farmaciasahumada.campannas.service.archivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ArchivoStorageLocalimplements implements ArchivoStorageService {

        private final Path directorioBase;

        public ArchivoStorageLocalimplements(
                        @Value("${app.storage.base-dir}") String rutaBase)
                        throws IOException {

                this.directorioBase = Paths.get(rutaBase)
                                .toAbsolutePath()
                                .normalize();

                Files.createDirectories(
                                directorioBase);
        }

        @Override
        public Path guardar(
                        MultipartFile archivo,
                        String nombreAlmacenado)
                        throws IOException {

                if (archivo == null
                                || archivo.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "El archivo es obligatorio.");
                }

                if (nombreAlmacenado == null
                                || nombreAlmacenado.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El nombre de almacenamiento es obligatorio.");
                }

                Path destino = directorioBase
                                .resolve(nombreAlmacenado)
                                .normalize();

                /*
                 * Seguridad:
                 * evita guardar archivos fuera
                 * del directorio configurado.
                 */
                if (!destino.startsWith(directorioBase)) {

                        throw new IllegalArgumentException(
                                        "Ruta de almacenamiento inválida.");
                }

                Files.copy(
                                archivo.getInputStream(),
                                destino,
                                StandardCopyOption.REPLACE_EXISTING);

                return destino;
        }

        @Override
        public byte[] leer(
                        String ruta)
                        throws IOException {

                Path archivo = obtenerRutaSegura(
                                ruta);

                return Files.readAllBytes(
                                archivo);
        }

        @Override
        public boolean existe(
                        String ruta) {

                try {

                        Path archivo = obtenerRutaSegura(
                                        ruta);

                        return Files.exists(
                                        archivo);

                } catch (IllegalArgumentException e) {

                        return false;
                }
        }

        /*
         * =========================================================
         * VALIDACIÓN DE RUTAS
         * =========================================================
         */

        private Path obtenerRutaSegura(
                        String ruta) {

                if (ruta == null
                                || ruta.isBlank()) {

                        throw new IllegalArgumentException(
                                        "La ruta del archivo es obligatoria.");
                }

                Path archivo = Paths.get(ruta)
                                .toAbsolutePath()
                                .normalize();

                if (!archivo.startsWith(
                                directorioBase)) {

                        throw new IllegalArgumentException(
                                        "Ruta de almacenamiento inválida.");
                }

                return archivo;
        }
}