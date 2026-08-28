package cl.farmaciasahumada.campannas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.RegistroGenerico;

public interface RegistroGenericoRepository extends JpaRepository<RegistroGenerico, Long> {

    Optional<RegistroGenerico>findByDefinicionIdAndClaveNegocio(Long definicionId,String claveNegocio);

    List<RegistroGenerico>    findByDefinicionIdAndActivoTrue(Long definicionId);
}