package cl.farmaciasahumada.campannas.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.farmaciasahumada.campannas.model.Campania;
import cl.farmaciasahumada.campannas.service.CampaniaService;

@RestController
@RequestMapping("/api/campanias")
public class CampaniaController {
    private final CampaniaService campaniaService;

    public CampaniaController(CampaniaService campaniaService){
        this.campaniaService = campaniaService;
    }

    @PostMapping
    public ResponseEntity<?> crearCampania(
            @RequestBody Campania campania) {

        try {

            Campania guardada =
                    campaniaService.guardarCampania(campania);

            return ResponseEntity.ok(guardada);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Campania>> listarCampanias(){
        return ResponseEntity.ok(campaniaService.listarCampanias());
    }

}
