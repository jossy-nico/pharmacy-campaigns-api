package cl.farmaciasahumada.campannas.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.farmaciasahumada.campannas.model.Farmacia;
import cl.farmaciasahumada.campannas.repository.FarmaciaRepository;

@Service
public class FarmaciaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "ACTIVA",
            "INACTIVA",
            "CERRADA_TEMPORALMENTE",
            "CERRADA_DEFINITIVAMENTE");

    private final FarmaciaRepository farmaciaRepository;

    public FarmaciaService(
            FarmaciaRepository farmaciaRepository) {

        this.farmaciaRepository = farmaciaRepository;
    }

    public List<Farmacia> listar() {

        return farmaciaRepository.findAll();
    }

    public Farmacia obtenerPorId(
            Long id) {

        return farmaciaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la farmacia con id: " + id));
    }

    public Farmacia obtenerPorCodigo(
            String codigoFarmacia) {

        validarCodigo(
                codigoFarmacia);

        return farmaciaRepository
                .findByCodigoFarmacia(
                        codigoFarmacia.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la farmacia con código: "
                                + codigoFarmacia));
    }

    @Transactional
    public Farmacia crear(
            Farmacia farmacia) {

        validarFarmacia(
                farmacia);

        String codigo = farmacia
                .getCodigoFarmacia()
                .trim();

        if (farmaciaRepository
                .findByCodigoFarmacia(codigo)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Ya existe una farmacia con código: "
                            + codigo);
        }

        farmacia.setId(
                null);

        farmacia.setCodigoFarmacia(
                codigo);

        /*
         * Si al crearla no se informa estado,
         * la farmacia comienza ACTIVA.
         */
        if (farmacia.getEstado() == null
                || farmacia.getEstado().isBlank()) {

            farmacia.setEstado(
                    "ACTIVA");
        }

        validarEstado(
                farmacia.getEstado());

        farmacia.setEstado(
                farmacia.getEstado()
                        .trim()
                        .toUpperCase());

        return farmaciaRepository.save(
                farmacia);
    }

    @Transactional
    public Farmacia actualizar(
            Long id,
            Farmacia datos) {

        Farmacia farmacia = obtenerPorId(
                id);

        validarFarmacia(
                datos);

        String codigoNuevo = datos
                .getCodigoFarmacia()
                .trim();

        farmaciaRepository
                .findByCodigoFarmacia(
                        codigoNuevo)
                .filter(encontrada -> !encontrada
                        .getId()
                        .equals(id))
                .ifPresent(encontrada -> {

                    throw new IllegalArgumentException(
                            "Ya existe otra farmacia con código: "
                                    + codigoNuevo);
                });

        farmacia.setCodigoFarmacia(
                codigoNuevo);

        farmacia.setDireccion(
                datos.getDireccion());

        farmacia.setSubgerente(
                datos.getSubgerente());

        farmacia.setAdministradorZonal(
                datos.getAdministradorZonal());

        farmacia.setMercado(
                datos.getMercado());

        farmacia.setCiudad(
                datos.getCiudad());

        farmacia.setComuna(
                datos.getComuna());

        farmacia.setRegion(
                datos.getRegion());

        farmacia.setFormatoComercial(
                datos.getFormatoComercial());

        farmacia.setClasificacion(
                datos.getClasificacion());

        /*
         * El estado no se modifica mediante el PUT general.
         * Tendrá su propia operación.
         */

        return farmaciaRepository.save(
                farmacia);
    }

    @Transactional
    public Farmacia cambiarEstado(
            Long id,
            String estado) {

        Farmacia farmacia = obtenerPorId(
                id);

        validarEstado(
                estado);

        farmacia.setEstado(
                estado.trim()
                        .toUpperCase());

        return farmaciaRepository.save(
                farmacia);
    }

    private void validarFarmacia(
            Farmacia farmacia) {

        if (farmacia == null) {

            throw new IllegalArgumentException(
                    "Los datos de la farmacia son obligatorios.");
        }

        validarCodigo(
                farmacia.getCodigoFarmacia());
    }

    private void validarCodigo(
            String codigoFarmacia) {

        if (codigoFarmacia == null
                || codigoFarmacia.isBlank()) {

            throw new IllegalArgumentException(
                    "El código de farmacia es obligatorio.");
        }
    }

    private void validarEstado(
            String estado) {

        if (estado == null
                || estado.isBlank()) {

            throw new IllegalArgumentException(
                    "El estado de la farmacia es obligatorio.");
        }

        String estadoNormalizado = estado
                .trim()
                .toUpperCase();

        if (!ESTADOS_VALIDOS.contains(
                estadoNormalizado)) {

            throw new IllegalArgumentException(
                    "Estado de farmacia no válido: "
                            + estado
                            + ". Valores permitidos: "
                            + String.join(
                                    ", ",
                                    ESTADOS_VALIDOS));
        }
    }
}