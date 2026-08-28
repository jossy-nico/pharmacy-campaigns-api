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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.farmaciasahumada.campannas.dto.ArchivoResponse;
import cl.farmaciasahumada.campannas.model.Archivo;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.ArchivoRegistro;
import cl.farmaciasahumada.campannas.repository.ArchivoRegistroRepository;
import cl.farmaciasahumada.campannas.repository.ArchivoRepository;
import cl.farmaciasahumada.campannas.service.archivo.ArchivoSchemaRegistry;
import cl.farmaciasahumada.campannas.service.archivo.ArchivoStorageService;
import cl.farmaciasahumada.campannas.service.archivo.EsquemaArchivo;
import cl.farmaciasahumada.campannas.service.archivo.FilaTabular;
import cl.farmaciasahumada.campannas.service.archivo.LectorTabularGenerico;
import cl.farmaciasahumada.campannas.service.archivo.SincronizadorDestinoGenerico;
import cl.farmaciasahumada.campannas.service.archivo.SincronizadorGenerico;
import cl.farmaciasahumada.campannas.service.archivo.DocumentoTabular;
import cl.farmaciasahumada.campannas.service.archivo.EsquemaTabularInferido;
import cl.farmaciasahumada.campannas.service.archivo.InferidorEsquemaTabular;
import cl.farmaciasahumada.campannas.service.archivo.DocumentoTabular;
import cl.farmaciasahumada.campannas.service.archivo.EsquemaTabularInferido;
import cl.farmaciasahumada.campannas.service.archivo.GestorTablaDinamica;

@Service
public class ArchivoService {

        private final ArchivoRepository archivoRepository;
        private final ArchivoSchemaRegistry schemaRegistry;
        private final ArchivoStorageService storageService;
        private final ArchivoRegistroRepository archivoRegistroRepository;
        private final LectorTabularGenerico lectorTabularGenerico;
        private final SincronizadorGenerico sincronizadorGenerico;
        private final SincronizadorDestinoGenerico sincronizadorDestinoGenerico;
        private final InferidorEsquemaTabular inferidorEsquemaTabular;
        private final GestorTablaDinamica gestorTablaDinamica;

        public ArchivoService(
                        ArchivoRepository archivoRepository,
                        ArchivoSchemaRegistry schemaRegistry,
                        ArchivoStorageService storageService,
                        ArchivoRegistroRepository archivoRegistroRepository,
                        LectorTabularGenerico lectorTabularGenerico,
                        SincronizadorGenerico sincronizadorGenerico,
                        SincronizadorDestinoGenerico sincronizadorDestinoGenerico,
                        InferidorEsquemaTabular inferidorEsquemaTabular,
                        GestorTablaDinamica gestorTablaDinamica) {

                this.archivoRepository = archivoRepository;
                this.schemaRegistry = schemaRegistry;
                this.storageService = storageService;
                this.archivoRegistroRepository = archivoRegistroRepository;
                this.lectorTabularGenerico = lectorTabularGenerico;
                this.sincronizadorGenerico = sincronizadorGenerico;
                this.sincronizadorDestinoGenerico = sincronizadorDestinoGenerico;
                this.inferidorEsquemaTabular = inferidorEsquemaTabular;
                this.gestorTablaDinamica = gestorTablaDinamica;
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

        @Transactional
        public Archivo cargar(
                        String tipoArchivo,
                        MultipartFile archivo) throws IOException {

                if (archivo == null || archivo.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "El archivo no puede estar vacío.");
                }

                // 1. Busca la configuración en la BD
                EsquemaArchivo esquema = schemaRegistry.obtener(tipoArchivo);

                ArchivoDefinicion definicion = esquema.definicion();

                // 2. Para FARMACIAS, por ejemplo:
                // FARMACIAS será siempre el mismo grupo de versiones.
                String grupoVersion = definicion.getCodigo();

                // 3. Busca última versión
                Optional<Archivo> ultimoArchivo = archivoRepository
                                .findTopByGrupoVersionOrderByVersionDesc(
                                                grupoVersion);

                int nuevaVersion = ultimoArchivo
                                .map(a -> a.getVersion() + 1)
                                .orElse(1);

                // 4. Genera nombre interno único
                String extension = obtenerExtension(
                                archivo.getOriginalFilename());

                String nombreAlmacenado = UUID.randomUUID()
                                + (extension.isBlank()
                                                ? ""
                                                : "." + extension);

                // 5. Guarda físicamente el archivo
                Path ruta = storageService.guardar(
                                archivo,
                                nombreAlmacenado);

                // 6. Calcula SHA-256
                String hash = calcularSha256(
                                archivo.getBytes());

                // 7. Si existe una versión activa anterior,
                // pasa a REEMPLAZADO
                archivoRepository
                                .findByGrupoVersionAndEstadoArchivo(
                                                grupoVersion,
                                                "ACTIVO")
                                .ifPresent(anterior -> {

                                        anterior.setEstadoArchivo(
                                                        "REEMPLAZADO");

                                        anterior.setFechaModificacion(
                                                        LocalDateTime.now());

                                        /*
                                         * IMPORTANTE:
                                         * forzamos el UPDATE en PostgreSQL antes
                                         * de insertar la nueva versión ACTIVA.
                                         */
                                        archivoRepository.saveAndFlush(anterior);
                                });

                // 8. Registra nueva versión
                Archivo nuevo = new Archivo();

                nuevo.setDefinicion(definicion);

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

                nuevo.setDatosAdicionales(
                                new LinkedHashMap<>());

                nuevo.setDatosOrigen(
                                new LinkedHashMap<>());

                ultimoArchivo.ifPresent(
                                nuevo::setArchivoAnterior);

                Archivo archivoGuardado = archivoRepository.saveAndFlush(nuevo);

                /*
                 * Si el archivo es TABULAR,
                 * procesamos sus filas mediante el lector genérico.
                 */
                if ("TABULAR".equalsIgnoreCase(
                                definicion.getCategoria())) {

                        List<FilaTabular> filas = lectorTabularGenerico.leer(
                                        archivo,
                                        esquema);

                        boolean tieneErrores = false;

                        for (FilaTabular fila : filas) {

                                ArchivoRegistro registro = new ArchivoRegistro();

                                registro.setArchivo(
                                                archivoGuardado);

                                registro.setNumeroFila(
                                                fila.numeroFila());

                                registro.setClaveNegocio(
                                                fila.claveNegocio());

                                registro.setDatos(
                                                fila.datos());

                                registro.setDatosAdicionales(
                                                fila.datosAdicionales());

                                registro.setValido(
                                                fila.esValida());

                                registro.setErrores(
                                                fila.errores());

                                if (!fila.esValida()) {
                                        tieneErrores = true;
                                }

                                archivoRegistroRepository.save(
                                                registro);
                        }

                        sincronizadorGenerico.sincronizar(
                                        archivoGuardado,
                                        definicion,
                                        filas);

                        sincronizadorDestinoGenerico.sincronizar(
                                        esquema,
                                        filas);

                        archivoGuardado.setEstadoProcesamiento(
                                        tieneErrores
                                                        ? "PROCESADO_CON_ADVERTENCIAS"
                                                        : "PROCESADO");

                        archivoGuardado = archivoRepository.save(
                                        archivoGuardado);
                }

                return archivoGuardado;
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
        public Archivo reemplazar(
                        Long id,
                        MultipartFile nuevoArchivo) throws IOException {

                Archivo archivoActual = archivoRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe el archivo con id: " + id));

                if (!"ACTIVO".equalsIgnoreCase(
                                archivoActual.getEstadoArchivo())) {

                        throw new IllegalArgumentException(
                                        "Solo se puede reemplazar la versión ACTIVA del archivo.");
                }

                /*
                 * Utilizamos la definición del archivo actual.
                 * El usuario no necesita volver a indicar FARMACIAS,
                 * PRODUCTOS_PAI, etc.
                 */
                String tipoArchivo = archivoActual
                                .getDefinicion()
                                .getCodigo();

                return cargar(
                                tipoArchivo,
                                nuevoArchivo);
        }

        @Transactional
        public Map<String, Object> crearTablaDinamica(
                        String nombreTabla,
                        MultipartFile archivo) throws IOException {

                if (nombreTabla == null || nombreTabla.isBlank()) {
                        throw new IllegalArgumentException(
                                        "El nombre de la tabla es obligatorio.");
                }

                if (archivo == null || archivo.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "El archivo no puede estar vacío.");
                }

                /*
                 * 1. Lee automáticamente todas las hojas.
                 */
                DocumentoTabular documento = lectorTabularGenerico.leerAutomatico(
                                archivo);

                /*
                 * 2. Detecta automáticamente columnas y tipos.
                 */
                EsquemaTabularInferido esquema = inferidorEsquemaTabular.inferir(
                                documento);

                /*
                 * 3. Crea/actualiza la tabla e inserta los datos.
                 *
                 * GestorTablaDinamica no sabe si esto corresponde
                 * a Mascotas, Clientes, Productos PAI, etc.
                 */
                String tablaCreada = gestorTablaDinamica.crearOActualizarYCargar(
                                nombreTabla,
                                documento,
                                esquema);

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put(
                                "tabla",
                                tablaCreada);

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
}