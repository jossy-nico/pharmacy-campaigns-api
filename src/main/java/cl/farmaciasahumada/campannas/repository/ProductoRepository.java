package cl.farmaciasahumada.campannas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByNombreIgnoreCase(String nombre);

}
