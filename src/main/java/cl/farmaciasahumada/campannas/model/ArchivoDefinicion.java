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

    @Column(name = "fila_encabezado")
    private Integer filaEncabezado;

    @Column(name = "modo_sincronizacion", nullable = false)
    private String modoSincronizacion;

    @Column(name = "tabla_destino")
    private String tablaDestino;

    @Column(name = "requiere_campania", nullable = false)
    private Boolean requiereCampania;

    @Column(name = "requiere_farmacia", nullable = false)
    private Boolean requiereFarmacia;

    @Column(name = "requiere_exhibidor", nullable = false)
    private Boolean requiereExhibidor;

    @Column(name = "conservar_columnas_desconocidas", nullable = false)
    private Boolean conservarColumnasDesconocidas;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}