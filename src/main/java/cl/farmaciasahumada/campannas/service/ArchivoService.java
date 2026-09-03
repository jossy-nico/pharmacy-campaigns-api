package cl.farmaciasahumada.campannas.service;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.farmaciasahumada.campannas.dto.ArchivoResponse;
import cl.farmaciasahumada.campannas.model.Archivo;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.repository.ArchivoRepository;
import cl.farmaciasahumada.campannas.repository.ArchivoDefinicionRepository;

import cl.farmaciasahumada.campannas.service.archivo.ArchivoStorageService;
import cl.farmaciasahumada.campannas.service.archivo.LectorTabularGenerico;
import cl.farmaciasahumada.campannas.service.archivo.ValidadorDatasetGenerico;
import cl.farmaciasahumada.campannas.service.archivo.DocumentoTabular;
import cl.farmaciasahumada.campannas.service.archivo.EsquemaTabularInferido;
import cl.farmaciasahumada.campannas.service.archivo.InferidorEsquemaTabular;
import cl.farmaciasahumada.campannas.service.archivo.GestorTablaDinamica;

@Service
public class ArchivoService {

        private final ArchivoRepository archivoRepository;
        private final ArchivoStorageService storageService;
        private final LectorTabularGenerico lectorTabularGenerico;
        private final InferidorEsquemaTabular inferidorEsquemaTabular;
        private final GestorTablaDinamica gestorTablaDinamica;
        private final ArchivoDefinicionRepository archivoDefinicionRepository;
        private final ValidadorDatasetGenerico validadorDatasetGenerico;

        public ArchivoService(
                        ArchivoRepository archivoRepository,
                        ArchivoStorageService storageService,
                        LectorTabularGenerico lectorTabularGenerico,
                        InferidorEsquemaTabular inferidorEsquemaTabular,
                        GestorTablaDinamica gestorTablaDinamica,
                        ArchivoDefinicionRepository archivoDefinicionRepository,
                        ValidadorDatasetGenerico validadorDatasetGenerico) {

                this.archivoRepository = archivoRepository;
                this.storageService = storageService;
                this.lectorTabularGenerico = lectorTabularGenerico;
                this.inferidorEsquemaTabular = inferidorEsquemaTabular;
                this.gestorTablaDinamica = gestorTablaDinamica;
                this.archivoDefinicionRepository = archivoDefinicionRepository;
                this.validadorDatasetGenerico = validadorDatasetGenerico;
        }

        public EsquemaTabularInferido analizar(
                        MultipartFile archivo) throws IOException {

                if (archivo == null || archivo.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "El archivo no puede estar vacío.");
                }

                DocumentoTabular documento = lectorTabularGenerico.leerAutomatico(
                                archivo);

                return inferidorEsquemaTabular.inferir(
                                documento);
        }

        private String obtenerExtension(
                        String nombreArchivo) {

                if (nombreArchivo == null ||
                                !nombreArchivo.contains(".")) {

                        return "";
                }

                return nombreArchivo
                                .substring(
                                                nombreArchivo.lastIndexOf(".") + 1)
                                .toLowerCase();
        }

        private String calcularSha256(
                        byte[] contenido) {

                try {

                        MessageDigest digest = MessageDigest.getInstance(
                                        "SHA-256");

                        byte[] hash = digest.digest(contenido);

                        return HexFormat
                                        .of()
                                        .formatHex(hash);

                } catch (Exception e) {

                        throw new IllegalStateException(
                                        "No fue posible calcular SHA-256.",
                                        e);
                }
        }

        public List<ArchivoResponse> listar() {

                return archivoRepository
                                .findAll()
                                .stream()
                                .map(this::convertirResponse)
                                .toList();
        }

        public ArchivoResponse obtener(Long id) {

                Archivo archivo = archivoRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe el archivo con id: " + id));

                return convertirResponse(archivo);
        }

        public List<ArchivoResponse> historial(
                        String grupoVersion) {

                return archivoRepository
                                .findByGrupoVersionOrderByVersionDesc(
                                                grupoVersion.toUpperCase())
                                .stream()
                                .map(this::convertirResponse)
                                .toList();
        }

        private ArchivoResponse convertirResponse(
                        Archivo archivo) {

                return new ArchivoResponse(
                                archivo.getId(),
                                archivo.getDefinicion().getCodigo(),
                                archivo.getNombreOriginal(),
                                archivo.getVersion(),
                                archivo.getEstadoArchivo(),
                                archivo.getEstadoProcesamiento(),
                                archivo.getTamanoBytes(),
                                archivo.getHashSha256(),
                                archivo.getOrigen(),
                                archivo.getFechaCarga());
        }

        @Transactional
        public void eliminar(Long id) {

                Archivo archivo = archivoRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe el archivo con id: " + id));

                /*
                 * DELETE idempotente:
                 * si ya está eliminado, no hacemos nada.
                 */
                if ("ELIMINADO".equalsIgnoreCase(
                                archivo.getEstadoArchivo())) {

                        return;
                }

                archivo.setEstadoArchivo(
                                "ELIMINADO");

                archivo.setFechaEliminacion(
                                LocalDateTime.now());

                archivo.setFechaModificacion(
                                LocalDateTime.now());

                archivoRepository.save(archivo);
        }

        @Transactional
        public Map<String, Object> crearDatasetNuevo(
                        String nombreDataset,
                        MultipartFile archivo) throws IOException {

                validarArchivoDinamico(
                                nombreDataset,
                                archivo);

                String codigoDataset = normalizarCodigoDataset(
                                nombreDataset);

                if (archivoDefinicionRepository
                                .existsByCodigoIgnoreCase(
                                                codigoDataset)) {

                        throw new IllegalArgumentException(
                                        "Ya existe un dataset registrado con el código: "
                                                        + codigoDataset);
                }

                /*
                 * Antes de registrar el dataset comprobamos
                 * que el archivo realmente pueda ser leído.
                 */
                lectorTabularGenerico.leerAutomatico(
                                archivo);

                ArchivoDefinicion definicion = new ArchivoDefinicion();
                definicion.setPoliticaRetencion(
                                "HISTORICO_COMPLETO");

                definicion.setMaxVersionesRetenidas(
                                null);

                definicion.setCodigo(
                                codigoDataset);

                definicion.setNombre(
                                nombreDataset.trim());

                definicion.setCategoria(
                                "TABULAR");

                definicion.setFormatosPermitidos(
                                "[\"xlsx\",\"xls\",\"csv\"]");

                /*
                 * El encabezado se detecta automáticamente.
                 */

                definicion.setActivo(
                                true);

                definicion.setFechaCreacion(
                                LocalDateTime.now());

                ArchivoDefinicion guardada = archivoDefinicionRepository
                                .saveAndFlush(
                                                definicion);
                /*
                 * La primera carga del dataset utiliza
                 * el mismo flujo versionado que las cargas posteriores.
                 */
                return cargarDatasetExistente(
                                guardada.getCodigo(),
                                null,
                                archivo);

        }

        public Map<String, Object> obtenerDatasetActivo(
                        String codigoDataset,
                        String periodoReferencia) {

                if (codigoDataset == null
                                || codigoDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Debe seleccionar un dataset.");
                }

                String codigoNormalizado = normalizarCodigoDataset(
                                codigoDataset);

                ArchivoDefinicion definicion = archivoDefinicionRepository
                                .findByCodigoIgnoreCaseAndActivoTrue(
                                                codigoNormalizado)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El dataset '"
                                                                + codigoNormalizado
                                                                + "' no está registrado."));
                validarDatasetTabular(
                                definicion);
                String grupoVersion = construirGrupoVersionDinamico(
                                definicion.getCodigo(),
                                periodoReferencia);

                Archivo archivoActivo = archivoRepository
                                .findByGrupoVersionAndEstadoArchivo(
                                                grupoVersion,
                                                "ACTIVO")
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe una versión ACTIVA para el dataset '"
                                                                + definicion.getCodigo()
                                                                + "'"
                                                                + (periodoReferencia == null
                                                                                || periodoReferencia.isBlank()
                                                                                                ? "."
                                                                                                : " en el período "
                                                                                                                + periodoReferencia
                                                                                                                + ".")));

                List<Map<String, Object>> registros = gestorTablaDinamica.obtenerSnapshot(
                                definicion.getCodigo(),
                                archivoActivo.getId());

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put(
                                "datasetId",
                                definicion.getId());

                respuesta.put(
                                "codigoDataset",
                                definicion.getCodigo());

                respuesta.put(
                                "nombreDataset",
                                definicion.getNombre());

                respuesta.put(
                                "periodoReferencia",
                                periodoReferencia);

                respuesta.put(
                                "archivoId",
                                archivoActivo.getId());

                respuesta.put(
                                "grupoVersion",
                                archivoActivo.getGrupoVersion());

                respuesta.put(
                                "version",
                                archivoActivo.getVersion());

                respuesta.put(
                                "estadoArchivo",
                                archivoActivo.getEstadoArchivo());

                respuesta.put(
                                "cantidadRegistros",
                                registros.size());

                respuesta.put(
                                "registros",
                                registros);

                return respuesta;
        }

        @Transactional
        public Map<String, Object> cargarDatasetExistente(
                        String codigoDataset,
                        String periodoReferencia,
                        MultipartFile archivo) throws IOException {

                if (codigoDataset == null
                                || codigoDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Debe seleccionar un dataset.");
                }

                if (archivo == null
                                || archivo.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "El archivo no puede estar vacío.");
                }

                String codigoNormalizado = normalizarCodigoDataset(
                                codigoDataset);

                ArchivoDefinicion definicion = archivoDefinicionRepository
                                .findByCodigoIgnoreCaseAndActivoTrue(
                                                codigoNormalizado)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El dataset '"
                                                                + codigoNormalizado
                                                                + "' no está registrado. "
                                                                + "Debe crearlo antes de cargar información."));
                validarDatasetTabular(
                                definicion);

                DocumentoTabular documento = lectorTabularGenerico.leerAutomatico(
                                archivo);
                validadorDatasetGenerico.validar(
                                definicion,
                                documento);

                EsquemaTabularInferido esquema = inferidorEsquemaTabular.inferir(
                                documento);

                /*
                 * El grupo determina qué cargas pertenecen
                 * a la misma línea de versiones.
                 *
                 * Con período:
                 * DINAMICO:PRODUCTOS_PAI:2026-09
                 *
                 * Sin período:
                 * DINAMICO:CLIENTES
                 */
                String grupoVersion = construirGrupoVersionDinamico(
                                definicion.getCodigo(),
                                periodoReferencia);

                Optional<Archivo> ultimoArchivo = archivoRepository
                                .findTopByGrupoVersionOrderByVersionDesc(
                                                grupoVersion);

                int nuevaVersion = ultimoArchivo
                                .map(a -> a.getVersion() + 1)
                                .orElse(1);

                /*
                 * Si existe una versión ACTIVA
                 * del MISMO grupo/período,
                 * pasa a REEMPLAZADO.
                 *
                 * Agosto no reemplaza septiembre
                 * porque tienen grupos diferentes.
                 */
                archivoRepository
                                .findByGrupoVersionAndEstadoArchivo(
                                                grupoVersion,
                                                "ACTIVO")
                                .ifPresent(anterior -> {

                                        anterior.setEstadoArchivo(
                                                        "REEMPLAZADO");

                                        anterior.setFechaModificacion(
                                                        LocalDateTime.now());

                                        archivoRepository.saveAndFlush(
                                                        anterior);
                                });

                String extension = obtenerExtension(
                                archivo.getOriginalFilename());

                String nombreAlmacenado = UUID.randomUUID()
                                + (extension.isBlank()
                                                ? ""
                                                : "." + extension);

                Path ruta = storageService.guardar(
                                archivo,
                                nombreAlmacenado);

                String hash = calcularSha256(
                                archivo.getBytes());

                Archivo nuevo = new Archivo();

                nuevo.setDefinicion(
                                definicion);

                nuevo.setGrupoVersion(
                                grupoVersion);

                nuevo.setVersion(
                                nuevaVersion);

                nuevo.setEstadoArchivo(
                                "ACTIVO");

                nuevo.setEstadoProcesamiento(
                                "PENDIENTE");

                nuevo.setNombreOriginal(
                                archivo.getOriginalFilename());

                nuevo.setNombreAlmacenado(
                                nombreAlmacenado);

                nuevo.setMimeType(
                                archivo.getContentType());

                nuevo.setExtension(
                                extension);

                nuevo.setTamanoBytes(
                                archivo.getSize());

                nuevo.setRutaAlmacenamiento(
                                ruta.toString());

                nuevo.setHashSha256(
                                hash);

                nuevo.setOrigen(
                                "CARGA_WEB");

                nuevo.setFechaCarga(
                                LocalDateTime.now());

                Map<String, Object> datosAdicionales = new LinkedHashMap<>();

                datosAdicionales.put(
                                "dataset",
                                definicion.getCodigo());

                if (periodoReferencia != null
                                && !periodoReferencia.isBlank()) {

                        datosAdicionales.put(
                                        "periodoReferencia",
                                        periodoReferencia.trim());
                }

                nuevo.setDatosAdicionales(
                                datosAdicionales);

                nuevo.setDatosOrigen(
                                new LinkedHashMap<>());

                ultimoArchivo.ifPresent(
                                nuevo::setArchivoAnterior);

                Archivo archivoGuardado = archivoRepository.saveAndFlush(
                                nuevo);

                /*
                 * NUEVO MOTOR:
                 *
                 * no borra registros anteriores.
                 * Todas las filas quedan asociadas
                 * al archivo/version que las originó.
                 */
                String tabla = gestorTablaDinamica.agregarSnapshot(
                                definicion.getCodigo(),
                                documento,
                                esquema,
                                archivoGuardado.getId());

                archivoGuardado.setEstadoProcesamiento(
                                "PROCESADO");

                archivoRepository.saveAndFlush(
                                archivoGuardado);

                /*
                 * Una vez guardada correctamente la nueva versión,
                 * aplicamos la política genérica de retención.
                 */
                aplicarPoliticaRetencion(
                                definicion,
                                grupoVersion);

                archivoRepository.save(
                                archivoGuardado);

                Map<String, Object> respuesta = construirRespuestaDataset(
                                definicion,
                                tabla,
                                documento,
                                esquema);

                respuesta.put(
                                "archivoId",
                                archivoGuardado.getId());

                respuesta.put(
                                "grupoVersion",
                                grupoVersion);

                respuesta.put(
                                "version",
                                nuevaVersion);

                respuesta.put(
                                "estadoArchivo",
                                archivoGuardado.getEstadoArchivo());

                respuesta.put(
                                "periodoReferencia",
                                periodoReferencia);

                return respuesta;
        }

        // método auxiliar
        private String construirGrupoVersionDinamico(
                        String codigoDataset,
                        String periodoReferencia) {

                String grupo = "DINAMICO:"
                                + normalizarCodigoDataset(
                                                codigoDataset);

                if (periodoReferencia == null
                                || periodoReferencia.isBlank()) {

                        return grupo;
                }

                String periodo = periodoReferencia
                                .trim()
                                .toUpperCase(Locale.ROOT);

                return grupo
                                + ":"
                                + periodo;
        }

        private void aplicarPoliticaRetencion(
                        ArchivoDefinicion definicion,
                        String grupoVersion) {

                String politica = definicion.getPoliticaRetencion();

                /*
                 * Por seguridad, si no existe política
                 * conservamos todo el histórico.
                 */
                if (politica == null
                                || politica.isBlank()
                                || "HISTORICO_COMPLETO".equalsIgnoreCase(
                                                politica)) {

                        return;
                }

                /*
                 * Las versiones vienen ordenadas:
                 *
                 * V3
                 * V2
                 * V1
                 */
                List<Archivo> versiones = archivoRepository
                                .findByGrupoVersionOrderByVersionDesc(
                                                grupoVersion);

                int cantidadConservar;

                switch (politica
                                .trim()
                                .toUpperCase(Locale.ROOT)) {

                        case "SOLO_ACTIVO" -> {

                                cantidadConservar = 1;
                        }

                        case "ULTIMAS_N_VERSIONES" -> {

                                Integer maxVersiones = definicion.getMaxVersionesRetenidas();

                                if (maxVersiones == null
                                                || maxVersiones <= 0) {

                                        throw new IllegalStateException(
                                                        "La política ULTIMAS_N_VERSIONES "
                                                                        + "requiere un número de versiones mayor a cero.");
                                }

                                cantidadConservar = maxVersiones;
                        }

                        default -> throw new IllegalStateException(
                                        "Política de retención no soportada: "
                                                        + politica);
                }

                /*
                 * Todavía no superamos el límite.
                 */
                if (versiones.size() <= cantidadConservar) {
                        return;
                }

                /*
                 * Las primeras N versiones se conservan.
                 *
                 * Desde N en adelante eliminamos únicamente
                 * las filas físicas del snapshot.
                 *
                 * El registro public.archivo permanece para
                 * conservar auditoría e historial.
                 */
                for (int i = cantidadConservar; i < versiones.size(); i++) {

                        Archivo versionAntigua = versiones.get(i);

                        gestorTablaDinamica.eliminarSnapshot(
                                        definicion.getCodigo(),
                                        versionAntigua.getId());
                }
        }

        private void validarArchivoDinamico(
                        String nombreDataset,
                        MultipartFile archivo) {

                if (nombreDataset == null
                                || nombreDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El nombre del dataset es obligatorio.");
                }

                if (archivo == null
                                || archivo.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "El archivo no puede estar vacío.");
                }
        }

        private String normalizarCodigoDataset(
                        String nombre) {

                String normalizado = Normalizer.normalize(
                                nombre,
                                Normalizer.Form.NFD)
                                .replaceAll("\\p{M}", "")
                                .toUpperCase(Locale.ROOT)
                                .trim()
                                .replaceAll("[^A-Z0-9]+", "_")
                                .replaceAll("^_+|_+$", "")
                                .replaceAll("_+", "_");

                if (normalizado.isBlank()) {
                        throw new IllegalArgumentException(
                                        "El nombre del dataset no es válido.");
                }

                return normalizado;
        }

        private Map<String, Object> construirRespuestaDataset(
                        ArchivoDefinicion definicion,
                        String tabla,
                        DocumentoTabular documento,
                        EsquemaTabularInferido esquema) {

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put(
                                "datasetId",
                                definicion.getId());

                respuesta.put(
                                "codigoDataset",
                                definicion.getCodigo());

                respuesta.put(
                                "nombreDataset",
                                definicion.getNombre());

                respuesta.put(
                                "tabla",
                                tabla);

                respuesta.put(
                                "cantidadHojas",
                                documento.cantidadHojas());

                respuesta.put(
                                "cantidadRegistros",
                                documento.cantidadRegistros());

                respuesta.put(
                                "cantidadColumnas",
                                esquema.columnas().size());

                respuesta.put(
                                "columnas",
                                esquema.columnas());

                return respuesta;
        }

        @Transactional
        public Map<String, Object> actualizarPoliticaRetencion(
                        String codigoDataset,
                        String politicaRetencion,
                        Integer maxVersionesRetenidas) {

                if (codigoDataset == null
                                || codigoDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Debe seleccionar un dataset.");
                }

                if (politicaRetencion == null
                                || politicaRetencion.isBlank()) {

                        throw new IllegalArgumentException(
                                        "La política de retención es obligatoria.");
                }

                String codigoNormalizado = normalizarCodigoDataset(
                                codigoDataset);

                ArchivoDefinicion definicion = archivoDefinicionRepository
                                .findByCodigoIgnoreCaseAndActivoTrue(
                                                codigoNormalizado)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El dataset '"
                                                                + codigoNormalizado
                                                                + "' no está registrado."));

                validarDatasetTabular(
                                definicion);

                String politica = politicaRetencion
                                .trim()
                                .toUpperCase(Locale.ROOT);

                switch (politica) {

                        case "HISTORICO_COMPLETO",
                                        "SOLO_ACTIVO" -> {

                                definicion.setMaxVersionesRetenidas(
                                                null);
                        }

                        case "ULTIMAS_N_VERSIONES" -> {

                                if (maxVersionesRetenidas == null
                                                || maxVersionesRetenidas <= 0) {

                                        throw new IllegalArgumentException(
                                                        "ULTIMAS_N_VERSIONES requiere "
                                                                        + "un número de versiones mayor a cero.");
                                }

                                definicion.setMaxVersionesRetenidas(
                                                maxVersionesRetenidas);
                        }

                        default -> throw new IllegalArgumentException(
                                        "Política de retención no válida: "
                                                        + politicaRetencion);
                }

                definicion.setPoliticaRetencion(
                                politica);

                ArchivoDefinicion guardada = archivoDefinicionRepository.saveAndFlush(
                                definicion);

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put(
                                "codigoDataset",
                                guardada.getCodigo());

                respuesta.put(
                                "politicaRetencion",
                                guardada.getPoliticaRetencion());

                respuesta.put(
                                "maxVersionesRetenidas",
                                guardada.getMaxVersionesRetenidas());

                return respuesta;
        }

        @Transactional
        public Map<String, Object> desactivarDataset(
                        String codigoDataset) {

                if (codigoDataset == null
                                || codigoDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Debe indicar el dataset.");
                }

                String codigoNormalizado = normalizarCodigoDataset(
                                codigoDataset);

                ArchivoDefinicion definicion = archivoDefinicionRepository
                                .findByCodigoIgnoreCase(
                                                codigoNormalizado)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El dataset '"
                                                                + codigoNormalizado
                                                                + "' no está registrado."));

                if (!Boolean.TRUE.equals(
                                definicion.getActivo())) {

                        throw new IllegalArgumentException(
                                        "El dataset '"
                                                        + definicion.getCodigo()
                                                        + "' ya se encuentra inactivo.");
                }

                definicion.setActivo(false);

                ArchivoDefinicion guardada = archivoDefinicionRepository
                                .saveAndFlush(
                                                definicion);

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put(
                                "codigoDataset",
                                guardada.getCodigo());

                respuesta.put(
                                "nombreDataset",
                                guardada.getNombre());

                respuesta.put(
                                "activo",
                                guardada.getActivo());

                return respuesta;
        }

        @Transactional
        public Map<String, Object> reactivarDataset(
                        String codigoDataset) {

                if (codigoDataset == null
                                || codigoDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Debe indicar el dataset.");
                }

                String codigoNormalizado = normalizarCodigoDataset(
                                codigoDataset);

                ArchivoDefinicion definicion = archivoDefinicionRepository
                                .findByCodigoIgnoreCase(
                                                codigoNormalizado)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El dataset '"
                                                                + codigoNormalizado
                                                                + "' no está registrado."));

                if (Boolean.TRUE.equals(
                                definicion.getActivo())) {

                        throw new IllegalArgumentException(
                                        "El dataset '"
                                                        + definicion.getCodigo()
                                                        + "' ya se encuentra activo.");
                }

                definicion.setActivo(true);

                ArchivoDefinicion guardada = archivoDefinicionRepository
                                .saveAndFlush(
                                                definicion);

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put(
                                "codigoDataset",
                                guardada.getCodigo());

                respuesta.put(
                                "nombreDataset",
                                guardada.getNombre());

                respuesta.put(
                                "activo",
                                guardada.getActivo());

                return respuesta;
        }

        public List<Map<String, Object>> listarDatasets() {

                return convertirDatasets(
                                archivoDefinicionRepository
                                                .findAllByActivoTrueOrderByNombreAsc());
        }

        public List<Map<String, Object>> listarDatasets(
                        String categoria) {

                return listarDatasets(
                                categoria,
                                "ACTIVO");
        }

        public List<Map<String, Object>> listarDatasets(
                        String categoria,
                        String estado) {

                String estadoNormalizado = estado == null
                                || estado.isBlank()
                                                ? "ACTIVO"
                                                : estado.trim()
                                                                .toUpperCase(Locale.ROOT);

                String categoriaNormalizada;

                if (categoria == null || categoria.isBlank()) {
                        categoriaNormalizada = null;
                } else {
                        categoriaNormalizada = categoria.trim();
                }

                List<ArchivoDefinicion> definiciones;

                switch (estadoNormalizado) {

                        case "ACTIVO" -> {

                                if (categoriaNormalizada == null) {

                                        definiciones = archivoDefinicionRepository
                                                        .findAllByActivoTrueOrderByNombreAsc();

                                } else {

                                        definiciones = archivoDefinicionRepository
                                                        .findAllByCategoriaIgnoreCaseAndActivoTrueOrderByNombreAsc(
                                                                        categoriaNormalizada);
                                }
                        }

                        case "INACTIVO" -> {

                                if (categoriaNormalizada == null) {

                                        definiciones = archivoDefinicionRepository
                                                        .findAllByActivoFalseOrderByNombreAsc();

                                } else {

                                        definiciones = archivoDefinicionRepository
                                                        .findAllByCategoriaIgnoreCaseAndActivoFalseOrderByNombreAsc(
                                                                        categoriaNormalizada);
                                }
                        }

                        case "TODOS" -> {

                                definiciones = archivoDefinicionRepository
                                                .findAll()
                                                .stream()
                                                .filter(definicion -> categoriaNormalizada == null
                                                                || definicion.getCategoria()
                                                                                .equalsIgnoreCase(
                                                                                                categoriaNormalizada))
                                                .sorted((a, b) -> a.getNombre()
                                                                .compareToIgnoreCase(
                                                                                b.getNombre()))
                                                .toList();
                        }

                        default -> throw new IllegalArgumentException(
                                        "Estado de dataset no válido: "
                                                        + estado
                                                        + ". Valores permitidos: "
                                                        + "ACTIVO, INACTIVO o TODOS.");
                }

                return convertirDatasets(
                                definiciones);
        }

        private List<Map<String, Object>> convertirDatasets(
                        List<ArchivoDefinicion> definiciones) {

                return definiciones
                                .stream()
                                .map(definicion -> {

                                        Map<String, Object> dataset = new LinkedHashMap<>();

                                        dataset.put(
                                                        "id",
                                                        definicion.getId());

                                        dataset.put(
                                                        "codigo",
                                                        definicion.getCodigo());

                                        dataset.put(
                                                        "nombre",
                                                        definicion.getNombre());

                                        dataset.put(
                                                        "categoria",
                                                        definicion.getCategoria());
                                        dataset.put(
                                                        "activo",
                                                        definicion.getActivo());
                                        dataset.put(
                                                        "politicaRetencion",
                                                        definicion.getPoliticaRetencion());

                                        dataset.put(
                                                        "maxVersionesRetenidas",
                                                        definicion.getMaxVersionesRetenidas());

                                        return dataset;
                                })
                                .toList();
        }

        private void validarDatasetTabular(
                        ArchivoDefinicion definicion) {

                if (!"TABULAR".equalsIgnoreCase(
                                definicion.getCategoria())) {

                        throw new IllegalArgumentException(
                                        "El dataset '"
                                                        + definicion.getCodigo()
                                                        + "' no es de tipo TABULAR.");
                }
        }
}