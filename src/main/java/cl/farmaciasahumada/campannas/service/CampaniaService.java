package cl.farmaciasahumada.campannas.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.farmaciasahumada.campannas.model.Archivo;
import cl.farmaciasahumada.campannas.model.Campania;
import cl.farmaciasahumada.campannas.model.ConfiguracionSistema;
import cl.farmaciasahumada.campannas.repository.ArchivoRepository;
import cl.farmaciasahumada.campannas.repository.CampaniaRepository;
import cl.farmaciasahumada.campannas.repository.ConfiguracionSistemaRepository;
import cl.farmaciasahumada.campannas.service.archivo.GestorTablaDinamica;

@Service
public class CampaniaService {

    private static final String DATASET_PRODUCTOS_PAI = "PRODUCTOS_PAI";

    private static final String CLAVE_DIA_LIMITE =
            "DIA_LIMITE_CARGA_CAMPANIA";

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "BORRADOR",
            "PROGRAMADA",
            "ACTIVA",
            "FINALIZADA",
            "CANCELADA");

    /*
     * Ranking no está incluido porque sabemos que
     * puede venir vacío legítimamente en algunos
     * exhibidores.
     */
    private static final List<String> CAMPOS_PAI_OBLIGATORIOS = List.of(
            "id_sku",
            "descriptor_descripcion",
            "marca",
            "ubicacion",
            "tipo_exhibicion");

    private static final Set<String> VALORES_NO_VALIDOS = Set.of(
            "*",
            "**",
            "-",
            "--",
            "N/A",
            "NA",
            "NULL");

    private final CampaniaRepository campaniaRepository;
    private final ArchivoRepository archivoRepository;
    private final ConfiguracionSistemaRepository configuracionRepository;
    private final GestorTablaDinamica gestorTablaDinamica;

    public CampaniaService(
            CampaniaRepository campaniaRepository,
            ArchivoRepository archivoRepository,
            ConfiguracionSistemaRepository configuracionRepository,
            GestorTablaDinamica gestorTablaDinamica) {

        this.campaniaRepository = campaniaRepository;
        this.archivoRepository = archivoRepository;
        this.configuracionRepository = configuracionRepository;
        this.gestorTablaDinamica = gestorTablaDinamica;
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

        /*
         * El código técnico nunca lo escribe
         * manualmente el usuario.
         */
        campania.setCodigo(
                generarCodigo(periodo));

        /*
         * Fechas calculadas automáticamente.
         */
        campania.setFechaInicio(
                periodo.atDay(1));

        campania.setFechaFin(
                periodo.atEndOfMonth());

        campania.setFechaLimiteCarga(
                fechaLimiteCarga);

        /*
         * Guardamos exactamente la versión del
         * archivo validado.
         */
        campania.setArchivoProductosId(
                archivoProductos.getId());

        /*
         * El usuario tampoco define manualmente
         * el estado inicial.
         */
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
    public Campania cambiarEstado(
            Long id,
            String estado) {

        Campania campania = obtenerPorId(
                id);

        validarEstado(
                estado);

        campania.setEstado(
                estado.trim()
                        .toUpperCase(Locale.ROOT));

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
                    "Debe cargar y seleccionar el archivo "
                            + "de productos PRODUCTOS_PAI.");
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

        /*
         * Permitimos:
         * letras, incluyendo acentos,
         * números,
         * espacios,
         * guión bajo.
         *
         * No se permiten *, /, \, @, etc.
         */
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
                            "Ya existe una campaña PAI para el período "
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
                        "No existe el archivo de productos con id: "
                                + archivoId));

        if (archivo.getDefinicion() == null
                || !DATASET_PRODUCTOS_PAI.equalsIgnoreCase(
                        archivo.getDefinicion().getCodigo())) {

            throw new IllegalArgumentException(
                    "El archivo seleccionado no pertenece "
                            + "al dataset PRODUCTOS_PAI.");
        }

        if (!"PROCESADO".equalsIgnoreCase(
                archivo.getEstadoProcesamiento())) {

            throw new IllegalArgumentException(
                    "El archivo PRODUCTOS_PAI todavía no "
                            + "ha sido procesado correctamente.");
        }

        /*
         * Para generar una campaña utilizamos la
         * versión ACTIVA del período.
         */
        if (!"ACTIVO".equalsIgnoreCase(
                archivo.getEstadoArchivo())) {

            throw new IllegalArgumentException(
                    "El archivo PRODUCTOS_PAI seleccionado "
                            + "no corresponde a la versión ACTIVA.");
        }

        String grupoEsperado =
                "DINAMICO:"
                        + DATASET_PRODUCTOS_PAI
                        + ":"
                        + String.format(
                                "%04d-%02d",
                                periodo.getYear(),
                                periodo.getMonthValue());

        if (archivo.getGrupoVersion() == null
                || !grupoEsperado.equalsIgnoreCase(
                        archivo.getGrupoVersion())) {

            throw new IllegalArgumentException(
                    "El archivo PRODUCTOS_PAI no corresponde "
                            + "al período de la campaña. "
                            + "Se esperaba: "
                            + periodo);
        }

        /*
         * Validamos el snapshot exacto asociado
         * a este archivo/version.
         */
        List<Map<String, Object>> registros =
                gestorTablaDinamica.obtenerSnapshot(
                        DATASET_PRODUCTOS_PAI,
                        archivo.getId());

        if (registros.isEmpty()) {

            throw new IllegalArgumentException(
                    "El archivo PRODUCTOS_PAI no contiene registros.");
        }

        validarRegistrosPai(
                registros);

        /*
         * No validamos unicidad de SHA-256.
         *
         * Octubre y noviembre pueden tener exactamente
         * el mismo contenido y ambas cargas son válidas.
         */
        return archivo;
    }

    private void validarRegistrosPai(
            List<Map<String, Object>> registros) {

        for (Map<String, Object> registro : registros) {

            for (String campo : CAMPOS_PAI_OBLIGATORIOS) {

                Object valor = registro.get(
                        campo);

                if (!esDatoValido(valor)) {

                    Object hoja = registro.get(
                            "_sys_hoja");

                    Object numeroFila = registro.get(
                            "_sys_numero_fila");

                    throw new IllegalArgumentException(
                            "El archivo PRODUCTOS_PAI contiene "
                                    + "un dato obligatorio vacío o inválido. "
                                    + "Campo: "
                                    + campo
                                    + ", hoja: "
                                    + hoja
                                    + ", fila: "
                                    + numeroFila
                                    + ".");
                }
            }
        }
    }

    private boolean esDatoValido(
            Object valor) {

        if (valor == null) {
            return false;
        }

        String texto = valor
                .toString()
                .trim();

        if (texto.isBlank()) {
            return false;
        }

        return !VALORES_NO_VALIDOS.contains(
                texto.toUpperCase(Locale.ROOT));
    }

    private LocalDate calcularFechaLimiteCarga(
            YearMonth periodo) {

        ConfiguracionSistema configuracion =
                configuracionRepository
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

        YearMonth mesAnterior = periodo
                .minusMonths(1);

        /*
         * Si se configura 31 y el mes anterior
         * tiene solo 30 días, usamos el último
         * día válido de ese mes.
         */
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

    private void validarEstado(
            String estado) {

        if (estado == null
                || estado.isBlank()) {

            throw new IllegalArgumentException(
                    "El estado de la campaña es obligatorio.");
        }

        String estadoNormalizado = estado
                .trim()
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