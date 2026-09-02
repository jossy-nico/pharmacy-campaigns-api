package cl.farmaciasahumada.campannas.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "archivo_definicion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArchivoDefinicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String categoria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formatos_permitidos", columnDefinition = "jsonb", nullable = false)
    private String formatosPermitidos;

    @Column(name = "politica_retencion", nullable = false)
    private String politicaRetencion;

    @Column(name = "max_versiones_retenidas")
    private Integer maxVersionesRetenidas;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "permitir_columnas_adicionales", nullable = false)
    private Boolean permitirColumnasAdicionales;
}