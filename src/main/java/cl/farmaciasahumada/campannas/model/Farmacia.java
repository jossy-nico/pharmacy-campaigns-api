package cl.farmaciasahumada.campannas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "farmacia")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Farmacia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_farmacia", nullable = false, unique = true)
    private String codigoFarmacia;

    private String direccion;

    private String subgerente;

    @Column(name = "administrador_zonal")
    private String administradorZonal;

    private String mercado;

    private String ciudad;

    private String comuna;

    private String region;

    @Column(name = "formato_comercial")
    private String formatoComercial;

    private String clasificacion;
}