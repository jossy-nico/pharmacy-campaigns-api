package cl.farmaciasahumada.campannas.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "archivo_definicion_columna")
public class ArchivoDefinicionColumna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "definicion_id", nullable = false)
    private ArchivoDefinicion definicion;

    @Column(name = "nombre_columna", nullable = false)
    private String nombreColumna;

    @Column(name = "campo_destino", nullable = false)
    private String campoDestino;

    @Column(name = "tipo_dato", nullable = false)
    private String tipoDato;

    @Column(nullable = false)
    private Boolean obligatoria;

    @Column(name = "es_clave_negocio", nullable = false)
    private Boolean esClaveNegocio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String aliases;

    private Integer orden;

    @Column(nullable = false)
    private Boolean activo;
}
