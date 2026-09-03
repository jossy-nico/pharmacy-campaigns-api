package cl.farmaciasahumada.campannas.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.farmaciasahumada.campannas.service.EvidenciaFotograficaService;

@RestController
@RequestMapping("/api/evidencias")
public class EvidenciaFotograficaController {

    private final EvidenciaFotograficaService evidenciaService;

    public EvidenciaFotograficaController(
            EvidenciaFotograficaService evidenciaService) {

        this.evidenciaService = evidenciaService;
    }

    /*
     * =========================================================
     * CARGA DE FOTO DE REFERENCIA ZONAL
     * =========================================================
     */

    @PostMapping("/referencias")
    public ResponseEntity<?> subirReferenciaZonal(
            @RequestParam("campaniaId") Long campaniaId,
            @RequestParam("exhibidor") String exhibidor,
            @RequestParam("vista") String vista,
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam(value = "observacion", required = false) String observacion) {

        try {

            return ResponseEntity.ok(
                    evidenciaService.subirReferenciaZonal(
                            campaniaId,
                            exhibidor,
                            vista,
                            imagen,
                            observacion));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "error",
                            "No fue posible almacenar la fotografía."));
        }
    }

    /*
     * =========================================================
     * CARGA DE FOTO DE FARMACIA
     * =========================================================
     */

    @PostMapping("/farmacias")
    public ResponseEntity<?> subirEvidenciaFarmacia(
            @RequestParam("campaniaId") Long campaniaId,
            @RequestParam("farmaciaId") Long farmaciaId,
            @RequestParam("referenciaZonalId") Long referenciaZonalId,
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam(value = "observacion", required = false) String observacion) {

        try {

            return ResponseEntity.ok(
                    evidenciaService.subirEvidenciaFarmacia(
                            campaniaId,
                            farmaciaId,
                            referenciaZonalId,
                            imagen,
                            observacion));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "error",
                            "No fue posible almacenar la fotografía."));
        }
    }

    /*
     * =========================================================
     * CONSULTAS
     * =========================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    evidenciaService.obtenerPorId(
                            id));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @GetMapping("/campania/{campaniaId}")
    public ResponseEntity<?> listarPorCampania(
            @PathVariable Long campaniaId) {

        try {

            return ResponseEntity.ok(
                    evidenciaService.listarPorCampania(
                            campaniaId));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @GetMapping("/campania/{campaniaId}/referencias")
    public ResponseEntity<?> listarReferenciasZonales(
            @PathVariable Long campaniaId) {

        try {

            return ResponseEntity.ok(
                    evidenciaService.listarReferenciasZonales(
                            campaniaId));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @GetMapping("/campania/{campaniaId}/farmacia/{farmaciaId}")
    public ResponseEntity<?> listarPorFarmacia(
            @PathVariable Long campaniaId,
            @PathVariable Long farmaciaId) {

        try {

            return ResponseEntity.ok(
                    evidenciaService.listarPorFarmacia(
                            campaniaId,
                            farmaciaId));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }
}