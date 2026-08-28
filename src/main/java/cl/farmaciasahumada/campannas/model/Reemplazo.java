package cl.farmaciasahumada.campannas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reemplazo")
public class Reemplazo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campania_id", nullable = false)
    private Campania campania;

    @ManyToOne
    @JoinColumn(name = "producto_principal_id", nullable = false)
    private Producto productoPrincipal;

    @ManyToOne
    @JoinColumn(name = "producto_reemplazo_id", nullable = false)
    private Producto productoReemplazo;

    private Integer prioridad;


}
