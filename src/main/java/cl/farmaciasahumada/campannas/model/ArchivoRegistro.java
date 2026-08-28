package cl.farmaciasahumada.campannas.model;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "archivo_registro")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArchivoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "archivo_id", nullable = false)
    private Archivo archivo;

    @Column(name = "numero_fila", nullable = false)
    private Integer numeroFila;

    @Column(name = "clave_negocio")
    private String claveNegocio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> datos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_adicionales", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> datosAdicionales;

    @Column(nullable = false)
    private Boolean valido;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errores", columnDefinition = "jsonb", nullable = false)
    private List<String> errores;
}