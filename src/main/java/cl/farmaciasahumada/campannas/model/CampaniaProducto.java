package cl.farmaciasahumada.campannas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "campania_productos")
public class CampaniaProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campania_id", nullable = false)
    private Campania campania;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

}
