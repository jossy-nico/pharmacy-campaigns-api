package cl.farmaciasahumada.campannas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.Campania;

public interface CampaniaRepository
        extends JpaRepository<Campania, Long> {

    /*
     * Una campaña PAI por período.
     *
     * Ejemplo:
     * Octubre 2026 -> año 2026 / mes 10
     */
    boolean existsByAnioAndMes(
            Integer anio,
            Integer mes);

    Optional<Campania> findByAnioAndMes(
            Integer anio,
            Integer mes);

    /*
     * Muestra las campañas más recientes primero.
     */
    List<Campania> findAllByOrderByAnioDescMesDesc();
}