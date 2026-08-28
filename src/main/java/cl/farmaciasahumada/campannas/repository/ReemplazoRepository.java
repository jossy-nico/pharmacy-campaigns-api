package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.Reemplazo;

public interface ReemplazoRepository extends JpaRepository<Reemplazo, Long>{

    List<Reemplazo> findByCampaniaIdOrderByPrioridadAsc(Long campaniaId);

}
