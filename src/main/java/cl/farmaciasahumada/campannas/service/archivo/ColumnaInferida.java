package cl.farmaciasahumada.campannas.service.archivo;

public record ColumnaInferida(
        String nombreOriginal,
        String nombreCampo,
        TipoDatoTabular tipoDato,
        boolean permiteNulos,
        String formatoFecha) {
}