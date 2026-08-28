package cl.farmaciasahumada.campannas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.Archivo;

public interface ArchivoRepository
        extends JpaRepository<Archivo, Long> {

    Optional<Archivo>
        findTopByGrupoVersionOrderByVersionDesc(String grupoVersion);

    Optional<Archivo>
        findByGrupoVersionAndEstadoArchivo(
            String grupoVersion,
            String estadoArchivo
        );

    List<Archivo>
        findByGrupoVersionOrderByVersionDesc(String grupoVersion);

    List<Archivo>
        findByDefinicionCodigoAndEstadoArchivo(
            String codigoDefinicion,
            String estadoArchivo
        );
}
