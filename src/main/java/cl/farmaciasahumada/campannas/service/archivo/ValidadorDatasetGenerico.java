package cl.farmaciasahumada.campannas.service.archivo;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicionRegla;
import cl.farmaciasahumada.campannas.repository.ArchivoDefinicionReglaRepository;

@Service
public class ValidadorDatasetGenerico {

    private static final Set<String> PLACEHOLDERS_INVALIDOS = Set.of(
            "*",
            "**",
            "-",
            "--",
            "N/A",
            "NA",
            "NULL");

    private final ArchivoDefinicionReglaRepository reglaRepository;

    public ValidadorDatasetGenerico(
            ArchivoDefinicionReglaRepository reglaRepository) {

        this.reglaRepository = reglaRepository;
    }

    public void validar(
            ArchivoDefinicion definicion,
            DocumentoTabular documento) {

        if (definicion == null) {

            throw new IllegalArgumentException(
                    "La definición del dataset es obligatoria.");
        }

        if (documento == null
                || documento.hojas() == null
                || documento.hojas().isEmpty()) {

            throw new IllegalArgumentException(
                    "El archivo no contiene información tabular válida.");
        }

        List<ArchivoDefinicionRegla> reglas = reglaRepository
                .findAllByDefinicionIdAndActivoTrueOrderByNombreCampoAsc(
                        definicion.getId());

        /*
         * Si el dataset todavía no tiene reglas configuradas,
         * el motor continúa funcionando normalmente.
         */
        if (reglas.isEmpty()) {
            return;
        }

        Set<String> columnasArchivo = obtenerColumnasDocumento(
                documento);

        validarColumnasObligatorias(
                reglas,
                columnasArchivo);

        validarColumnasAdicionales(
                definicion,
                reglas,
                columnasArchivo);

        validarValores(
                documento,
                reglas);
    }

    private Set<String> obtenerColumnasDocumento(
            DocumentoTabular documento) {

        Set<String> columnas = new HashSet<>();

        for (HojaTabular hoja : documento.hojas()) {

            for (ColumnaTabular columna : hoja.columnas()) {

                columnas.add(
                        columna.nombreCampo()
                                .toLowerCase(Locale.ROOT));
            }
        }

        return columnas;
    }

    private void validarColumnasObligatorias(
            List<ArchivoDefinicionRegla> reglas,
            Set<String> columnasArchivo) {

        for (ArchivoDefinicionRegla regla : reglas) {

            if (!Boolean.TRUE.equals(
                    regla.getColumnaObligatoria())) {

                continue;
            }

            String campo = regla
                    .getNombreCampo()
                    .toLowerCase(Locale.ROOT);

            if (!columnasArchivo.contains(
                    campo)) {

                throw new IllegalArgumentException(
                        "Falta una columna obligatoria del dataset: "
                                + regla.getNombreCampo());
            }
        }
    }

    private void validarColumnasAdicionales(
            ArchivoDefinicion definicion,
            List<ArchivoDefinicionRegla> reglas,
            Set<String> columnasArchivo) {

        if (!Boolean.FALSE.equals(
                definicion.getPermitirColumnasAdicionales())) {

            return;
        }

        Set<String> columnasConfiguradas = new HashSet<>();

        for (ArchivoDefinicionRegla regla : reglas) {

            columnasConfiguradas.add(
                    regla.getNombreCampo()
                            .toLowerCase(Locale.ROOT));
        }

        for (String columna : columnasArchivo) {

            if (!columnasConfiguradas.contains(
                    columna)) {

                throw new IllegalArgumentException(
                        "El dataset no permite columnas adicionales. "
                                + "Columna no configurada: "
                                + columna);
            }
        }
    }

    private void validarValores(
            DocumentoTabular documento,
            List<ArchivoDefinicionRegla> reglas) {

        for (HojaTabular hoja : documento.hojas()) {

            for (FilaTabularCruda fila : hoja.filas()) {

                for (ArchivoDefinicionRegla regla : reglas) {

                    if (!Boolean.TRUE.equals(
                            regla.getValorObligatorio())) {

                        continue;
                    }

                    String valor = fila
                            .valores()
                            .get(
                                    regla.getNombreCampo());

                    if (valor == null
                            || valor.isBlank()) {

                        throw new IllegalArgumentException(
                                construirErrorFila(
                                        regla,
                                        fila,
                                        "valor vacío"));
                    }

                    if (Boolean.TRUE.equals(
                            regla.getRechazarPlaceholders())
                            && PLACEHOLDERS_INVALIDOS.contains(
                                    valor.trim()
                                            .toUpperCase(Locale.ROOT))) {

                        throw new IllegalArgumentException(
                                construirErrorFila(
                                        regla,
                                        fila,
                                        "valor no válido: "
                                                + valor));
                    }
                }
            }
        }
    }

    private String construirErrorFila(
            ArchivoDefinicionRegla regla,
            FilaTabularCruda fila,
            String detalle) {

        return "Dato inválido en el campo '"
                + regla.getNombreCampo()
                + "', hoja '"
                + fila.nombreHoja()
                + "', fila "
                + fila.numeroFila()
                + ": "
                + detalle
                + ".";
    }
}