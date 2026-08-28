package cl.farmaciasahumada.campannas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.Exhibidor;

public interface ExhibidorRepository extends JpaRepository<Exhibidor, Long> {

    Optional<Exhibidor> findByCodigo(String codigo);
}