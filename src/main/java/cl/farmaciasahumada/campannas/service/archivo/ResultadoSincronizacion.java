package cl.farmaciasahumada.campannas.service.archivo;

public record ResultadoSincronizacion(
        int creados,
        int actualizados,
        int inactivados
) {
}