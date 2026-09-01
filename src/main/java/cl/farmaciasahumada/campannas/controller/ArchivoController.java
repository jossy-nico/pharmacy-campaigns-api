package cl.farmaciasahumada.campannas.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cl.farmaciasahumada.campannas.dto.ArchivoResponse;
import cl.farmaciasahumada.campannas.service.ArchivoService;
import cl.farmaciasahumada.campannas.service.archivo.EsquemaTabularInferido;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

        private final ArchivoService archivoService;

        public ArchivoController(
                        ArchivoService archivoService) {

                this.archivoService = archivoService;
        }

        @PostMapping("/analizar")
        public ResponseEntity<?> analizar(
                        @RequestParam("archivo") MultipartFile archivo) {

                try {

                        EsquemaTabularInferido resultado = archivoService.analizar(archivo);

                        return ResponseEntity.ok(resultado);

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));

                } catch (IOException e) {

                        return ResponseEntity
                                        .internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No fue posible analizar el archivo."));
                }
        }

        @GetMapping("/datasets/{codigo}/activo")
        public ResponseEntity<?> obtenerDatasetActivo(
                        @PathVariable String codigo,
                        @RequestParam(value = "periodoReferencia", required = false) String periodoReferencia) {

                try {

                        Map<String, Object> resultado = archivoService.obtenerDatasetActivo(
                                        codigo,
                                        periodoReferencia);

                        return ResponseEntity.ok(
                                        resultado);

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @GetMapping("/datasets")
        public ResponseEntity<?> listarDatasets(
                        @RequestParam(value = "categoria", required = false) String categoria,
                        @RequestParam(value = "estado", required = false) String estado) {

                try {

                        return ResponseEntity.ok(
                                        archivoService.listarDatasets(
                                                        categoria,
                                                        estado));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @GetMapping
        public ResponseEntity<List<ArchivoResponse>> listar() {

                return ResponseEntity.ok(
                                archivoService.listar());
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> obtener(
                        @PathVariable Long id) {

                try {

                        return ResponseEntity.ok(
                                        archivoService.obtener(id));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .notFound()
                                        .build();
                }
        }

        @GetMapping("/historial/{grupoVersion}")
        public ResponseEntity<List<ArchivoResponse>> historial(
                        @PathVariable String grupoVersion) {

                return ResponseEntity.ok(
                                archivoService.historial(
                                                grupoVersion));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<?> eliminar(
                        @PathVariable Long id) {

                try {

                        archivoService.eliminar(id);

                        return ResponseEntity
                                        .noContent()
                                        .build();

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .notFound()
                                        .build();
                }
        }

        @PostMapping("/datasets")
        public ResponseEntity<?> crearDataset(@RequestParam("nombreDataset") String nombreDataset,
                        @RequestParam("archivo") MultipartFile archivo) {

                try {

                        Map<String, Object> resultado = archivoService.crearDatasetNuevo(
                                        nombreDataset,
                                        archivo);

                        return ResponseEntity.ok(
                                        resultado);

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));

                } catch (IOException e) {

                        return ResponseEntity
                                        .internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No fue posible crear el dataset."));
                }
        }

        @PatchMapping("/datasets/{codigo}/retencion")
        public ResponseEntity<?> actualizarPoliticaRetencion(
                        @PathVariable String codigo,
                        @RequestParam("politica") String politica,
                        @RequestParam(value = "maxVersiones", required = false) Integer maxVersiones) {

                try {

                        Map<String, Object> resultado = archivoService.actualizarPoliticaRetencion(
                                        codigo,
                                        politica,
                                        maxVersiones);

                        return ResponseEntity.ok(
                                        resultado);

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PostMapping("/datasets/{codigo}/cargar")
        public ResponseEntity<?> cargarDatasetExistente(
                        @PathVariable String codigo,
                        @RequestParam(value = "periodoReferencia", required = false) String periodoReferencia,
                        @RequestParam("archivo") MultipartFile archivo) {

                try {

                        Map<String, Object> resultado = archivoService.cargarDatasetExistente(
                                        codigo,
                                        periodoReferencia,
                                        archivo);

                        return ResponseEntity.ok(
                                        resultado);

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));

                } catch (IOException e) {

                        return ResponseEntity
                                        .internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No fue posible cargar el archivo."));
                }
        }

        @PatchMapping("/datasets/{codigo}/desactivar")
        public ResponseEntity<?> desactivarDataset(
                        @PathVariable String codigo) {

                try {

                        return ResponseEntity.ok(
                                        archivoService.desactivarDataset(
                                                        codigo));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PatchMapping("/datasets/{codigo}/reactivar")
        public ResponseEntity<?> reactivarDataset(
                        @PathVariable String codigo) {

                try {

                        return ResponseEntity.ok(
                                        archivoService.reactivarDataset(
                                                        codigo));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

}