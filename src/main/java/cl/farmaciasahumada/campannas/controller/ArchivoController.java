package cl.farmaciasahumada.campannas.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cl.farmaciasahumada.campannas.dto.ArchivoResponse;
import cl.farmaciasahumada.campannas.model.Archivo;
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

        @PostMapping
        public ResponseEntity<?> cargar(
                        @RequestParam("tipoArchivo") String tipoArchivo,
                        @RequestParam("archivo") MultipartFile archivo) {

                try {

                        Archivo resultado = archivoService.cargar(
                                        tipoArchivo,
                                        archivo);

                        Map<String, Object> respuesta = new LinkedHashMap<>();

                        respuesta.put("id", resultado.getId());
                        respuesta.put(
                                        "tipoArchivo",
                                        resultado.getDefinicion().getCodigo());
                        respuesta.put(
                                        "nombreOriginal",
                                        resultado.getNombreOriginal());
                        respuesta.put(
                                        "version",
                                        resultado.getVersion());
                        respuesta.put(
                                        "estadoArchivo",
                                        resultado.getEstadoArchivo());
                        respuesta.put(
                                        "estadoProcesamiento",
                                        resultado.getEstadoProcesamiento());
                        respuesta.put(
                                        "tamanoBytes",
                                        resultado.getTamanoBytes());
                        respuesta.put(
                                        "hashSha256",
                                        resultado.getHashSha256());

                        return ResponseEntity.ok(respuesta);

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
                                                        "Error al almacenar el archivo."));
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

        @PutMapping("/{id}")
        public ResponseEntity<?> reemplazar(
                        @PathVariable Long id,
                        @RequestParam("archivo") MultipartFile archivo) {

                try {

                        Archivo resultado = archivoService.reemplazar(
                                        id,
                                        archivo);

                        Map<String, Object> respuesta = new LinkedHashMap<>();

                        respuesta.put(
                                        "id",
                                        resultado.getId());

                        respuesta.put(
                                        "tipoArchivo",
                                        resultado.getDefinicion().getCodigo());

                        respuesta.put(
                                        "nombreOriginal",
                                        resultado.getNombreOriginal());

                        respuesta.put(
                                        "version",
                                        resultado.getVersion());

                        respuesta.put(
                                        "estadoArchivo",
                                        resultado.getEstadoArchivo());

                        respuesta.put(
                                        "estadoProcesamiento",
                                        resultado.getEstadoProcesamiento());

                        return ResponseEntity.ok(
                                        respuesta);

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
                                                        "Error al reemplazar el archivo."));
                }
        }

        @PostMapping("/dinamico")
        public ResponseEntity<?> cargarDinamico(
                        @RequestParam("nombreTabla") String nombreTabla,
                        @RequestParam("archivo") MultipartFile archivo) {

                try {

                        Map<String, Object> resultado = archivoService.crearTablaDinamica(
                                        nombreTabla,
                                        archivo);

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
                                                        "No fue posible procesar el archivo."));
                }
        }
}