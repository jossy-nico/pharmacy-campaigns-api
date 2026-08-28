package cl.farmaciasahumada.campannas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;

public interface ArchivoDefinicionRepository extends JpaRepository<ArchivoDefinicion, Long>{
    Optional<ArchivoDefinicion> findByCodigoAndActivoTrue(String codigo);

}
