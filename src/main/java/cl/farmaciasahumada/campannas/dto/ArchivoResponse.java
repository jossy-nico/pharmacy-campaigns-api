package cl.farmaciasahumada.campannas.dto;

import java.time.LocalDateTime;

public record ArchivoResponse(
        Long id,
        String tipoArchivo,
        String nombreOriginal,
        Integer version,
        String estadoArchivo,
        String estadoProcesamiento,
        Long tamanoBytes,
        String hashSha256,
        String origen,
        LocalDateTime fechaCarga
) {
}