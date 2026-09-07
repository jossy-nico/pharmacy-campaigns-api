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
         * CARGA DE REFERENCIA OFICIAL DEL PLANOGRAMA
         * =========================================================
         */

        @PostMapping("/referencias-oficiales")
        public ResponseEntity<?> subirReferenciaOficial(
                        @RequestParam("campaniaId") Long campaniaId,
                        @RequestParam("exhibidor") String exhibidor,
                        @RequestParam("vista") String vista,
                        @RequestParam("imagen") MultipartFile imagen,
                        @RequestParam(value = "observacion", required = false) String observacion) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.subirReferenciaOficial(
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
                                                        "No fue posible almacenar la referencia oficial."));
                }
        }

        /*
         * =========================================================
         * CARGA DE EVIDENCIA ZONAL
         * =========================================================
         */

        @PostMapping("/zonales")
        public ResponseEntity<?> subirEvidenciaZonal(
                        @RequestParam("campaniaId") Long campaniaId,
                        @RequestParam("farmaciaId") Long farmaciaId,
                        @RequestParam("referenciaOficialId") Long referenciaOficialId,
                        @RequestParam("imagen") MultipartFile imagen,
                        @RequestParam(value = "observacion", required = false) String observacion) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.subirEvidenciaZonal(
                                                        campaniaId,
                                                        farmaciaId,
                                                        referenciaOficialId,
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
                                                        "No fue posible almacenar la evidencia zonal."));
                }
        }

        /*
         * =========================================================
         * CARGA DE EVIDENCIA DE FARMACIA
         * =========================================================
         */

        @PostMapping("/farmacias")
        public ResponseEntity<?> subirEvidenciaFarmacia(
                        @RequestParam("campaniaId") Long campaniaId,
                        @RequestParam("farmaciaId") Long farmaciaId,
                        @RequestParam("referenciaOficialId") Long referenciaOficialId,
                        @RequestParam("imagen") MultipartFile imagen,
                        @RequestParam(value = "observacion", required = false) String observacion) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.subirEvidenciaFarmacia(
                                                        campaniaId,
                                                        farmaciaId,
                                                        referenciaOficialId,
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
                                                        "No fue posible almacenar la evidencia de farmacia."));
                }
        }

        /*
         * =========================================================
         * OBTENER EVIDENCIA POR ID
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

        /*
         * =========================================================
         * LISTAR TODAS LAS EVIDENCIAS DE UNA CAMPAÑA
         * =========================================================
         */

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

        /*
         * =========================================================
         * LISTAR REFERENCIAS OFICIALES
         * =========================================================
         */

        @GetMapping("/campania/{campaniaId}/referencias-oficiales")
        public ResponseEntity<?> listarReferenciasOficiales(
                        @PathVariable Long campaniaId) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.listarReferenciasOficiales(
                                                        campaniaId));

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
         * LISTAR EVIDENCIAS ZONALES
         * =========================================================
         */

        @GetMapping("/campania/{campaniaId}/zonales")
        public ResponseEntity<?> listarEvidenciasZonales(
                        @PathVariable Long campaniaId) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.listarEvidenciasZonales(
                                                        campaniaId));

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
         * LISTAR EVIDENCIAS DE FARMACIA
         * =========================================================
         */

        @GetMapping("/campania/{campaniaId}/farmacias")
        public ResponseEntity<?> listarEvidenciasFarmacia(
                        @PathVariable Long campaniaId) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.listarEvidenciasFarmacia(
                                                        campaniaId));

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
         * LISTAR EVIDENCIAS DE UNA FARMACIA
         * =========================================================
         */

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

        /*
         * =========================================================
         * LISTAR EVIDENCIAS ASOCIADAS A REFERENCIA OFICIAL
         * =========================================================
         */

        @GetMapping("/referencia-oficial/{referenciaOficialId}/evidencias")
        public ResponseEntity<?> listarPorReferenciaOficial(
                        @PathVariable Long referenciaOficialId) {

                try {

                        return ResponseEntity.ok(
                                        evidenciaService.listarPorReferenciaOficial(
                                                        referenciaOficialId));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }
}