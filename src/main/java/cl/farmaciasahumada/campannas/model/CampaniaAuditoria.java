package cl.farmaciasahumada.campannas.model;

import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campania_auditoria")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CampaniaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campania_id", nullable = false)
    private Campania campania;

    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> datosNuevos;

    @Column(name = "usuario")
    private String usuario;

    @Column(name = "fecha_modificacion", nullable = false)
    private OffsetDateTime fechaModificacion;
}