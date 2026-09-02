package cl.farmaciasahumada.campannas.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.service.ConfiguracionSistemaService;

@RestController
@RequestMapping("/api/configuraciones")
public class ConfiguracionSistemaController {

    private final ConfiguracionSistemaService configuracionSistemaService;

    public ConfiguracionSistemaController(
            ConfiguracionSistemaService configuracionSistemaService) {

        this.configuracionSistemaService = configuracionSistemaService;
    }

    @GetMapping("/campanias/dia-limite-carga")
    public ResponseEntity<Object> obtenerDiaLimiteCargaCampania() {

        try {

            return ResponseEntity.ok(
                    configuracionSistemaService
                            .obtenerDiaLimiteCargaCampania());

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @PatchMapping("/campanias/dia-limite-carga")
    public ResponseEntity<Object> actualizarDiaLimiteCargaCampania(
            @RequestParam("dia") Integer dia) {

        try {

            return ResponseEntity.ok(
                    configuracionSistemaService
                            .actualizarDiaLimiteCargaCampania(
                                    dia));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }
}