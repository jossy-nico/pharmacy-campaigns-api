package cl.farmaciasahumada.campannas.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "analisis_producto")
public class AnalisisProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Relación con el análisis general
     * de la evidencia.
     */
    @Column(name = "analisis_id", nullable = false)
    private Long analisisId;

    /*
     * Datos provenientes del archivo
     * PRODUCTOS_PAI asociado a la campaña.
     */
    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "descriptor", length = 500)
    private String descriptor;

    @Column(name = "marca", length = 250)
    private String marca;

    /*
     * Indica si el producto fue detectado
     * en la fotografía de referencia zonal.
     */
    @Column(name = "presente_referencia", nullable = false)
    private Boolean presenteReferencia = false;

    /*
     * Indica si el producto fue detectado
     * en la fotografía enviada por la farmacia.
     */
    @Column(name = "presente_evidencia")
    private Boolean presenteEvidencia;

    /*
     * Nivel de confianza de detección
     * en la fotografía zonal.
     */
    @Column(name = "confianza_referencia", precision = 5, scale = 4)
    private BigDecimal confianzaReferencia;

    /*
     * Nivel de confianza de detección
     * en la fotografía de farmacia.
     */
    @Column(name = "confianza_evidencia")
    private BigDecimal confianzaEvidencia;

    /*
     * Valores permitidos:
     *
     * COINCIDE
     * FALTANTE_FARMACIA
     * ADICIONAL_FARMACIA
     * NO_CONCLUYENTE
     */
    @Column(name = "estado_comparacion", nullable = false, length = 40)
    private String estadoComparacion;

}