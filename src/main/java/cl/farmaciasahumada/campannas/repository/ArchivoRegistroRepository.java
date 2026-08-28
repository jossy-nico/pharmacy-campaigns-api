package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.ArchivoRegistro;

public interface ArchivoRegistroRepository
        extends JpaRepository<ArchivoRegistro, Long> {

    List<ArchivoRegistro>
        findByArchivoIdOrderByNumeroFilaAsc(Long archivoId);
}