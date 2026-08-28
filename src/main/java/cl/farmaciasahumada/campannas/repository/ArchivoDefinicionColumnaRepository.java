package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicionColumna;

public interface ArchivoDefinicionColumnaRepository extends JpaRepository<ArchivoDefinicionColumna, Long>{
    List<ArchivoDefinicionColumna> findByDefinicionIdAndActivoTrueOrderByOrdenAsc(Long definicionId);

}
