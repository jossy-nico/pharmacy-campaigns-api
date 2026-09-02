package cl.farmaciasahumada.campannas.model;

import java.time.LocalDate;

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
@Table(name = "campania")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Campania {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Código técnico generado por el backend.
     *
     * Ejemplo:
     * PAI_2026_10
     *
     * No debe ser escrito manualmente por el usuario.
     */
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre")
    private String nombre;

    /*
     * Período real de la campaña.
     *
     * Ejemplo:
     * año = 2026
     * mes = 10
     */
    @Column(name = "anio")
    private Integer anio;

    @Column(name = "mes")
    private Integer mes;

    /*
     * Estas fechas serán calculadas automáticamente
     * a partir del año y mes.
     *
     * Octubre 2026:
     * 2026-10-01
     * 2026-10-31
     */
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /*
     * Se calcula utilizando la configuración vigente
     * al momento de preparar la campaña.
     *
     * Luego queda guardada para mantener histórico.
     */
    @Column(name = "fecha_limite_carga")
    private LocalDate fechaLimiteCarga;

    /*
     * ID exacto del archivo PRODUCTOS_PAI utilizado
     * para construir la campaña.
     *
     * No apuntamos solamente al nombre del dataset,
     * sino a una versión física concreta del archivo.
     */
    @Column(name = "archivo_productos_id")
    private Long archivoProductosId;

    /*
     * Estados previstos:
     *
     * BORRADOR
     * PROGRAMADA
     * ACTIVA
     * FINALIZADA
     * CANCELADA
     */
    @Column(name = "estado")
    private String estado;
}