package cl.farmaciasahumada.campannas.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.model.Campania;
import cl.farmaciasahumada.campannas.service.CampaniaService;

@RestController
@RequestMapping("/api/campanias")
public class CampaniaController {

        private final CampaniaService campaniaService;

        public CampaniaController(
                        CampaniaService campaniaService) {

                this.campaniaService = campaniaService;
        }

        @GetMapping
        public ResponseEntity<?> listarCampanias() {

                return ResponseEntity.ok(
                                campaniaService.listarCampanias());
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> obtenerPorId(
                        @PathVariable Long id) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.obtenerPorId(
                                                        id));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .notFound()
                                        .build();
                }
        }

        @PostMapping
        public ResponseEntity<?> crearCampania(
                        @RequestBody Campania campania) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.guardarCampania(
                                                        campania));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PutMapping("/{id}")
        public ResponseEntity<?> actualizarCampania(
                        @PathVariable Long id,
                        @RequestBody Campania campania) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.actualizar(
                                                        id,
                                                        campania));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PatchMapping("/{id}/estado")
        public ResponseEntity<?> cambiarEstado(
                        @PathVariable Long id,
                        @RequestParam("estado") String estado) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.cambiarEstado(
                                                        id,
                                                        estado));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PatchMapping("/{id}/correccion")
        public ResponseEntity<?> corregirCampania(
                        @PathVariable Long id,
                        @RequestParam("motivo") String motivo,
                        @RequestBody Campania campania) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.corregirCampania(
                                                        id,
                                                        campania,
                                                        motivo));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @GetMapping("/{id}/auditoria")
        public ResponseEntity<?> obtenerHistorialAuditoria(
                        @PathVariable Long id) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.obtenerHistorialAuditoria(
                                                        id));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PatchMapping("/{id}/extension")
        public ResponseEntity<?> extenderCampania(
                        @PathVariable Long id,
                        @RequestParam("fechaFin") LocalDate fechaFin,
                        @RequestParam("motivo") String motivo) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.extenderCampania(
                                                        id,
                                                        fechaFin,
                                                        motivo));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }

        @PostMapping("/excepcional")
        public ResponseEntity<?> crearCampaniaExcepcional(
                        @RequestParam("motivo") String motivo,
                        @RequestBody Campania campania) {

                try {

                        return ResponseEntity.ok(
                                        campaniaService.guardarCampaniaExcepcional(
                                                        campania,
                                                        motivo));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                        .badRequest()
                                        .body(Map.of(
                                                        "error",
                                                        e.getMessage()));
                }
        }
}