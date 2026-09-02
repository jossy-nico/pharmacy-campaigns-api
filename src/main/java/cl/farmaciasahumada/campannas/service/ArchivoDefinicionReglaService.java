package cl.farmaciasahumada.campannas.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicionRegla;
import cl.farmaciasahumada.campannas.repository.ArchivoDefinicionReglaRepository;
import cl.farmaciasahumada.campannas.repository.ArchivoDefinicionRepository;

@Service
public class ArchivoDefinicionReglaService {

    private final ArchivoDefinicionRepository definicionRepository;
    private final ArchivoDefinicionReglaRepository reglaRepository;

    public ArchivoDefinicionReglaService(
            ArchivoDefinicionRepository definicionRepository,
            ArchivoDefinicionReglaRepository reglaRepository) {

        this.definicionRepository = definicionRepository;
        this.reglaRepository = reglaRepository;
    }

    public Map<String, Object> listarReglas(
            String codigoDataset) {

        ArchivoDefinicion definicion = obtenerDefinicion(
                codigoDataset);

        List<ArchivoDefinicionRegla> reglas = reglaRepository
                .findAllByDefinicionIdAndActivoTrueOrderByNombreCampoAsc(
                        definicion.getId());

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put(
                "codigoDataset",
                definicion.getCodigo());

        respuesta.put(
                "nombreDataset",
                definicion.getNombre());

        respuesta.put(
                "permitirColumnasAdicionales",
                definicion.getPermitirColumnasAdicionales());

        respuesta.put(
                "reglas",
                reglas.stream()
                        .map(this::convertirRegla)
                        .toList());

        return respuesta;
    }

    @Transactional
    public Map<String, Object> guardarRegla(
            String codigoDataset,
            String nombreCampo,
            Boolean columnaObligatoria,
            Boolean valorObligatorio,
            Boolean rechazarPlaceholders) {

        ArchivoDefinicion definicion = obtenerDefinicion(
                codigoDataset);

        String campoNormalizado = normalizarNombreCampo(
                nombreCampo);

        ArchivoDefinicionRegla regla = reglaRepository
                .findByDefinicionIdAndNombreCampoIgnoreCase(
                        definicion.getId(),
                        campoNormalizado)
                .orElseGet(
                        ArchivoDefinicionRegla::new);

        regla.setDefinicion(
                definicion);

        regla.setNombreCampo(
                campoNormalizado);

        regla.setColumnaObligatoria(
                columnaObligatoria != null
                        ? columnaObligatoria
                        : true);

        regla.setValorObligatorio(
                valorObligatorio != null
                        ? valorObligatorio
                        : true);

        regla.setRechazarPlaceholders(
                rechazarPlaceholders != null
                        ? rechazarPlaceholders
                        : true);

        /*
         * Si una regla existía pero estaba desactivada,
         * al volver a guardarla queda activa nuevamente.
         */
        regla.setActivo(
                true);

        ArchivoDefinicionRegla guardada = reglaRepository.saveAndFlush(
                regla);

        return convertirRegla(
                guardada);
    }

    @Transactional
    public Map<String, Object> desactivarRegla(
            String codigoDataset,
            String nombreCampo) {

        ArchivoDefinicion definicion = obtenerDefinicion(
                codigoDataset);

        String campoNormalizado = normalizarNombreCampo(
                nombreCampo);

        ArchivoDefinicionRegla regla = reglaRepository
                .findByDefinicionIdAndNombreCampoIgnoreCase(
                        definicion.getId(),
                        campoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una regla para el campo '"
                                + campoNormalizado
                                + "' en el dataset "
                                + definicion.getCodigo()
                                + "."));

        if (!Boolean.TRUE.equals(
                regla.getActivo())) {

            throw new IllegalArgumentException(
                    "La regla del campo '"
                            + campoNormalizado
                            + "' ya se encuentra inactiva.");
        }

        regla.setActivo(
                false);

        ArchivoDefinicionRegla guardada = reglaRepository.saveAndFlush(
                regla);

        return convertirRegla(
                guardada);
    }

    @Transactional
    public Map<String, Object> actualizarPermitirColumnasAdicionales(
            String codigoDataset,
            Boolean permitir) {

        if (permitir == null) {

            throw new IllegalArgumentException(
                    "Debe indicar si el dataset permite "
                            + "columnas adicionales.");
        }

        ArchivoDefinicion definicion = obtenerDefinicion(
                codigoDataset);

        definicion.setPermitirColumnasAdicionales(
                permitir);

        ArchivoDefinicion guardada = definicionRepository.saveAndFlush(
                definicion);

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put(
                "codigoDataset",
                guardada.getCodigo());

        respuesta.put(
                "permitirColumnasAdicionales",
                guardada.getPermitirColumnasAdicionales());

        return respuesta;
    }

    private ArchivoDefinicion obtenerDefinicion(
            String codigoDataset) {

        if (codigoDataset == null
                || codigoDataset.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar el código del dataset.");
        }

        String codigoNormalizado = codigoDataset
                .trim()
                .toUpperCase(Locale.ROOT);

        return definicionRepository
                .findByCodigoIgnoreCase(
                        codigoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El dataset '"
                                + codigoNormalizado
                                + "' no está registrado."));
    }

    private String normalizarNombreCampo(
            String nombreCampo) {

        if (nombreCampo == null
                || nombreCampo.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del campo es obligatorio.");
        }

        String normalizado = Normalizer.normalize(
                nombreCampo,
                Normalizer.Form.NFD)
                .replaceAll(
                        "\\p{M}",
                        "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll(
                        "[^a-z0-9]+",
                        "_")
                .replaceAll(
                        "^_+|_+$",
                        "")
                .replaceAll(
                        "_+",
                        "_");

        if (!normalizado.matches(
                "^[a-z][a-z0-9_]*$")) {

            throw new IllegalArgumentException(
                    "El nombre del campo no es válido. "
                            + "Debe comenzar con una letra y solo puede "
                            + "contener letras, números y guión bajo.");
        }

        return normalizado;
    }

    private Map<String, Object> convertirRegla(
            ArchivoDefinicionRegla regla) {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put(
                "id",
                regla.getId());

        respuesta.put(
                "nombreCampo",
                regla.getNombreCampo());

        respuesta.put(
                "columnaObligatoria",
                regla.getColumnaObligatoria());

        respuesta.put(
                "valorObligatorio",
                regla.getValorObligatorio());

        respuesta.put(
                "rechazarPlaceholders",
                regla.getRechazarPlaceholders());

        respuesta.put(
                "activo",
                regla.getActivo());

        return respuesta;
    }
}