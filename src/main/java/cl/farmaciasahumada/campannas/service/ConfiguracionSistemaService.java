package cl.farmaciasahumada.campannas.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import cl.farmaciasahumada.campannas.model.ConfiguracionSistema;
import cl.farmaciasahumada.campannas.repository.ConfiguracionSistemaRepository;
import jakarta.transaction.Transactional;

@Service
public class ConfiguracionSistemaService {

    private static final String CLAVE_DIA_LIMITE_CAMPANIA = "DIA_LIMITE_CARGA_CAMPANIA";

    private final ConfiguracionSistemaRepository configuracionRepository;

    public ConfiguracionSistemaService(
            ConfiguracionSistemaRepository configuracionRepository) {

        this.configuracionRepository = configuracionRepository;
    }

    public Map<String, Object> obtenerDiaLimiteCargaCampania() {

        ConfiguracionSistema configuracion = configuracionRepository
                .findByClaveIgnoreCase(
                        CLAVE_DIA_LIMITE_CAMPANIA)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El día límite de carga de campañas "
                                + "todavía no ha sido configurado."));

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put(
                "clave",
                configuracion.getClave());

        respuesta.put(
                "diaLimite",
                Integer.parseInt(
                        configuracion.getValor()));

        respuesta.put(
                "descripcion",
                configuracion.getDescripcion());

        respuesta.put(
                "fechaModificacion",
                configuracion.getFechaModificacion());

        return respuesta;
    }

    @Transactional
    public Map<String, Object> actualizarDiaLimiteCargaCampania(
            Integer diaLimite) {

        validarDiaLimite(
                diaLimite);

        ConfiguracionSistema configuracion = configuracionRepository
                .findByClaveIgnoreCase(
                        CLAVE_DIA_LIMITE_CAMPANIA)
                .orElseGet(() -> {

                    ConfiguracionSistema nueva = new ConfiguracionSistema();

                    nueva.setClave(
                            CLAVE_DIA_LIMITE_CAMPANIA);

                    nueva.setDescripcion(
                            "Día límite del mes anterior "
                                    + "para cargar la campaña "
                                    + "del mes siguiente.");

                    return nueva;
                });

        configuracion.setValor(
                diaLimite.toString());

        configuracion.setFechaModificacion(
                LocalDateTime.now());

        ConfiguracionSistema guardada = configuracionRepository.saveAndFlush(
                configuracion);

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put(
                "clave",
                guardada.getClave());

        respuesta.put(
                "diaLimite",
                Integer.parseInt(
                        guardada.getValor()));

        respuesta.put(
                "descripcion",
                guardada.getDescripcion());

        respuesta.put(
                "fechaModificacion",
                guardada.getFechaModificacion());

        return respuesta;
    }

    private void validarDiaLimite(
            Integer diaLimite) {

        if (diaLimite == null) {

            throw new IllegalArgumentException(
                    "El día límite es obligatorio.");
        }

        if (diaLimite < 1
                || diaLimite > 31) {

            throw new IllegalArgumentException(
                    "El día límite debe estar entre 1 y 31.");
        }
    }
}
