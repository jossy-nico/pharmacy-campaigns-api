package cl.farmaciasahumada.campannas.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.farmaciasahumada.campannas.model.Campania;
import cl.farmaciasahumada.campannas.repository.CampaniaRepository;

@Service
public class CampaniaService {

    private final CampaniaRepository campaniaRepository;

    public CampaniaService(CampaniaRepository campaniaRepository) {
        this.campaniaRepository = campaniaRepository;
    }

    public Campania guardarCampania(Campania campania) {
        if (campania.getFechaInicio() == null || campania.getFechaFin() == null) {
            throw new IllegalArgumentException("Las fechas de la campaña son obligatorias");
        }

        if (campania.getFechaFin().isBefore(campania.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de término no puede ser anterior a la fecha de inicio");
        }
        return campaniaRepository.save(campania);
    }

    public List<Campania> listarCampanias() {
        return campaniaRepository.findAll();
    }

}
