package cl.farmaciasahumada.campannas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.Farmacia;

public interface FarmaciaRepository extends JpaRepository<Farmacia, Long> {

    Optional<Farmacia> findByCodigoFarmacia(String codigoFarmacia);
}