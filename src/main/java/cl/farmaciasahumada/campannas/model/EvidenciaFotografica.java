package cl.farmaciasahumada.campannas.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidencia_fotografica")
public class EvidenciaFotografica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campania_id", nullable = false)
    private Long campaniaId;

    /*
     * REFERENCIA_ZONAL:
     * farmaciaId = null
     *
     * EVIDENCIA_FARMACIA:
     * farmaciaId = farmacia que subió la evidencia
     */
    @Column(name = "farmacia_id")
    private Long farmaciaId;

    @Column(name = "tipo_evidencia", nullable = false, length = 30)
    private String tipoEvidencia;

    /*
     * Para una foto zonal será null.
     *
     * Para una foto de farmacia contiene
     * el ID de la fotografía zonal contra
     * la cual deberá compararse.
     */
    @Column(name = "referencia_zonal_id")
    private Long referenciaZonalId;

    @Column(name = "exhibidor", nullable = false, length = 150)
    private String exhibidor;

    @Column(name = "vista", nullable = false, length = 100)
    private String vista;

    @Column(name = "nombre_original", nullable = false, length = 500)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, length = 500)
    private String nombreAlmacenado;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 1000)
    private String rutaAlmacenamiento;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "origen", nullable = false, length = 50)
    private String origen;

    /*
     * Futuro:
     * permitirá relacionar la evidencia con
     * un identificador proveniente de Frogmi
     * u otro proveedor externo.
     */
    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Column(name = "resultado", length = 40)
    private String resultado;

    @Column(name = "observacion", length = 2000)
    private String observacion;

    @Column(name = "fecha_carga", nullable = false)
    private OffsetDateTime fechaCarga;

    @Column(name = "fecha_modificacion")
    private OffsetDateTime fechaModificacion;

    /*
     * Temporalmente será null.
     * Se completará cuando implementemos
     * usuarios y roles al final.
     */
    @Column(name = "usuario_carga", length = 150)
    private String usuarioCarga;

    @PrePersist
    public void prePersist() {

        if (origen == null || origen.isBlank()) {
            origen = "CARGA_WEB";
        }

        if (estado == null || estado.isBlank()) {
            estado = "CARGADA";
        }

        if (fechaCarga == null) {
            fechaCarga = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaniaId() {
        return campaniaId;
    }

    public void setCampaniaId(Long campaniaId) {
        this.campaniaId = campaniaId;
    }

    public Long getFarmaciaId() {
        return farmaciaId;
    }

    public void setFarmaciaId(Long farmaciaId) {
        this.farmaciaId = farmaciaId;
    }

    public String getTipoEvidencia() {
        return tipoEvidencia;
    }

    public void setTipoEvidencia(String tipoEvidencia) {
        this.tipoEvidencia = tipoEvidencia;
    }

    public Long getReferenciaZonalId() {
        return referenciaZonalId;
    }

    public void setReferenciaZonalId(Long referenciaZonalId) {
        this.referenciaZonalId = referenciaZonalId;
    }

    public String getExhibidor() {
        return exhibidor;
    }

    public void setExhibidor(String exhibidor) {
        this.exhibidor = exhibidor;
    }

    public String getVista() {
        return vista;
    }

    public void setVista(String vista) {
        this.vista = vista;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getNombreAlmacenado() {
        return nombreAlmacenado;
    }

    public void setNombreAlmacenado(String nombreAlmacenado) {
        this.nombreAlmacenado = nombreAlmacenado;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public void setRutaAlmacenamiento(String rutaAlmacenamiento) {
        this.rutaAlmacenamiento = rutaAlmacenamiento;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public OffsetDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(OffsetDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(OffsetDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUsuarioCarga() {
        return usuarioCarga;
    }

    public void setUsuarioCarga(String usuarioCarga) {
        this.usuarioCarga = usuarioCarga;
    }
}