package cl.farmaciasahumada.campannas.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.model.EvidenciaFotografica;
import cl.farmaciasahumada.campannas.service.EvidenciaFotograficaService;
import cl.farmaciasahumada.campannas.service.vision.CalidadImagenService;
import cl.farmaciasahumada.campannas.service.vision.CalidadImagenService.CalidadImagenResultado;
import cl.farmaciasahumada.campannas.service.vision.VisionExhibidorService;
import cl.farmaciasahumada.campannas.service.vision.VisionExhibidorService.RegionDetectada;
import cl.farmaciasahumada.campannas.service.vision.AlineacionReferenciaService;
import cl.farmaciasahumada.campannas.service.vision.AlineacionReferenciaService.ResultadoAlineacion;

import cl.farmaciasahumada.campannas.service.ocr.OcrService;

import cl.farmaciasahumada.campannas.service.analisis.AnalisisTextoOcrService;
import cl.farmaciasahumada.campannas.service.analisis.AnalisisTextoOcrService.ResultadoNormalizacion;

@RestController
@RequestMapping("/api/vision")
public class VisionExhibidorController {

    private final VisionExhibidorService visionService;
    private final CalidadImagenService calidadImagenService;
    private final EvidenciaFotograficaService evidenciaService;
    private final AlineacionReferenciaService alineacionReferenciaService;
    private final OcrService ocrService;
    private final AnalisisTextoOcrService analisisTextoOcrService;

    public VisionExhibidorController(
            VisionExhibidorService visionService,
            CalidadImagenService calidadImagenService,
            EvidenciaFotograficaService evidenciaService,
            AlineacionReferenciaService alineacionReferenciaService,
            OcrService ocrService,
            AnalisisTextoOcrService analisisTextoOcrService) {

        this.visionService = visionService;

        this.calidadImagenService = calidadImagenService;

        this.evidenciaService = evidenciaService;
        this.alineacionReferenciaService = alineacionReferenciaService;
        this.ocrService = ocrService;
        this.analisisTextoOcrService = analisisTextoOcrService;
    }

    /*
     * =========================================================
     * DETECTAR REGIÓN PRINCIPAL
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}/detectar-region")
    public ResponseEntity<?> detectarRegion(
            @PathVariable Long evidenciaId) {

        try {

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            RegionDetectada region = visionService.detectarRegionPrincipal(
                    evidencia.getRutaAlmacenamiento());

            Map<String, Object> respuesta = new LinkedHashMap<>();

            respuesta.put(
                    "evidenciaId",
                    evidencia.getId());

            respuesta.put(
                    "tipoEvidencia",
                    evidencia.getTipoEvidencia());

            respuesta.put(
                    "exhibidor",
                    evidencia.getExhibidor());

            respuesta.put(
                    "vista",
                    evidencia.getVista());

            respuesta.put(
                    "x",
                    region.x());

            respuesta.put(
                    "y",
                    region.y());

            respuesta.put(
                    "ancho",
                    region.ancho());

            respuesta.put(
                    "alto",
                    region.alto());

            respuesta.put(
                    "proporcionArea",
                    region.proporcionArea());

            respuesta.put(
                    "rectangularidad",
                    region.rectangularidad());

            respuesta.put(
                    "relacionAspecto",
                    region.relacionAspecto());

            respuesta.put(
                    "tocaBorde",
                    region.tocaBorde());

            respuesta.put(
                    "puntaje",
                    region.puntaje());

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

    /*
     * =========================================================
     * LISTAR REGIONES CANDIDATAS
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}/candidatas")
    public ResponseEntity<?> detectarCandidatas(
            @PathVariable Long evidenciaId) {

        try {

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            List<RegionDetectada> regiones = visionService.detectarRegionesCandidatas(
                    evidencia.getRutaAlmacenamiento());

            return ResponseEntity.ok(
                    regiones);

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

    /*
     * =========================================================
     * ANALIZAR CALIDAD DE LA FOTOGRAFÍA
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}/calidad")
    public ResponseEntity<?> analizarCalidad(
            @PathVariable Long evidenciaId) {

        try {

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            CalidadImagenResultado calidad = calidadImagenService.analizar(
                    evidencia.getRutaAlmacenamiento());

            Map<String, Object> respuesta = new LinkedHashMap<>();

            respuesta.put(
                    "evidenciaId",
                    evidencia.getId());

            respuesta.put(
                    "tipoEvidencia",
                    evidencia.getTipoEvidencia());

            respuesta.put(
                    "exhibidor",
                    evidencia.getExhibidor());

            respuesta.put(
                    "vista",
                    evidencia.getVista());

            respuesta.put(
                    "ancho",
                    calidad.ancho());

            respuesta.put(
                    "alto",
                    calidad.alto());

            respuesta.put(
                    "totalPixeles",
                    calidad.totalPixeles());

            respuesta.put(
                    "nitidez",
                    calidad.nitidez());

            respuesta.put(
                    "brilloPromedio",
                    calidad.brilloPromedio());

            respuesta.put(
                    "contraste",
                    calidad.contraste());

            respuesta.put(
                    "porcentajeSobreexpuesto",
                    calidad.porcentajeSobreexpuesto());

            respuesta.put(
                    "porcentajeOscuro",
                    calidad.porcentajeOscuro());

            respuesta.put(
                    "porcentajeReflejo",
                    calidad.porcentajeReflejo());

            respuesta.put(
                    "estadoCalidad",
                    calidad.estadoCalidad());

            respuesta.put(
                    "advertencias",
                    calidad.advertencias());

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
    /*
     * =========================================================
     * IMAGEN DE DIAGNÓSTICO DE REGIONES CANDIDATAS
     *
     * Devuelve un PNG con los rectángulos detectados
     * dibujados sobre la fotografía.
     * =========================================================
     */

    @GetMapping(value = "/evidencia/{evidenciaId}/debug", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> generarDebug(
            @PathVariable Long evidenciaId) {

        try {

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            byte[] imagen = visionService.generarImagenDiagnostico(
                    evidencia.getRutaAlmacenamiento());

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.IMAGE_PNG)
                    .body(
                            imagen);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .contentType(
                            MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .internalServerError()
                    .contentType(
                            MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));
        }
    }
    /*
     * =========================================================
     * ANALIZAR ALINEACIÓN CON REFERENCIA OFICIAL
     *
     * La evidencia evaluada debe poseer:
     * referenciaOficialId
     *
     * Ejemplo:
     *
     * EVIDENCIA_FARMACIA 7
     * ↓
     * referenciaOficialId = 4
     * ↓
     * REFERENCIA_OFICIAL 4
     *
     * OpenCV compara ambas mediante ORB.
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}/alineacion")
    public ResponseEntity<?> analizarAlineacion(
            @PathVariable Long evidenciaId) {

        try {

            /*
             * Evidencia que queremos evaluar.
             */
            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            if (evidencia.getReferenciaOficialId() == null) {

                throw new IllegalArgumentException(
                        "La evidencia no posee una referencia oficial asociada.");
            }

            /*
             * Obtener automáticamente la referencia oficial.
             */
            EvidenciaFotografica referenciaOficial = evidenciaService.obtenerPorId(
                    evidencia.getReferenciaOficialId());

            /*
             * Ejecutar comparación visual ORB.
             */
            ResultadoAlineacion resultado = alineacionReferenciaService.analizar(
                    referenciaOficial.getRutaAlmacenamiento(),
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
                    referenciaOficial.getId());

            respuesta.put(
                    "exhibidor",
                    evidencia.getExhibidor());

            respuesta.put(
                    "vista",
                    evidencia.getVista());

            respuesta.put(
                    "puntosReferencia",
                    resultado.puntosReferencia());

            respuesta.put(
                    "puntosEvidencia",
                    resultado.puntosEvidencia());

            respuesta.put(
                    "coincidenciasTotales",
                    resultado.coincidenciasTotales());

            respuesta.put(
                    "coincidenciasValidas",
                    resultado.coincidenciasValidas());

            respuesta.put(
                    "porcentajeCoincidencia",
                    resultado.porcentajeCoincidencia());

            respuesta.put(
                    "coincidenciasGeometricas",
                    resultado.coincidenciasGeometricas());

            respuesta.put(
                    "porcentajeInliers",
                    resultado.porcentajeInliers());

            respuesta.put(
                    "homografiaCalculada",
                    resultado.homografiaCalculada());

            respuesta.put(
                    "alineacionPosible",
                    resultado.alineacionPosible());

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
    /*
     * =========================================================
     * DEBUG VISUAL DE ALINEACIÓN SIFT + RANSAC
     *
     * Genera una imagen PNG mostrando:
     *
     * REFERENCIA OFICIAL | EVIDENCIA
     *
     * y dibuja únicamente las coincidencias geométricas
     * aceptadas por RANSAC.
     * =========================================================
     */

    @GetMapping(value = "/evidencia/{evidenciaId}/alineacion/debug", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> generarDebugAlineacion(
            @PathVariable Long evidenciaId) {

        try {

            /*
             * =====================================================
             * 1. OBTENER EVIDENCIA
             * =====================================================
             */

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            /*
             * La evidencia zonal o farmacia debe apuntar
             * directamente a su REFERENCIA_OFICIAL.
             */
            if (evidencia.getReferenciaOficialId() == null) {

                throw new IllegalArgumentException(
                        "La evidencia no posee una referencia oficial asociada.");
            }

            /*
             * =====================================================
             * 2. OBTENER REFERENCIA OFICIAL
             * =====================================================
             */

            EvidenciaFotografica referenciaOficial = evidenciaService.obtenerPorId(
                    evidencia.getReferenciaOficialId());

            /*
             * =====================================================
             * 3. GENERAR IMAGEN DE DIAGNÓSTICO
             * =====================================================
             */

            byte[] imagen = alineacionReferenciaService.generarDiagnostico(
                    referenciaOficial.getRutaAlmacenamiento(),
                    evidencia.getRutaAlmacenamiento());

            /*
             * =====================================================
             * 4. DEVOLVER PNG
             * =====================================================
             */

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.IMAGE_PNG)
                    .header(
                            "Content-Disposition",
                            "inline; filename=alineacion-evidencia-"
                                    + evidenciaId
                                    + ".png")
                    .body(
                            imagen);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .contentType(
                            MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .internalServerError()
                    .contentType(
                            MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));
        }
    }

    /*
     * =========================================================
     * RECORTE DE REGIÓN DE BÚSQUEDA DEL EXHIBIDOR
     *
     * Devuelve únicamente la zona de la evidencia que contiene
     * aproximadamente el exhibidor localizado mediante
     * SIFT + RANSAC.
     *
     * Funciona tanto para:
     *
     * - EVIDENCIA_ZONAL
     * - EVIDENCIA_FARMACIA
     *
     * Ambas se comparan siempre contra REFERENCIA_OFICIAL.
     * =========================================================
     */

    @GetMapping(value = "/evidencia/{evidenciaId}/recorte", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> generarRecorte(
            @PathVariable Long evidenciaId) {

        try {

            /*
             * 1. Obtener evidencia a evaluar.
             */
            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            /*
             * Una evidencia ZONAL o FARMACIA debe apuntar
             * directamente a su REFERENCIA_OFICIAL.
             */
            if (evidencia.getReferenciaOficialId() == null) {

                throw new IllegalArgumentException(
                        "La evidencia no posee una referencia oficial asociada.");
            }

            /*
             * 2. Obtener referencia oficial.
             */
            EvidenciaFotografica referenciaOficial = evidenciaService.obtenerPorId(
                    evidencia.getReferenciaOficialId());

            /*
             * 3. Generar el recorte.
             */
            byte[] imagen = alineacionReferenciaService
                    .generarRecorteRegionBusqueda(
                            referenciaOficial
                                    .getRutaAlmacenamiento(),
                            evidencia
                                    .getRutaAlmacenamiento());

            /*
             * 4. Retornar PNG.
             */
            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.IMAGE_PNG)
                    .header(
                            "Content-Disposition",
                            "inline; filename=recorte-evidencia-"
                                    + evidenciaId
                                    + ".png")
                    .body(
                            imagen);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .contentType(
                            MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .internalServerError()
                    .contentType(
                            MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));
        }
    }
    /*
     * =========================================================
     * OCR SOBRE RECORTE DEL EXHIBIDOR
     *
     * Flujo:
     *
     * EVIDENCIA
     * ↓
     * REFERENCIA_OFICIAL
     * ↓
     * SIFT + RANSAC
     * ↓
     * RECORTE
     * ↓
     * OCR
     *
     * Funciona tanto para:
     *
     * - EVIDENCIA_ZONAL
     * - EVIDENCIA_FARMACIA
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}/recorte/ocr")
    public ResponseEntity<?> procesarOcrRecorte(
            @PathVariable Long evidenciaId) {

        try {

            /*
             * =====================================================
             * 1. OBTENER EVIDENCIA
             * =====================================================
             */

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            /*
             * Zonal y farmacia siempre se comparan
             * directamente contra REFERENCIA_OFICIAL.
             */
            if (evidencia.getReferenciaOficialId() == null) {

                throw new IllegalArgumentException(
                        "La evidencia no posee una referencia oficial asociada.");
            }

            /*
             * =====================================================
             * 2. OBTENER REFERENCIA OFICIAL
             * =====================================================
             */

            EvidenciaFotografica referenciaOficial = evidenciaService.obtenerPorId(
                    evidencia.getReferenciaOficialId());

            /*
             * =====================================================
             * 3. GENERAR RECORTE
             * =====================================================
             */

            byte[] recorte = alineacionReferenciaService
                    .generarRecorteRegionBusqueda(
                            referenciaOficial
                                    .getRutaAlmacenamiento(),
                            evidencia
                                    .getRutaAlmacenamiento());

            /*
             * =====================================================
             * 4. OCR SOBRE EL RECORTE
             * =====================================================
             */

            String textoOcr = ocrService.extraerTexto(
                    recorte);

            /*
             * =====================================================
             * 5. RESPUESTA
             * =====================================================
             */

            Map<String, Object> respuesta = new LinkedHashMap<>();

            respuesta.put(
                    "evidenciaId",
                    evidencia.getId());

            respuesta.put(
                    "tipoEvidencia",
                    evidencia.getTipoEvidencia());

            respuesta.put(
                    "referenciaOficialId",
                    referenciaOficial.getId());

            respuesta.put(
                    "exhibidor",
                    evidencia.getExhibidor());

            respuesta.put(
                    "vista",
                    evidencia.getVista());

            respuesta.put(
                    "recorteGenerado",
                    true);

            respuesta.put(
                    "textoOcr",
                    textoOcr);

            return ResponseEntity.ok(
                    respuesta);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));
        }
    }

    /*
     * =========================================================
     * ANALIZAR TEXTO OCR DEL RECORTE
     *
     * Flujo:
     *
     * evidencia
     * ↓
     * referencia oficial
     * ↓
     * SIFT + RANSAC
     * ↓
     * recorte
     * ↓
     * OCR multivariante
     * ↓
     * normalización de texto
     *
     * Todavía NO determina cumplimiento.
     * =========================================================
     */

    @PostMapping("/evidencia/{evidenciaId}/recorte/ocr/analisis")
    public ResponseEntity<?> analizarTextoOcrRecorte(
            @PathVariable Long evidenciaId) {

        try {

            /*
             * =====================================================
             * 1. OBTENER EVIDENCIA
             * =====================================================
             */

            EvidenciaFotografica evidencia = evidenciaService.obtenerPorId(
                    evidenciaId);

            if (evidencia.getReferenciaOficialId() == null) {

                throw new IllegalArgumentException(
                        "La evidencia no posee una referencia oficial asociada.");
            }

            /*
             * =====================================================
             * 2. OBTENER REFERENCIA OFICIAL
             * =====================================================
             */

            EvidenciaFotografica referenciaOficial = evidenciaService.obtenerPorId(
                    evidencia.getReferenciaOficialId());

            /*
             * =====================================================
             * 3. GENERAR RECORTE
             * =====================================================
             */

            byte[] recorte = alineacionReferenciaService
                    .generarRecorteRegionBusqueda(
                            referenciaOficial
                                    .getRutaAlmacenamiento(),
                            evidencia
                                    .getRutaAlmacenamiento());

            /*
             * =====================================================
             * 4. OCR
             * =====================================================
             */

            String textoOcr = ocrService.extraerTexto(
                    recorte);

            /*
             * =====================================================
             * 5. NORMALIZAR TEXTO OCR
             * =====================================================
             */

            ResultadoNormalizacion normalizado = analisisTextoOcrService
                    .normalizar(
                            textoOcr);

            /*
             * =====================================================
             * 6. RESPUESTA
             * =====================================================
             */

            Map<String, Object> respuesta = new LinkedHashMap<>();

            respuesta.put(
                    "evidenciaId",
                    evidencia.getId());

            respuesta.put(
                    "tipoEvidencia",
                    evidencia.getTipoEvidencia());

            respuesta.put(
                    "referenciaOficialId",
                    referenciaOficial.getId());

            respuesta.put(
                    "exhibidor",
                    evidencia.getExhibidor());

            respuesta.put(
                    "vista",
                    evidencia.getVista());

            respuesta.put(
                    "cantidadTokens",
                    normalizado.cantidadTokens());

            respuesta.put(
                    "tokens",
                    normalizado.tokens());

            respuesta.put(
                    "lineas",
                    normalizado.lineas());

            respuesta.put(
                    "textoNormalizado",
                    normalizado.textoNormalizado());

            return ResponseEntity.ok(
                    respuesta);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()));
        }
    }
}