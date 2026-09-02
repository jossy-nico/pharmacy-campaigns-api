package cl.farmaciasahumada.campannas.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.farmaciasahumada.campannas.model.Archivo;
import cl.farmaciasahumada.campannas.model.Campania;
import cl.farmaciasahumada.campannas.model.CampaniaAuditoria;
import cl.farmaciasahumada.campannas.model.ConfiguracionSistema;
import cl.farmaciasahumada.campannas.repository.ArchivoRepository;
import cl.farmaciasahumada.campannas.repository.CampaniaAuditoriaRepository;
import cl.farmaciasahumada.campannas.repository.CampaniaRepository;
import cl.farmaciasahumada.campannas.repository.ConfiguracionSistemaRepository;
import cl.farmaciasahumada.campannas.service.archivo.GestorTablaDinamica;

@Service
public class CampaniaService {

        private static final String CLAVE_DIA_LIMITE = "DIA_LIMITE_CARGA_CAMPANIA";

        private static final Set<String> ESTADOS_VALIDOS = Set.of(
                        "BORRADOR",
                        "PROGRAMADA",
                        "ACTIVA",
                        "FINALIZADA",
                        "CANCELADA");

        private final CampaniaRepository campaniaRepository;
        private final ArchivoRepository archivoRepository;
        private final ConfiguracionSistemaRepository configuracionRepository;
        private final GestorTablaDinamica gestorTablaDinamica;
        private final CampaniaAuditoriaRepository campaniaAuditoriaRepository;

        public CampaniaService(
                        CampaniaRepository campaniaRepository,
                        ArchivoRepository archivoRepository,
                        ConfiguracionSistemaRepository configuracionRepository,
                        GestorTablaDinamica gestorTablaDinamica,
                        CampaniaAuditoriaRepository campaniaAuditoriaRepository) {

                this.campaniaRepository = campaniaRepository;
                this.archivoRepository = archivoRepository;
                this.configuracionRepository = configuracionRepository;
                this.gestorTablaDinamica = gestorTablaDinamica;
                this.campaniaAuditoriaRepository = campaniaAuditoriaRepository;
        }

        public List<Campania> listarCampanias() {

                return campaniaRepository
                                .findAllByOrderByAnioDescMesDesc();
        }

        public Campania obtenerPorId(
                        Long id) {

                return campaniaRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe la campaña con id: " + id));
        }

        public List<Map<String, Object>> obtenerHistorialAuditoria(
                        Long campaniaId) {

                /*
                 * Primero comprobamos que la campaña exista.
                 */
                obtenerPorId(
                                campaniaId);

                return campaniaAuditoriaRepository
                                .findAllByCampaniaIdOrderByFechaModificacionDesc(
                                                campaniaId)
                                .stream()
                                .map(auditoria -> {

                                        Map<String, Object> registro = new LinkedHashMap<>();

                                        registro.put(
                                                        "id",
                                                        auditoria.getId());

                                        registro.put(
                                                        "campaniaId",
                                                        auditoria.getCampania().getId());

                                        registro.put(
                                                        "accion",
                                                        auditoria.getAccion());

                                        registro.put(
                                                        "motivo",
                                                        auditoria.getMotivo());

                                        registro.put(
                                                        "datosAnteriores",
                                                        auditoria.getDatosAnteriores());

                                        registro.put(
                                                        "datosNuevos",
                                                        auditoria.getDatosNuevos());

                                        registro.put(
                                                        "usuario",
                                                        auditoria.getUsuario());

                                        registro.put(
                                                        "fechaModificacion",
                                                        auditoria.getFechaModificacion());

                                        return registro;
                                })
                                .toList();
        }

        @Transactional
        public Campania guardarCampania(
                        Campania campania) {

                validarDatosEntrada(
                                campania);

                YearMonth periodo = obtenerPeriodo(
                                campania.getAnio(),
                                campania.getMes());

                validarPeriodoDisponible(
                                campania.getAnio(),
                                campania.getMes(),
                                null);

                Archivo archivoProductos = validarArchivoProductos(
                                campania.getArchivoProductosId(),
                                periodo);

                LocalDate fechaLimiteCarga = calcularFechaLimiteCarga(
                                periodo);

                validarPlazoCarga(
                                fechaLimiteCarga);

                campania.setId(
                                null);

                campania.setNombre(
                                campania.getNombre().trim());

                campania.setCodigo(
                                generarCodigo(periodo));

                campania.setFechaInicio(
                                periodo.atDay(1));

                campania.setFechaFin(
                                periodo.atEndOfMonth());

                campania.setFechaLimiteCarga(
                                fechaLimiteCarga);

                campania.setArchivoProductosId(
                                archivoProductos.getId());

                campania.setEstado(
                                "BORRADOR");

                return campaniaRepository.saveAndFlush(
                                campania);
        }

        @Transactional
        public Campania actualizar(
                        Long id,
                        Campania datos) {

                Campania campania = obtenerPorId(
                                id);

                if (!"BORRADOR".equalsIgnoreCase(
                                campania.getEstado())) {

                        throw new IllegalArgumentException(
                                        "Solo se puede modificar una campaña "
                                                        + "que se encuentre en estado BORRADOR.");
                }

                validarDatosEntrada(
                                datos);

                YearMonth periodo = obtenerPeriodo(
                                datos.getAnio(),
                                datos.getMes());

                validarPeriodoDisponible(
                                datos.getAnio(),
                                datos.getMes(),
                                id);

                Archivo archivoProductos = validarArchivoProductos(
                                datos.getArchivoProductosId(),
                                periodo);

                LocalDate fechaLimiteCarga = calcularFechaLimiteCarga(
                                periodo);

                validarPlazoCarga(
                                fechaLimiteCarga);

                campania.setNombre(
                                datos.getNombre().trim());

                campania.setAnio(
                                datos.getAnio());

                campania.setMes(
                                datos.getMes());

                campania.setCodigo(
                                generarCodigo(periodo));

                campania.setFechaInicio(
                                periodo.atDay(1));

                campania.setFechaFin(
                                periodo.atEndOfMonth());

                campania.setFechaLimiteCarga(
                                fechaLimiteCarga);

                campania.setArchivoProductosId(
                                archivoProductos.getId());

                return campaniaRepository.saveAndFlush(
                                campania);
        }

        @Transactional
        public Campania corregirCampania(
                        Long id,
                        Campania datos,
                        String motivo) {

                validarMotivoCorreccion(
                                motivo);

                Campania campania = obtenerPorId(
                                id);

                validarDatosEntrada(
                                datos);

                Map<String, Object> datosAnteriores = construirSnapshotAuditoria(
                                campania);

                YearMonth periodo = obtenerPeriodo(
                                datos.getAnio(),
                                datos.getMes());

                validarPeriodoDisponible(
                                datos.getAnio(),
                                datos.getMes(),
                                id);

                Archivo archivoProductos = validarArchivoProductos(
                                datos.getArchivoProductosId(),
                                periodo);

                boolean cambioPeriodo = !campania.getAnio().equals(
                                datos.getAnio())
                                || !campania.getMes().equals(
                                                datos.getMes());

                campania.setNombre(
                                datos.getNombre().trim());

                campania.setAnio(
                                datos.getAnio());

                campania.setMes(
                                datos.getMes());

                campania.setCodigo(
                                generarCodigo(periodo));

                campania.setFechaInicio(
                                periodo.atDay(1));

                campania.setFechaFin(
                                periodo.atEndOfMonth());

                /*
                 * Si solo corregimos nombre o archivo,
                 * conservamos la fecha límite histórica.
                 *
                 * Si cambia realmente el período,
                 * calculamos la fecha correspondiente
                 * al nuevo período.
                 */
                if (cambioPeriodo) {

                        campania.setFechaLimiteCarga(
                                        calcularFechaLimiteCarga(
                                                        periodo));
                }

                campania.setArchivoProductosId(
                                archivoProductos.getId());

                /*
                 * La corrección NO cambia el estado.
                 * Si estaba ACTIVA, continúa ACTIVA.
                 */
                Campania corregida = campaniaRepository.saveAndFlush(
                                campania);

                Map<String, Object> datosNuevos = construirSnapshotAuditoria(
                                corregida);

                CampaniaAuditoria auditoria = new CampaniaAuditoria();

                auditoria.setCampania(
                                corregida);

                auditoria.setAccion(
                                "CORRECCION");

                auditoria.setMotivo(
                                motivo.trim());

                auditoria.setDatosAnteriores(
                                datosAnteriores);

                auditoria.setDatosNuevos(
                                datosNuevos);

                /*
                 * Temporalmente null.
                 * Cuando tengamos autenticación,
                 * se obtendrá automáticamente
                 * del usuario que realizó la corrección.
                 */
                auditoria.setUsuario(
                                null);

                auditoria.setFechaModificacion(
                                OffsetDateTime.now());

                campaniaAuditoriaRepository.saveAndFlush(
                                auditoria);

                return corregida;
        }

        @Transactional
        public Campania extenderCampania(
                        Long id,
                        LocalDate nuevaFechaFin,
                        String motivo) {

                Campania campania = obtenerPorId(
                                id);

                if (!"ACTIVA".equalsIgnoreCase(
                                campania.getEstado())) {

                        throw new IllegalArgumentException(
                                        "Solo se puede extender una campaña "
                                                        + "que se encuentre ACTIVA.");
                }

                if (nuevaFechaFin == null) {

                        throw new IllegalArgumentException(
                                        "La nueva fecha de término es obligatoria.");
                }

                if (campania.getFechaFin() == null) {

                        throw new IllegalArgumentException(
                                        "La campaña no posee una fecha de término válida.");
                }

                if (!nuevaFechaFin.isAfter(
                                campania.getFechaFin())) {

                        throw new IllegalArgumentException(
                                        "La nueva fecha de término debe ser posterior "
                                                        + "a la fecha de término actual.");
                }

                validarMotivoExtension(
                                motivo);

                Map<String, Object> datosAnteriores = construirSnapshotAuditoria(
                                campania);

                /*
                 * Una extensión modifica únicamente la fecha final.
                 *
                 * No cambia:
                 * - año
                 * - mes
                 * - código
                 * - archivo
                 * - estado
                 */
                campania.setFechaFin(
                                nuevaFechaFin);

                Campania extendida = campaniaRepository.saveAndFlush(
                                campania);

                Map<String, Object> datosNuevos = construirSnapshotAuditoria(
                                extendida);

                CampaniaAuditoria auditoria = new CampaniaAuditoria();

                auditoria.setCampania(
                                extendida);

                auditoria.setAccion(
                                "EXTENSION");

                auditoria.setMotivo(
                                motivo.trim());

                auditoria.setDatosAnteriores(
                                datosAnteriores);

                auditoria.setDatosNuevos(
                                datosNuevos);

                /*
                 * Temporalmente null hasta implementar
                 * autenticación y roles.
                 */
                auditoria.setUsuario(
                                null);

                auditoria.setFechaModificacion(
                                OffsetDateTime.now());

                campaniaAuditoriaRepository.saveAndFlush(
                                auditoria);

                return extendida;
        }

        @Transactional
        public Campania cambiarEstado(
                        Long id,
                        String estado) {

                Campania campania = obtenerPorId(
                                id);

                validarEstado(
                                estado);

                String nuevoEstado = estado
                                .trim()
                                .toUpperCase(Locale.ROOT);

                validarTransicionEstado(
                                campania.getEstado(),
                                nuevoEstado);

                campania.setEstado(
                                nuevoEstado);

                return campaniaRepository.saveAndFlush(
                                campania);
        }

        private void validarDatosEntrada(
                        Campania campania) {

                if (campania == null) {

                        throw new IllegalArgumentException(
                                        "Los datos de la campaña son obligatorios.");
                }

                validarNombre(
                                campania.getNombre());

                if (campania.getAnio() == null) {

                        throw new IllegalArgumentException(
                                        "El año de la campaña es obligatorio.");
                }

                if (campania.getMes() == null) {

                        throw new IllegalArgumentException(
                                        "El mes de la campaña es obligatorio.");
                }

                if (campania.getAnio() < 2000
                                || campania.getAnio() > 2100) {

                        throw new IllegalArgumentException(
                                        "El año de la campaña no es válido.");
                }

                if (campania.getMes() < 1
                                || campania.getMes() > 12) {

                        throw new IllegalArgumentException(
                                        "El mes debe estar entre 1 y 12.");
                }

                if (campania.getArchivoProductosId() == null) {

                        throw new IllegalArgumentException(
                                        "Debe cargar y seleccionar "
                                                        + "el archivo asociado a la campaña.");
                }
        }

        private void validarNombre(
                        String nombre) {

                if (nombre == null
                                || nombre.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El nombre de la campaña es obligatorio.");
                }

                String nombreNormalizado = nombre.trim();

                if (!nombreNormalizado.matches(
                                "^[\\p{L}\\p{N}_ ]+$")) {

                        throw new IllegalArgumentException(
                                        "El nombre de la campaña solo puede contener "
                                                        + "letras, números, espacios y guión bajo.");
                }
        }

        private YearMonth obtenerPeriodo(
                        Integer anio,
                        Integer mes) {

                try {

                        return YearMonth.of(
                                        anio,
                                        mes);

                } catch (Exception e) {

                        throw new IllegalArgumentException(
                                        "El período de la campaña no es válido.");
                }
        }

        private void validarPeriodoDisponible(
                        Integer anio,
                        Integer mes,
                        Long campaniaActualId) {

                campaniaRepository
                                .findByAnioAndMes(
                                                anio,
                                                mes)
                                .filter(encontrada -> campaniaActualId == null
                                                || !encontrada.getId()
                                                                .equals(campaniaActualId))
                                .ifPresent(encontrada -> {

                                        throw new IllegalArgumentException(
                                                        "Ya existe una campaña para el período "
                                                                        + String.format(
                                                                                        "%04d-%02d",
                                                                                        anio,
                                                                                        mes)
                                                                        + ".");
                                });
        }

        private Archivo validarArchivoProductos(
                        Long archivoId,
                        YearMonth periodo) {

                Archivo archivo = archivoRepository
                                .findById(archivoId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe el archivo con id: "
                                                                + archivoId));

                if (archivo.getDefinicion() == null) {

                        throw new IllegalArgumentException(
                                        "El archivo no posee una definición de dataset.");
                }

                /*
                 * CampaniaService no conoce columnas específicas.
                 *
                 * La estructura y las reglas del dataset
                 * ya fueron validadas durante su carga por
                 * ValidadorDatasetGenerico.
                 */
                if (!"TABULAR".equalsIgnoreCase(
                                archivo.getDefinicion().getCategoria())) {

                        throw new IllegalArgumentException(
                                        "El archivo seleccionado no corresponde "
                                                        + "a un dataset TABULAR.");
                }

                if (!"PROCESADO".equalsIgnoreCase(
                                archivo.getEstadoProcesamiento())) {

                        throw new IllegalArgumentException(
                                        "El archivo seleccionado todavía no "
                                                        + "ha sido procesado correctamente.");
                }

                if (!"ACTIVO".equalsIgnoreCase(
                                archivo.getEstadoArchivo())) {

                        throw new IllegalArgumentException(
                                        "El archivo seleccionado no corresponde "
                                                        + "a la versión ACTIVA.");
                }

                String codigoDataset = archivo.getDefinicion()
                                .getCodigo();

                if (codigoDataset == null
                                || codigoDataset.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El archivo no posee un código de dataset válido.");
                }

                String grupoEsperado = "DINAMICO:"
                                + codigoDataset
                                + ":"
                                + String.format(
                                                "%04d-%02d",
                                                periodo.getYear(),
                                                periodo.getMonthValue());

                if (archivo.getGrupoVersion() == null
                                || !grupoEsperado.equalsIgnoreCase(
                                                archivo.getGrupoVersion())) {

                        throw new IllegalArgumentException(
                                        "El archivo seleccionado no corresponde "
                                                        + "al período de la campaña. "
                                                        + "Se esperaba el período "
                                                        + periodo
                                                        + ".");
                }

                boolean tieneRegistros = !gestorTablaDinamica
                                .obtenerSnapshot(
                                                codigoDataset,
                                                archivo.getId())
                                .isEmpty();

                if (!tieneRegistros) {

                        throw new IllegalArgumentException(
                                        "El archivo seleccionado no contiene registros.");
                }

                return archivo;
        }

        private LocalDate calcularFechaLimiteCarga(
                        YearMonth periodo) {

                ConfiguracionSistema configuracion = configuracionRepository
                                .findByClaveIgnoreCase(
                                                CLAVE_DIA_LIMITE)
                                .orElseThrow(() -> new IllegalStateException(
                                                "No está configurado "
                                                                + CLAVE_DIA_LIMITE
                                                                + "."));

                int diaLimite;

                try {

                        diaLimite = Integer.parseInt(
                                        configuracion.getValor().trim());

                } catch (Exception e) {

                        throw new IllegalStateException(
                                        "La configuración "
                                                        + CLAVE_DIA_LIMITE
                                                        + " debe contener un número válido.");
                }

                if (diaLimite < 1
                                || diaLimite > 31) {

                        throw new IllegalStateException(
                                        "El día límite de carga debe estar "
                                                        + "entre 1 y 31.");
                }

                YearMonth mesAnterior = periodo.minusMonths(1);

                int diaAplicable = Math.min(
                                diaLimite,
                                mesAnterior.lengthOfMonth());

                return mesAnterior.atDay(
                                diaAplicable);
        }

        private void validarPlazoCarga(
                        LocalDate fechaLimiteCarga) {

                if (LocalDate.now()
                                .isAfter(fechaLimiteCarga)) {

                        throw new IllegalArgumentException(
                                        "El plazo de carga de esta campaña "
                                                        + "finalizó el "
                                                        + fechaLimiteCarga
                                                        + ".");
                }
        }

        private String generarCodigo(
                        YearMonth periodo) {

                return String.format(
                                "PAI_%04d_%02d",
                                periodo.getYear(),
                                periodo.getMonthValue());
        }

        private void validarMotivoCorreccion(
                        String motivo) {

                if (motivo == null
                                || motivo.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El motivo de la corrección es obligatorio.");
                }

                String motivoNormalizado = motivo.trim()
                                .toUpperCase(Locale.ROOT);

                if (motivoNormalizado.equals("*")
                                || motivoNormalizado.equals("-")
                                || motivoNormalizado.equals("--")
                                || motivoNormalizado.equals("N/A")
                                || motivoNormalizado.equals("NULL")) {

                        throw new IllegalArgumentException(
                                        "Debe ingresar un motivo de corrección válido.");
                }
        }

        private Map<String, Object> construirSnapshotAuditoria(
                        Campania campania) {

                Map<String, Object> datos = new LinkedHashMap<>();

                datos.put(
                                "id",
                                campania.getId());

                datos.put(
                                "codigo",
                                campania.getCodigo());

                datos.put(
                                "nombre",
                                campania.getNombre());

                datos.put(
                                "anio",
                                campania.getAnio());

                datos.put(
                                "mes",
                                campania.getMes());

                datos.put(
                                "fechaInicio",
                                campania.getFechaInicio() == null
                                                ? null
                                                : campania.getFechaInicio().toString());

                datos.put(
                                "fechaFin",
                                campania.getFechaFin() == null
                                                ? null
                                                : campania.getFechaFin().toString());

                datos.put(
                                "fechaLimiteCarga",
                                campania.getFechaLimiteCarga() == null
                                                ? null
                                                : campania.getFechaLimiteCarga().toString());

                datos.put(
                                "archivoProductosId",
                                campania.getArchivoProductosId());

                datos.put(
                                "estado",
                                campania.getEstado());

                return datos;
        }

        private void validarTransicionEstado(
                        String estadoActual,
                        String nuevoEstado) {

                if (estadoActual == null
                                || estadoActual.isBlank()) {

                        throw new IllegalArgumentException(
                                        "La campaña no posee un estado válido.");
                }

                String actual = estadoActual
                                .trim()
                                .toUpperCase(Locale.ROOT);

                /*
                 * Repetir el mismo estado es idempotente.
                 */
                if (actual.equals(
                                nuevoEstado)) {

                        return;
                }

                boolean transicionPermitida = switch (actual) {

                        case "BORRADOR" ->
                                "PROGRAMADA".equals(nuevoEstado)
                                                || "CANCELADA".equals(nuevoEstado);

                        case "PROGRAMADA" ->
                                "ACTIVA".equals(nuevoEstado)
                                                || "CANCELADA".equals(nuevoEstado);

                        case "ACTIVA" ->
                                "FINALIZADA".equals(nuevoEstado)
                                                || "CANCELADA".equals(nuevoEstado);

                        case "FINALIZADA",
                                        "CANCELADA" ->
                                false;

                        default -> false;
                };

                if (!transicionPermitida) {

                        throw new IllegalArgumentException(
                                        "No se permite cambiar una campaña de "
                                                        + actual
                                                        + " a "
                                                        + nuevoEstado
                                                        + ".");
                }
        }

        private void validarMotivoExtension(
                        String motivo) {

                if (motivo == null
                                || motivo.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El motivo de la extensión es obligatorio.");
                }

                String motivoNormalizado = motivo.trim()
                                .toUpperCase(Locale.ROOT);

                if (motivoNormalizado.equals("*")
                                || motivoNormalizado.equals("-")
                                || motivoNormalizado.equals("--")
                                || motivoNormalizado.equals("N/A")
                                || motivoNormalizado.equals("NULL")) {

                        throw new IllegalArgumentException(
                                        "Debe ingresar un motivo de extensión válido.");
                }
        }

        private void validarEstado(
                        String estado) {

                if (estado == null
                                || estado.isBlank()) {

                        throw new IllegalArgumentException(
                                        "El estado de la campaña es obligatorio.");
                }

                String estadoNormalizado = estado.trim()
                                .toUpperCase(Locale.ROOT);

                if (!ESTADOS_VALIDOS.contains(
                                estadoNormalizado)) {

                        throw new IllegalArgumentException(
                                        "Estado de campaña no válido: "
                                                        + estado
                                                        + ". Valores permitidos: "
                                                        + String.join(
                                                                        ", ",
                                                                        ESTADOS_VALIDOS));
                }
        }
}