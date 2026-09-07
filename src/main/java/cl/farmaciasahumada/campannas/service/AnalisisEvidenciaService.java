package cl.farmaciasahumada.campannas.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.farmaciasahumada.campannas.model.AnalisisEvidencia;
import cl.farmaciasahumada.campannas.model.EvidenciaFotografica;
import cl.farmaciasahumada.campannas.repository.AnalisisEvidenciaRepository;
import cl.farmaciasahumada.campannas.repository.EvidenciaFotograficaRepository;

@Service
public class AnalisisEvidenciaService {

        private static final String REFERENCIA_OFICIAL = "REFERENCIA_OFICIAL";
        private static final String EVIDENCIA_ZONAL = "EVIDENCIA_ZONAL";
        private static final String EVIDENCIA_FARMACIA = "EVIDENCIA_FARMACIA";

        private final AnalisisEvidenciaRepository analisisRepository;
        private final EvidenciaFotograficaRepository evidenciaRepository;

        public AnalisisEvidenciaService(
                        AnalisisEvidenciaRepository analisisRepository,
                        EvidenciaFotograficaRepository evidenciaRepository) {

                this.analisisRepository = analisisRepository;
                this.evidenciaRepository = evidenciaRepository;
        }

        /*
         * =========================================================
         * CREAR ANÁLISIS
         * =========================================================
         */

        @Transactional
        public AnalisisEvidencia crearAnalisis(
                        Long evidenciaEvaluadaId) {

                if (evidenciaEvaluadaId == null) {

                        throw new IllegalArgumentException(
                                        "La evidencia a evaluar es obligatoria.");
                }

                EvidenciaFotografica evidenciaEvaluada = evidenciaRepository
                                .findById(evidenciaEvaluadaId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe la evidencia con id: "
                                                                                + evidenciaEvaluadaId));

                validarTipoEvidenciaEvaluada(
                                evidenciaEvaluada);

                if (evidenciaEvaluada.getFarmaciaId() == null) {

                        throw new IllegalArgumentException(
                                        "La evidencia no posee una farmacia asociada.");
                }

                if (evidenciaEvaluada.getReferenciaOficialId() == null) {

                        throw new IllegalArgumentException(
                                        "La evidencia no posee una referencia oficial asociada.");
                }

                EvidenciaFotografica referenciaOficial = evidenciaRepository
                                .findById(
                                                evidenciaEvaluada.getReferenciaOficialId())
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe la referencia oficial asociada."));

                validarRelacion(
                                evidenciaEvaluada,
                                referenciaOficial);

                AnalisisEvidencia analisis = new AnalisisEvidencia();

                analisis.setEvidenciaEvaluadaId(
                                evidenciaEvaluada.getId());

                analisis.setReferenciaOficialId(
                                referenciaOficial.getId());

                /*
                 * Todavía no se ejecuta OCR ni visión computacional.
                 * El análisis queda preparado para que posteriormente
                 * el motor procese la referencia oficial y la evidencia.
                 */
                analisis.setEstado(
                                "PENDIENTE");

                analisis.setResultado(
                                null);

                analisis.setResumen(
                                null);

                analisis.setTextoOcrReferencia(
                                null);

                analisis.setTextoOcrEvidencia(
                                null);

                analisis.setConfianza(
                                null);

                analisis.setVersionAlgoritmo(
                                "BASE_1");

                analisis.setFechaInicio(
                                null);

                analisis.setFechaFin(
                                null);

                analisis.setError(
                                null);

                return analisisRepository.saveAndFlush(
                                analisis);
        }

        /*
         * =========================================================
         * CONSULTAS
         * =========================================================
         */

        public AnalisisEvidencia obtenerPorId(
                        Long id) {

                if (id == null) {

                        throw new IllegalArgumentException(
                                        "El id del análisis es obligatorio.");
                }

                return analisisRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe el análisis con id: "
                                                                                + id));
        }

        public List<AnalisisEvidencia> listarPorEvidencia(
                        Long evidenciaEvaluadaId) {

                if (evidenciaEvaluadaId == null) {

                        throw new IllegalArgumentException(
                                        "La evidencia es obligatoria.");
                }

                return analisisRepository
                                .findAllByEvidenciaEvaluadaIdOrderByIdDesc(
                                                evidenciaEvaluadaId);
        }

        /*
         * =========================================================
         * VALIDACIÓN DEL TIPO DE EVIDENCIA
         * =========================================================
         */

        private void validarTipoEvidenciaEvaluada(
                        EvidenciaFotografica evidencia) {

                String tipo = normalizar(
                                evidencia.getTipoEvidencia());

                if (!EVIDENCIA_ZONAL.equals(tipo)
                                && !EVIDENCIA_FARMACIA.equals(tipo)) {

                        throw new IllegalArgumentException(
                                        "Solo se pueden analizar evidencias zonales "
                                                        + "o evidencias de farmacia.");
                }
        }

        /*
         * =========================================================
         * VALIDACIÓN:
         *
         * REFERENCIA_OFICIAL
         * VS
         * EVIDENCIA_ZONAL / EVIDENCIA_FARMACIA
         * =========================================================
         */

        private void validarRelacion(
                        EvidenciaFotografica evidenciaEvaluada,
                        EvidenciaFotografica referenciaOficial) {

                if (!REFERENCIA_OFICIAL.equals(
                                normalizar(
                                                referenciaOficial.getTipoEvidencia()))) {

                        throw new IllegalArgumentException(
                                        "La fotografía asociada no corresponde "
                                                        + "a una referencia oficial del planograma.");
                }

                /*
                 * La referencia oficial no debe pertenecer
                 * a ninguna farmacia.
                 */
                if (referenciaOficial.getFarmaciaId() != null) {

                        throw new IllegalArgumentException(
                                        "La referencia oficial posee una farmacia asociada "
                                                        + "y su estructura no es válida.");
                }

                /*
                 * Una referencia oficial tampoco debe apuntar
                 * a otra referencia.
                 */
                if (referenciaOficial.getReferenciaOficialId() != null) {

                        throw new IllegalArgumentException(
                                        "La referencia oficial posee una relación inválida.");
                }

                if (!evidenciaEvaluada
                                .getCampaniaId()
                                .equals(
                                                referenciaOficial.getCampaniaId())) {

                        throw new IllegalArgumentException(
                                        "La evidencia y la referencia oficial "
                                                        + "pertenecen a campañas diferentes.");
                }

                if (!normalizar(
                                evidenciaEvaluada.getExhibidor())
                                .equals(
                                                normalizar(
                                                                referenciaOficial.getExhibidor()))) {

                        throw new IllegalArgumentException(
                                        "La evidencia y la referencia oficial "
                                                        + "corresponden a exhibidores diferentes.");
                }

                if (!normalizar(
                                evidenciaEvaluada.getVista())
                                .equals(
                                                normalizar(
                                                                referenciaOficial.getVista()))) {

                        throw new IllegalArgumentException(
                                        "La evidencia y la referencia oficial "
                                                        + "corresponden a vistas diferentes.");
                }

                /*
                 * Validación adicional:
                 * la referencia cargada debe ser exactamente
                 * la indicada por la evidencia.
                 */
                if (!referenciaOficial
                                .getId()
                                .equals(
                                                evidenciaEvaluada.getReferenciaOficialId())) {

                        throw new IllegalArgumentException(
                                        "La evidencia no corresponde "
                                                        + "a la referencia oficial indicada.");
                }
        }

        /*
         * =========================================================
         * NORMALIZACIÓN
         * =========================================================
         */

        private String normalizar(
                        String valor) {

                if (valor == null) {
                        return "";
                }

                return valor
                                .trim()
                                .toUpperCase(Locale.ROOT)
                                .replaceAll("\\s+", "_");
        }
}