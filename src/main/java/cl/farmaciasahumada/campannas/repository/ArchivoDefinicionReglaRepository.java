package cl.farmaciasahumada.campannas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicionRegla;

public interface ArchivoDefinicionReglaRepository
        extends JpaRepository<ArchivoDefinicionRegla, Long> {

    List<ArchivoDefinicionRegla> findAllByDefinicionIdAndActivoTrueOrderByNombreCampoAsc(
            Long archivoDefinicionId);

    Optional<ArchivoDefinicionRegla> findByDefinicionIdAndNombreCampoIgnoreCase(
            Long archivoDefinicionId,
            String nombreCampo);

    boolean existsByDefinicionIdAndNombreCampoIgnoreCase(
            Long archivoDefinicionId,
            String nombreCampo);

    void deleteAllByDefinicionId(
            Long archivoDefinicionId);
}