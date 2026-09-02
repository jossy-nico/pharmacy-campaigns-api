package cl.farmaciasahumada.campannas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.ConfiguracionSistema;

public interface ConfiguracionSistemaRepository
        extends JpaRepository<ConfiguracionSistema, Long> {

    Optional<ConfiguracionSistema> findByClaveIgnoreCase(
            String clave);

    boolean existsByClaveIgnoreCase(
            String clave);
}