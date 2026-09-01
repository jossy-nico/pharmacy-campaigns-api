package cl.farmaciasahumada.campannas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;

public interface ArchivoDefinicionRepository extends JpaRepository<ArchivoDefinicion, Long> {
    Optional<ArchivoDefinicion> findByCodigoAndActivoTrue(String codigo);

    /*
     * Se modifica este método para la búsqueda de tablas sea escrito con mayúsculas
     * o minúsculas
     * y se refleje en una sola tabla y no agregue nuevas
     */
    Optional<ArchivoDefinicion> findByCodigoIgnoreCaseAndActivoTrue(String codigo);

    // permite comprobar si un código ya esta registrado, si se encuentra inactivo
    boolean existsByCodigoIgnoreCase(String codigo);

    // esto nos servira para llenar el selector del front
    List<ArchivoDefinicion> findAllByActivoTrueOrderByNombreAsc();

    List<ArchivoDefinicion> findAllByCategoriaIgnoreCaseAndActivoTrueOrderByNombreAsc(
            String categoria);

    Optional<ArchivoDefinicion> findByCodigoIgnoreCase(String codigo);

    List<ArchivoDefinicion> findAllByActivoFalseOrderByNombreAsc();

    List<ArchivoDefinicion> findAllByCategoriaIgnoreCaseAndActivoFalseOrderByNombreAsc(
            String categoria);

}
