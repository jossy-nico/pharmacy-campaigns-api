package cl.farmaciasahumada.campannas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.AnalisisEvidencia;

public interface AnalisisEvidenciaRepository
                extends JpaRepository<AnalisisEvidencia, Long> {

        List<AnalisisEvidencia> findAllByEvidenciaEvaluadaIdOrderByIdDesc(Long evidenciaEvaluadaId);

        List<AnalisisEvidencia> findAllByReferenciaOficialIdOrderByIdDesc(Long referenciaOficialId);

        Optional<AnalisisEvidencia> findTopByEvidenciaEvaluadaIdOrderByIdDesc(
                        Long evidenciaEvaluadaId);
}