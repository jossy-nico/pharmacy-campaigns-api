package cl.farmaciasahumada.campannas.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.service.ArchivoDefinicionReglaService;

@RestController
@RequestMapping("/api/admin/datasets")
public class ArchivoDefinicionReglaController {

    private final ArchivoDefinicionReglaService reglaService;

    public ArchivoDefinicionReglaController(
            ArchivoDefinicionReglaService reglaService) {

        this.reglaService = reglaService;
    }

    @GetMapping("/reglas")
    public ResponseEntity<?> listarReglas(
            @RequestParam("dataset") String codigoDataset) {

        try {

            return ResponseEntity.ok(
                    reglaService.listarReglas(
                            codigoDataset));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @PutMapping("/reglas")
    public ResponseEntity<?> guardarRegla(
            @RequestParam("dataset") String codigoDataset,
            @RequestParam("campo") String nombreCampo,
            @RequestParam(
                    value = "columnaObligatoria",
                    required = false) Boolean columnaObligatoria,
            @RequestParam(
                    value = "valorObligatorio",
                    required = false) Boolean valorObligatorio,
            @RequestParam(
                    value = "rechazarPlaceholders",
                    required = false) Boolean rechazarPlaceholders) {

        try {

            return ResponseEntity.ok(
                    reglaService.guardarRegla(
                            codigoDataset,
                            nombreCampo,
                            columnaObligatoria,
                            valorObligatorio,
                            rechazarPlaceholders));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @PatchMapping("/reglas/desactivar")
    public ResponseEntity<?> desactivarRegla(
            @RequestParam("dataset") String codigoDataset,
            @RequestParam("campo") String nombreCampo) {

        try {

            return ResponseEntity.ok(
                    reglaService.desactivarRegla(
                            codigoDataset,
                            nombreCampo));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @PatchMapping("/columnas-adicionales")
    public ResponseEntity<?> actualizarColumnasAdicionales(
            @RequestParam("dataset") String codigoDataset,
            @RequestParam("permitir") Boolean permitir) {

        try {

            return ResponseEntity.ok(
                    reglaService
                            .actualizarPermitirColumnasAdicionales(
                                    codigoDataset,
                                    permitir));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }
}