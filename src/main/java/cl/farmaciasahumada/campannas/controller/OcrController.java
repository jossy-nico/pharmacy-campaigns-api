package cl.farmaciasahumada.campannas.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.model.EvidenciaFotografica;
import cl.farmaciasahumada.campannas.service.EvidenciaFotograficaService;
import cl.farmaciasahumada.campannas.service.ocr.OcrService;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;
    private final EvidenciaFotograficaService evidenciaService;

    public OcrController(
            OcrService ocrService,
            EvidenciaFotograficaService evidenciaService) {

        this.ocrService = ocrService;
        this.evidenciaService = evidenciaService;
    }

    /*
     * =========================================================
     * PROCESAR OCR DE UNA EVIDENCIA
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}")
    public ResponseEntity<?> procesarEvidencia(
            @PathVariable Long evidenciaId) {

        try {

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            String textoDetectado = ocrService.extraerTexto(
                    evidencia.getRutaAlmacenamiento());

            Map<String, Object> respuesta = new LinkedHashMap<>();

            respuesta.put(
                    "evidenciaId",
                    evidencia.getId());

            respuesta.put(
                    "tipoEvidencia",
                    evidencia.getTipoEvidencia());

            respuesta.put(
                    "referenciaOficialId",
                    evidencia.getReferenciaOficialId());

            respuesta.put(
                    "exhibidor",
                    evidencia.getExhibidor());

            respuesta.put(
                    "vista",
                    evidencia.getVista());

            respuesta.put(
                    "textoDetectado",
                    textoDetectado);

            return ResponseEntity.ok(
                    respuesta);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }
}