package cl.farmaciasahumada.campannas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "archivo_definicion_regla")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ArchivoDefinicionRegla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "archivo_definicion_id", nullable = false)
    private ArchivoDefinicion definicion;

    @Column(name = "nombre_campo", nullable = false)
    private String nombreCampo;

    @Column(name = "columna_obligatoria", nullable = false)
    private Boolean columnaObligatoria;

    @Column(name = "valor_obligatorio", nullable = false)
    private Boolean valorObligatorio;

    @Column(name = "rechazar_placeholders", nullable = false)
    private Boolean rechazarPlaceholders;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}