package cl.farmaciasahumada.campannas.controller;

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

import cl.farmaciasahumada.campannas.model.Farmacia;
import cl.farmaciasahumada.campannas.service.FarmaciaService;

@RestController
@RequestMapping("/api/farmacias")
public class FarmaciaController {

    private final FarmaciaService farmaciaService;

    public FarmaciaController(
            FarmaciaService farmaciaService) {

        this.farmaciaService = farmaciaService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {

        return ResponseEntity.ok(
                farmaciaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    farmaciaService.obtenerPorId(id));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> obtenerPorCodigo(
            @PathVariable String codigo) {

        try {

            return ResponseEntity.ok(
                    farmaciaService.obtenerPorCodigo(
                            codigo));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody Farmacia farmacia) {

        try {

            return ResponseEntity.ok(
                    farmaciaService.crear(
                            farmacia));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestBody Farmacia farmacia) {

        try {

            return ResponseEntity.ok(
                    farmaciaService.actualizar(
                            id,
                            farmacia));

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
                    farmaciaService.cambiarEstado(
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
}