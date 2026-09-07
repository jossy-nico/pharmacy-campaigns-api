package cl.farmaciasahumada.campannas.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analisis_evidencia")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AnalisisEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidencia_evaluada_id", nullable = false)
    private Long evidenciaEvaluadaId;

    @Column(name = "referencia_oficial_id", nullable = false)
    private Long referenciaOficialId;

    /*
     * PENDIENTE
     * EN_PROCESO
     * COMPLETADO
     * ERROR
     */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /*
     * CUMPLE
     * NO_CUMPLE
     * REQUIERE_REVISION
     * REQUIERE_NUEVA_FOTO
     */
    @Column(name = "resultado", length = 30)
    private String resultado;

    /*
     * Ejemplo:
     *
     * Foto zonal contiene A, B y C.
     * Farmacia contiene A y B.
     * Falta producto C.
     * NO CUMPLE con lo establecido.
     */
    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "texto_ocr_referencia")
    private String textoOcrReferencia;

    @Column(name = "texto_ocr_evidencia")
    private String textoOcrEvidencia;

    /*
     * Confianza general del análisis.
     * Valor esperado entre 0 y 1.
     */
    @Column(name = "confianza", precision = 5, scale = 4)
    private BigDecimal confianza;

    /*
     * Permitirá saber qué versión del
     * algoritmo generó el resultado.
     */
    @Column(name = "version_algoritmo", length = 50)
    private String versionAlgoritmo;

    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    /*
     * En caso de error durante OCR,
     * comparación o procesamiento.
     */
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @PrePersist
    public void prePersist() {

        if (estado == null
                || estado.isBlank()) {

            estado = "PENDIENTE";
        }
    }

}