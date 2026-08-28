package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.CampaniaProducto;

public interface CampaniaProductoRepository extends JpaRepository<CampaniaProducto, Long>{

    List<CampaniaProducto> findByCampaniaId(Long campaniaId);

}
