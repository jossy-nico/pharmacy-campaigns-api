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
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "archivo")
public class Archivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "definicion_id", nullable = false)
    private ArchivoDefinicion definicion;

    @Column(name = "grupo_version", nullable = false)
    private String grupoVersion;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "estado_archivo", nullable = false)
    private String estadoArchivo;

    @Column(name = "estado_procesamiento", nullable = false)
    private String estadoProcesamiento;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false)
    private String nombreAlmacenado;

    @Column(name = "mime_type")
    private String mimeType;

    private String extension;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "ruta_almacenamiento", nullable = false)
    private String rutaAlmacenamiento;

    @Column(name = "hash_sha256")
    private String hashSha256;

    @ManyToOne
    @JoinColumn(name = "campania_id")
    private Campania campania;

    @ManyToOne
    @JoinColumn(name = "farmacia_id")
    private Farmacia farmacia;

    @ManyToOne
    @JoinColumn(name = "exhibidor_id")
    private Exhibidor exhibidor;

    private String vista;

    @Column(name = "rol_imagen")
    private String rolImagen;

    @Column(nullable = false)
    private String origen;

    @Column(name = "id_externo")
    private String idExterno;

    @Column(name = "fecha_captura")
    private LocalDateTime fechaCaptura;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_adicionales", columnDefinition = "jsonb")
    private Map<String, Object> datosAdicionales;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_origen", columnDefinition = "jsonb")
    private Map<String, Object> datosOrigen;

    @ManyToOne
    @JoinColumn(name = "archivo_anterior_id")
    private Archivo archivoAnterior;
}
