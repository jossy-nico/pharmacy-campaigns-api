package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.AnalisisProducto;

public interface AnalisisProductoRepository
        extends JpaRepository<AnalisisProducto, Long> {

    List<AnalisisProducto> findAllByAnalisisIdOrderByIdAsc(
            Long analisisId);

    List<AnalisisProducto> findAllByAnalisisIdAndEstadoComparacionOrderByIdAsc(
            Long analisisId,
            String estadoComparacion);
}