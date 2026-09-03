package cl.farmaciasahumada.campannas.service;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.farmaciasahumada.campannas.model.Campania;
import cl.farmaciasahumada.campannas.model.EvidenciaFotografica;
import cl.farmaciasahumada.campannas.repository.CampaniaRepository;
import cl.farmaciasahumada.campannas.repository.EvidenciaFotograficaRepository;
import cl.farmaciasahumada.campannas.repository.FarmaciaRepository;
import cl.farmaciasahumada.campannas.service.archivo.ArchivoStorageService;

@Service
public class EvidenciaFotograficaService {

    private static final long MAX_TAMANO_IMAGEN = 25L * 1024L * 1024L;

    private final EvidenciaFotograficaRepository evidenciaRepository;
    private final CampaniaRepository campaniaRepository;
    private final FarmaciaRepository farmaciaRepository;
    private final ArchivoStorageService storageService;

    public EvidenciaFotograficaService(
            EvidenciaFotograficaRepository evidenciaRepository,
            CampaniaRepository campaniaRepository,
            FarmaciaRepository farmaciaRepository,
            ArchivoStorageService storageService) {

        this.evidenciaRepository = evidenciaRepository;
        this.campaniaRepository = campaniaRepository;
        this.farmaciaRepository = farmaciaRepository;
        this.storageService = storageService;
    }

    /*
     * =========================================================
     * FOTO DE REFERENCIA DEL ZONAL
     * =========================================================
     */

    @Transactional
    public EvidenciaFotografica subirReferenciaZonal(
            Long campaniaId,
            String exhibidor,
            String vista,
            MultipartFile imagen,
            String observacion) throws Exception {

        Campania campania = obtenerCampania(
                campaniaId);

        validarEstadoParaReferencia(
                campania);

        validarTextoObligatorio(
                exhibidor,
                "El exhibidor es obligatorio.");

        validarTextoObligatorio(
                vista,
                "La vista de la fotografía es obligatoria.");

        validarImagen(
                imagen);

        DatosArchivoImagen datosArchivo = almacenarImagen(
                imagen);

        EvidenciaFotografica evidencia = new EvidenciaFotografica();

        evidencia.setCampaniaId(
                campania.getId());

        /*
         * La referencia zonal no pertenece
         * a una farmacia específica.
         */
        evidencia.setFarmaciaId(
                null);

        evidencia.setReferenciaZonalId(
                null);

        evidencia.setTipoEvidencia(
                "REFERENCIA_ZONAL");

        evidencia.setExhibidor(
                normalizarTexto(
                        exhibidor));

        evidencia.setVista(
                normalizarTexto(
                        vista));

        completarDatosArchivo(
                evidencia,
                imagen,
                datosArchivo);

        evidencia.setOrigen(
                "CARGA_WEB");

        evidencia.setEstado(
                "CARGADA");

        evidencia.setResultado(
                null);

        evidencia.setObservacion(
                normalizarObservacion(
                        observacion));

        evidencia.setUsuarioCarga(
                null);

        evidencia.setFechaCarga(
                OffsetDateTime.now());

        return evidenciaRepository.saveAndFlush(
                evidencia);
    }

    /*
     * =========================================================
     * FOTO ENVIADA POR UNA FARMACIA
     * =========================================================
     */

    @Transactional
    public EvidenciaFotografica subirEvidenciaFarmacia(
            Long campaniaId,
            Long farmaciaId,
            Long referenciaZonalId,
            MultipartFile imagen,
            String observacion) throws Exception {

        Campania campania = obtenerCampania(
                campaniaId);

        /*
         * Una farmacia solo debería enviar evidencia
         * cuando la campaña está realmente ACTIVA.
         */
        if (!"ACTIVA".equalsIgnoreCase(
                campania.getEstado())) {

            throw new IllegalArgumentException(
                    "La farmacia solo puede cargar evidencias "
                            + "para una campaña ACTIVA.");
        }

        if (farmaciaId == null) {

            throw new IllegalArgumentException(
                    "La farmacia es obligatoria.");
        }

        if (!farmaciaRepository.existsById(
                farmaciaId)) {

            throw new IllegalArgumentException(
                    "No existe la farmacia con id: "
                            + farmaciaId);
        }

        if (referenciaZonalId == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar la fotografía zonal "
                            + "que será utilizada como referencia.");
        }

        EvidenciaFotografica referencia = evidenciaRepository
                .findById(
                        referenciaZonalId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No existe la referencia zonal con id: "
                                        + referenciaZonalId));

        validarReferenciaZonal(
                referencia,
                campaniaId);

        validarImagen(
                imagen);

        DatosArchivoImagen datosArchivo = almacenarImagen(
                imagen);

        EvidenciaFotografica evidencia = new EvidenciaFotografica();

        evidencia.setCampaniaId(
                campania.getId());

        evidencia.setFarmaciaId(
                farmaciaId);

        evidencia.setTipoEvidencia(
                "EVIDENCIA_FARMACIA");

        evidencia.setReferenciaZonalId(
                referencia.getId());

        /*
         * Exhibidor y vista NO los escribe nuevamente
         * la farmacia.
         *
         * Se copian desde la referencia zonal para evitar
         * que una fotografía quede asociada accidentalmente
         * a una vista diferente.
         */
        evidencia.setExhibidor(
                referencia.getExhibidor());

        evidencia.setVista(
                referencia.getVista());

        completarDatosArchivo(
                evidencia,
                imagen,
                datosArchivo);

        evidencia.setOrigen(
                "CARGA_WEB");

        evidencia.setEstado(
                "CARGADA");

        evidencia.setResultado(
                null);

        evidencia.setObservacion(
                normalizarObservacion(
                        observacion));

        evidencia.setUsuarioCarga(
                null);

        evidencia.setFechaCarga(
                OffsetDateTime.now());

        return evidenciaRepository.saveAndFlush(
                evidencia);
    }

    /*
     * =========================================================
     * CONSULTAS
     * =========================================================
     */

    public EvidenciaFotografica obtenerPorId(
            Long id) {

        return evidenciaRepository
                .findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No existe la evidencia con id: "
                                        + id));
    }

    public List<EvidenciaFotografica> listarPorCampania(
            Long campaniaId) {

        obtenerCampania(
                campaniaId);

        return evidenciaRepository
                .findAllByCampaniaIdOrderByFechaCargaDesc(
                        campaniaId);
    }

    public List<EvidenciaFotografica> listarReferenciasZonales(
            Long campaniaId) {

        obtenerCampania(
                campaniaId);

        return evidenciaRepository
                .findAllByCampaniaIdAndTipoEvidenciaOrderByFechaCargaDesc(
                        campaniaId,
                        "REFERENCIA_ZONAL");
    }

    public List<EvidenciaFotografica> listarPorFarmacia(
            Long campaniaId,
            Long farmaciaId) {

        obtenerCampania(
                campaniaId);

        if (!farmaciaRepository.existsById(
                farmaciaId)) {

            throw new IllegalArgumentException(
                    "No existe la farmacia con id: "
                            + farmaciaId);
        }

        return evidenciaRepository
                .findAllByCampaniaIdAndFarmaciaIdOrderByFechaCargaDesc(
                        campaniaId,
                        farmaciaId);
    }

    /*
     * =========================================================
     * VALIDACIONES
     * =========================================================
     */

    private Campania obtenerCampania(
            Long campaniaId) {

        if (campaniaId == null) {

            throw new IllegalArgumentException(
                    "La campaña es obligatoria.");
        }

        return campaniaRepository
                .findById(
                        campaniaId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No existe la campaña con id: "
                                        + campaniaId));
    }

    private void validarEstadoParaReferencia(
            Campania campania) {

        String estado = campania.getEstado() == null
                ? ""
                : campania.getEstado()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        /*
         * El zonal puede preparar las referencias antes
         * de la activación de la campaña o mientras
         * la campaña ya se encuentra activa.
         */
        if (!"PROGRAMADA".equals(estado)
                && !"ACTIVA".equals(estado)) {

            throw new IllegalArgumentException(
                    "Las fotografías zonales solo pueden cargarse "
                            + "cuando la campaña está PROGRAMADA o ACTIVA.");
        }
    }

    private void validarReferenciaZonal(
            EvidenciaFotografica referencia,
            Long campaniaId) {

        if (!"REFERENCIA_ZONAL".equalsIgnoreCase(
                referencia.getTipoEvidencia())) {

            throw new IllegalArgumentException(
                    "La evidencia seleccionada no corresponde "
                            + "a una fotografía de referencia zonal.");
        }

        if (!campaniaId.equals(
                referencia.getCampaniaId())) {

            throw new IllegalArgumentException(
                    "La fotografía zonal seleccionada "
                            + "pertenece a otra campaña.");
        }

        if (referencia.getFarmaciaId() != null
                || referencia.getReferenciaZonalId() != null) {

            throw new IllegalArgumentException(
                    "La fotografía seleccionada "
                            + "no posee una estructura válida de referencia zonal.");
        }
    }

    private void validarImagen(
            MultipartFile imagen) {

        if (imagen == null
                || imagen.isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe adjuntar una fotografía.");
        }

        if (imagen.getSize() > MAX_TAMANO_IMAGEN) {

            throw new IllegalArgumentException(
                    "La fotografía supera el tamaño máximo permitido de 25 MB.");
        }

        String nombre = imagen.getOriginalFilename();

        String extension = obtenerExtension(
                nombre);

        if (!List.of(
                "jpg",
                "jpeg",
                "png",
                "webp",
                "heic",
                "heif")
                .contains(extension)) {

            throw new IllegalArgumentException(
                    "Formato de imagen no soportado. "
                            + "Se permiten JPG, JPEG, PNG, WEBP, HEIC y HEIF.");
        }

        String mimeType = imagen.getContentType();

        if (mimeType != null
                && !mimeType.isBlank()) {

            String mimeNormalizado = mimeType
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!mimeNormalizado.startsWith("image/")
                    && !"application/octet-stream".equals(
                            mimeNormalizado)) {

                throw new IllegalArgumentException(
                        "El archivo seleccionado no corresponde a una imagen.");
            }
        }
    }

    private void validarTextoObligatorio(
            String valor,
            String mensaje) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    mensaje);
        }
    }

    /*
     * =========================================================
     * ALMACENAMIENTO
     * =========================================================
     */

    private DatosArchivoImagen almacenarImagen(
            MultipartFile imagen) throws Exception {

        String extension = obtenerExtension(
                imagen.getOriginalFilename());

        String nombreAlmacenado = UUID.randomUUID()
                + "."
                + extension;

        Path ruta = storageService.guardar(
                imagen,
                nombreAlmacenado);

        String hash = calcularSha256(
                imagen.getBytes());

        return new DatosArchivoImagen(
                nombreAlmacenado,
                ruta.toString(),
                hash,
                extension);
    }

    private void completarDatosArchivo(
            EvidenciaFotografica evidencia,
            MultipartFile imagen,
            DatosArchivoImagen datosArchivo) {

        evidencia.setNombreOriginal(
                imagen.getOriginalFilename());

        evidencia.setNombreAlmacenado(
                datosArchivo.nombreAlmacenado());

        evidencia.setMimeType(
                imagen.getContentType());

        evidencia.setExtension(
                datosArchivo.extension());

        evidencia.setTamanoBytes(
                imagen.getSize());

        evidencia.setRutaAlmacenamiento(
                datosArchivo.ruta());

        evidencia.setHashSha256(
                datosArchivo.hashSha256());
    }

    private String calcularSha256(
            byte[] contenido) {

        try {

            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            return HexFormat
                    .of()
                    .formatHex(
                            digest.digest(
                                    contenido));

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No fue posible calcular el hash de la fotografía.",
                    e);
        }
    }

    private String obtenerExtension(
            String nombreArchivo) {

        if (nombreArchivo == null
                || nombreArchivo.isBlank()
                || !nombreArchivo.contains(".")) {

            throw new IllegalArgumentException(
                    "La fotografía debe poseer una extensión válida.");
        }

        String extension = nombreArchivo.substring(
                nombreArchivo.lastIndexOf('.') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);

        if (extension.isBlank()) {

            throw new IllegalArgumentException(
                    "La fotografía debe poseer una extensión válida.");
        }

        return extension;
    }

    private String normalizarTexto(
            String texto) {

        return texto
                .trim()
                .replaceAll(
                        "\\s+",
                        " ");
    }

    private String normalizarObservacion(
            String observacion) {

        if (observacion == null
                || observacion.isBlank()) {

            return null;
        }

        return observacion
                .trim();
    }

    /*
     * Datos internos utilizados después de almacenar
     * físicamente una fotografía.
     */
    private record DatosArchivoImagen(
            String nombreAlmacenado,
            String ruta,
            String hashSha256,
            String extension) {
    }
}