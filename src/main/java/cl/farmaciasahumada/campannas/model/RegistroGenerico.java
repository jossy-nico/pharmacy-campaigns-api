package cl.farmaciasahumada.campannas.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registro_generico")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistroGenerico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "definicion_id", nullable = false)
    private ArchivoDefinicion definicion;

    @Column(name = "clave_negocio", nullable = false)
    private String claveNegocio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> datos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_adicionales", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> datosAdicionales;

    @Column(nullable = false)
    private Boolean activo;

    @ManyToOne
    @JoinColumn(name = "archivo_origen_id")
    private Archivo archivoOrigen;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}