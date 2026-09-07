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

        private static final String TIPO_REFERENCIA_OFICIAL = "REFERENCIA_OFICIAL";
        private static final String TIPO_EVIDENCIA_ZONAL = "EVIDENCIA_ZONAL";
        private static final String TIPO_EVIDENCIA_FARMACIA = "EVIDENCIA_FARMACIA";

        private static final String ESTADO_CARGADA = "CARGADA";
        private static final String ORIGEN_CARGA_WEB = "CARGA_WEB";

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
         * REFERENCIA OFICIAL DEL PLANOGRAMA
         * =========================================================
         */

        @Transactional
        public EvidenciaFotografica subirReferenciaOficial(
                        Long campaniaId,
                        String exhibidor,
                        String vista,
                        MultipartFile imagen,
                        String observacion) throws Exception {

                Campania campania = obtenerCampania(campaniaId);

                validarEstadoParaReferenciaOficial(campania);

                validarTextoObligatorio(
                                exhibidor,
                                "El exhibidor es obligatorio.");

                validarTextoObligatorio(
                                vista,
                                "La vista de la fotografía es obligatoria.");

                validarImagen(imagen);

                DatosArchivoImagen datosArchivo = almacenarImagen(imagen);

                EvidenciaFotografica evidencia = new EvidenciaFotografica();

                evidencia.setCampaniaId(campania.getId());

                /*
                 * La referencia oficial pertenece a la campaña,
                 * no a una farmacia específica.
                 */
                evidencia.setFarmaciaId(null);

                /*
                 * Una referencia oficial no apunta
                 * a otra referencia.
                 */
                evidencia.setReferenciaOficialId(null);

                evidencia.setTipoEvidencia(
                                TIPO_REFERENCIA_OFICIAL);

                evidencia.setExhibidor(
                                normalizarTexto(exhibidor));

                evidencia.setVista(
                                normalizarTexto(vista));

                completarDatosArchivo(
                                evidencia,
                                imagen,
                                datosArchivo);

                completarDatosGenerales(
                                evidencia,
                                observacion);

                return evidenciaRepository.saveAndFlush(evidencia);
        }

        /*
         * =========================================================
         * EVIDENCIA ZONAL
         * =========================================================
         */

        @Transactional
        public EvidenciaFotografica subirEvidenciaZonal(
                        Long campaniaId,
                        Long farmaciaId,
                        Long referenciaOficialId,
                        MultipartFile imagen,
                        String observacion) throws Exception {

                return subirEvidenciaOperacional(
                                campaniaId,
                                farmaciaId,
                                referenciaOficialId,
                                imagen,
                                observacion,
                                TIPO_EVIDENCIA_ZONAL);
        }

        /*
         * =========================================================
         * EVIDENCIA FARMACIA
         * =========================================================
         */

        @Transactional
        public EvidenciaFotografica subirEvidenciaFarmacia(
                        Long campaniaId,
                        Long farmaciaId,
                        Long referenciaOficialId,
                        MultipartFile imagen,
                        String observacion) throws Exception {

                return subirEvidenciaOperacional(
                                campaniaId,
                                farmaciaId,
                                referenciaOficialId,
                                imagen,
                                observacion,
                                TIPO_EVIDENCIA_FARMACIA);
        }

        /*
         * =========================================================
         * CARGA COMÚN:
         * EVIDENCIA ZONAL / EVIDENCIA FARMACIA
         * =========================================================
         */

        private EvidenciaFotografica subirEvidenciaOperacional(
                        Long campaniaId,
                        Long farmaciaId,
                        Long referenciaOficialId,
                        MultipartFile imagen,
                        String observacion,
                        String tipoEvidencia) throws Exception {

                Campania campania = obtenerCampania(campaniaId);

                validarCampaniaActivaParaEvidencia(
                                campania,
                                tipoEvidencia);

                validarFarmacia(farmaciaId);

                if (referenciaOficialId == null) {

                        throw new IllegalArgumentException(
                                        "Debe seleccionar la referencia oficial del planograma.");
                }

                EvidenciaFotografica referenciaOficial = evidenciaRepository
                                .findById(referenciaOficialId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe la referencia oficial con id: "
                                                                                + referenciaOficialId));

                validarReferenciaOficial(
                                referenciaOficial,
                                campaniaId);

                validarImagen(imagen);

                DatosArchivoImagen datosArchivo = almacenarImagen(imagen);

                EvidenciaFotografica evidencia = new EvidenciaFotografica();

                evidencia.setCampaniaId(
                                campania.getId());

                evidencia.setFarmaciaId(
                                farmaciaId);

                evidencia.setReferenciaOficialId(
                                referenciaOficial.getId());

                evidencia.setTipoEvidencia(
                                tipoEvidencia);

                /*
                 * Exhibidor y vista se obtienen directamente
                 * de la referencia oficial.
                 *
                 * De esta forma una evidencia zonal o de farmacia
                 * no puede asociarse manualmente a una vista
                 * distinta de la que corresponde.
                 */
                evidencia.setExhibidor(
                                referenciaOficial.getExhibidor());

                evidencia.setVista(
                                referenciaOficial.getVista());

                completarDatosArchivo(
                                evidencia,
                                imagen,
                                datosArchivo);

                completarDatosGenerales(
                                evidencia,
                                observacion);

                return evidenciaRepository.saveAndFlush(evidencia);
        }

        /*
         * =========================================================
         * CONSULTAS
         * =========================================================
         */

        public EvidenciaFotografica obtenerPorId(
                        Long id) {

                if (id == null) {

                        throw new IllegalArgumentException(
                                        "El id de la evidencia es obligatorio.");
                }

                return evidenciaRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe la evidencia con id: "
                                                                                + id));
        }

        public List<EvidenciaFotografica> listarPorCampania(
                        Long campaniaId) {

                obtenerCampania(campaniaId);

                return evidenciaRepository
                                .findAllByCampaniaIdOrderByFechaCargaDesc(
                                                campaniaId);
        }

        public List<EvidenciaFotografica> listarReferenciasOficiales(
                        Long campaniaId) {

                obtenerCampania(campaniaId);

                return evidenciaRepository
                                .findAllByCampaniaIdAndTipoEvidenciaOrderByFechaCargaDesc(
                                                campaniaId,
                                                TIPO_REFERENCIA_OFICIAL);
        }

        public List<EvidenciaFotografica> listarEvidenciasZonales(
                        Long campaniaId) {

                obtenerCampania(campaniaId);

                return evidenciaRepository
                                .findAllByCampaniaIdAndTipoEvidenciaOrderByFechaCargaDesc(
                                                campaniaId,
                                                TIPO_EVIDENCIA_ZONAL);
        }

        public List<EvidenciaFotografica> listarEvidenciasFarmacia(
                        Long campaniaId) {

                obtenerCampania(campaniaId);

                return evidenciaRepository
                                .findAllByCampaniaIdAndTipoEvidenciaOrderByFechaCargaDesc(
                                                campaniaId,
                                                TIPO_EVIDENCIA_FARMACIA);
        }

        public List<EvidenciaFotografica> listarPorFarmacia(
                        Long campaniaId,
                        Long farmaciaId) {

                obtenerCampania(campaniaId);

                validarFarmacia(farmaciaId);

                return evidenciaRepository
                                .findAllByCampaniaIdAndFarmaciaIdOrderByFechaCargaDesc(
                                                campaniaId,
                                                farmaciaId);
        }

        public List<EvidenciaFotografica> listarPorReferenciaOficial(
                        Long referenciaOficialId) {

                if (referenciaOficialId == null) {

                        throw new IllegalArgumentException(
                                        "La referencia oficial es obligatoria.");
                }

                EvidenciaFotografica referencia = evidenciaRepository
                                .findById(referenciaOficialId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe la referencia oficial con id: "
                                                                                + referenciaOficialId));

                if (!TIPO_REFERENCIA_OFICIAL.equalsIgnoreCase(
                                referencia.getTipoEvidencia())) {

                        throw new IllegalArgumentException(
                                        "La evidencia seleccionada no corresponde "
                                                        + "a una referencia oficial.");
                }

                return evidenciaRepository
                                .findAllByReferenciaOficialIdOrderByFechaCargaDesc(
                                                referenciaOficialId);
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
                                .findById(campaniaId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "No existe la campaña con id: "
                                                                                + campaniaId));
        }

        private void validarEstadoParaReferenciaOficial(
                        Campania campania) {

                String estado = normalizarEstado(
                                campania.getEstado());

                /*
                 * La referencia oficial puede cargarse
                 * antes de iniciar la campaña o mientras
                 * esta se encuentra activa.
                 */
                if (!"PROGRAMADA".equals(estado)
                                && !"ACTIVA".equals(estado)) {

                        throw new IllegalArgumentException(
                                        "Las referencias oficiales solo pueden cargarse "
                                                        + "cuando la campaña está PROGRAMADA o ACTIVA.");
                }
        }

        private void validarCampaniaActivaParaEvidencia(
                        Campania campania,
                        String tipoEvidencia) {

                if (!"ACTIVA".equals(
                                normalizarEstado(campania.getEstado()))) {

                        if (TIPO_EVIDENCIA_ZONAL.equals(tipoEvidencia)) {

                                throw new IllegalArgumentException(
                                                "El zonal solo puede cargar evidencias "
                                                                + "para una campaña ACTIVA.");
                        }

                        throw new IllegalArgumentException(
                                        "La farmacia solo puede cargar evidencias "
                                                        + "para una campaña ACTIVA.");
                }
        }

        private void validarFarmacia(
                        Long farmaciaId) {

                if (farmaciaId == null) {

                        throw new IllegalArgumentException(
                                        "La farmacia es obligatoria.");
                }

                if (!farmaciaRepository.existsById(farmaciaId)) {

                        throw new IllegalArgumentException(
                                        "No existe la farmacia con id: "
                                                        + farmaciaId);
                }
        }

        private void validarReferenciaOficial(
                        EvidenciaFotografica referencia,
                        Long campaniaId) {

                if (!TIPO_REFERENCIA_OFICIAL.equalsIgnoreCase(
                                referencia.getTipoEvidencia())) {

                        throw new IllegalArgumentException(
                                        "La evidencia seleccionada no corresponde "
                                                        + "a una referencia oficial del planograma.");
                }

                if (!campaniaId.equals(
                                referencia.getCampaniaId())) {

                        throw new IllegalArgumentException(
                                        "La referencia oficial seleccionada "
                                                        + "pertenece a otra campaña.");
                }

                /*
                 * Una referencia oficial:
                 *
                 * - no pertenece a una farmacia
                 * - no puede apuntar a otra referencia oficial
                 */
                if (referencia.getFarmaciaId() != null
                                || referencia.getReferenciaOficialId() != null) {

                        throw new IllegalArgumentException(
                                        "La referencia seleccionada "
                                                        + "no posee una estructura válida de referencia oficial.");
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

                String extension = obtenerExtension(nombre);

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

                        /*
                         * application/octet-stream se admite porque
                         * algunas herramientas como Postman pueden
                         * enviar imágenes utilizando ese MIME genérico.
                         */
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
         * DATOS COMUNES
         * =========================================================
         */

        private void completarDatosGenerales(
                        EvidenciaFotografica evidencia,
                        String observacion) {

                evidencia.setOrigen(
                                ORIGEN_CARGA_WEB);

                evidencia.setEstado(
                                ESTADO_CARGADA);

                evidencia.setResultado(
                                null);

                evidencia.setObservacion(
                                normalizarObservacion(observacion));

                /*
                 * Se mantiene null hasta implementar
                 * autenticación y usuarios.
                 */
                evidencia.setUsuarioCarga(
                                null);

                evidencia.setFechaCarga(
                                OffsetDateTime.now());
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
                                                        digest.digest(contenido));

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

                String extension = nombreArchivo
                                .substring(
                                                nombreArchivo.lastIndexOf('.') + 1)
                                .trim()
                                .toLowerCase(Locale.ROOT);

                if (extension.isBlank()) {

                        throw new IllegalArgumentException(
                                        "La fotografía debe poseer una extensión válida.");
                }

                return extension;
        }

        /*
         * =========================================================
         * NORMALIZACIÓN
         * =========================================================
         */

        private String normalizarEstado(
                        String estado) {

                if (estado == null) {
                        return "";
                }

                return estado
                                .trim()
                                .toUpperCase(Locale.ROOT);
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

                return observacion.trim();
        }

        /*
         * =========================================================
         * DATOS INTERNOS DE ARCHIVO
         * =========================================================
         */

        private record DatosArchivoImagen(
                        String nombreAlmacenado,
                        String ruta,
                        String hashSha256,
                        String extension) {
        }
}