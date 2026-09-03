package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.EvidenciaFotografica;

public interface EvidenciaFotograficaRepository
                extends JpaRepository<EvidenciaFotografica, Long> {

        List<EvidenciaFotografica> findAllByCampaniaIdOrderByFechaCargaDesc(
                        Long campaniaId);

        List<EvidenciaFotografica> findAllByCampaniaIdAndFarmaciaIdOrderByFechaCargaDesc(
                        Long campaniaId,
                        Long farmaciaId);

        List<EvidenciaFotografica> findAllByCampaniaIdAndTipoEvidenciaOrderByFechaCargaDesc(
                        Long campaniaId,
                        String tipoEvidencia);

        List<EvidenciaFotografica> findAllByReferenciaZonalIdOrderByFechaCargaDesc(
                        Long referenciaZonalId);
}