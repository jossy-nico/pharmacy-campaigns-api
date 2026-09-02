package cl.farmaciasahumada.campannas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.farmaciasahumada.campannas.model.CampaniaAuditoria;

public interface CampaniaAuditoriaRepository
        extends JpaRepository<CampaniaAuditoria, Long> {

    List<CampaniaAuditoria> findAllByCampaniaIdOrderByFechaModificacionDesc(
            Long campaniaId);
}