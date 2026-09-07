package cl.farmaciasahumada.campannas.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.service.AnalisisEvidenciaService;

@RestController
@RequestMapping("/api/analisis-evidencias")
public class AnalisisEvidenciaController {

    private final AnalisisEvidenciaService analisisService;

    public AnalisisEvidenciaController(
            AnalisisEvidenciaService analisisService) {

        this.analisisService = analisisService;
    }

    /*
     * =========================================================
     * CREAR ANÁLISIS DE UNA EVIDENCIA
     *
     * Permite analizar:
     * - EVIDENCIA_ZONAL
     * - EVIDENCIA_FARMACIA
     *
     * Ambas se comparan contra su REFERENCIA_OFICIAL.
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaEvaluadaId}")
    public ResponseEntity<?> crearAnalisis(
            @PathVariable Long evidenciaEvaluadaId) {

        try {

            return ResponseEntity.ok(
                    analisisService.crearAnalisis(
                            evidenciaEvaluadaId));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    /*
     * =========================================================
     * OBTENER ANÁLISIS POR ID
     * =========================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    analisisService.obtenerPorId(
                            id));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    /*
     * =========================================================
     * LISTAR ANÁLISIS DE UNA EVIDENCIA
     * =========================================================
     */

    @GetMapping("/evidencia/{evidenciaEvaluadaId}")
    public ResponseEntity<?> listarPorEvidencia(
            @PathVariable Long evidenciaEvaluadaId) {

        try {

            return ResponseEntity.ok(
                    analisisService.listarPorEvidencia(
                            evidenciaEvaluadaId));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }
}