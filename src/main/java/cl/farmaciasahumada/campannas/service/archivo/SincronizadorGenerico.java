package cl.farmaciasahumada.campannas.service.archivo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import cl.farmaciasahumada.campannas.model.Archivo;
import cl.farmaciasahumada.campannas.model.ArchivoDefinicion;
import cl.farmaciasahumada.campannas.model.RegistroGenerico;
import cl.farmaciasahumada.campannas.repository.RegistroGenericoRepository;

@Component
public class SincronizadorGenerico {

    private final RegistroGenericoRepository registroRepository;

    public SincronizadorGenerico(
            RegistroGenericoRepository registroRepository) {

        this.registroRepository = registroRepository;
    }

    public ResultadoSincronizacion sincronizar(
            Archivo archivo,
            ArchivoDefinicion definicion,
            List<FilaTabular> filas) {

        int creados = 0;
        int actualizados = 0;
        int inactivados = 0;

        Set<String> clavesRecibidas =
                new HashSet<>();

        for (FilaTabular fila : filas) {

            /*
             * Las filas inválidas quedan en archivo_registro
             * como histórico, pero NO modifican el estado actual.
             */
            if (!fila.esValida()
                    || fila.claveNegocio() == null
                    || fila.claveNegocio().isBlank()) {

                continue;
            }

            String clave =
                    fila.claveNegocio();

            clavesRecibidas.add(clave);

            RegistroGenerico registro =
                    registroRepository
                            .findByDefinicionIdAndClaveNegocio(
                                    definicion.getId(),
                                    clave
                            )
                            .orElse(null);

            if (registro == null) {

                registro =
                        new RegistroGenerico();

                registro.setDefinicion(
                        definicion
                );

                registro.setClaveNegocio(
                        clave
                );

                registro.setFechaCreacion(
                        LocalDateTime.now()
                );

                creados++;

            } else {

                actualizados++;
            }

            registro.setDatos(
                    fila.datos()
            );

            registro.setDatosAdicionales(
                    fila.datosAdicionales()
            );

            registro.setActivo(
                    true
            );

            registro.setArchivoOrigen(
                    archivo
            );

            registro.setFechaActualizacion(
                    LocalDateTime.now()
            );

            registroRepository.save(
                    registro
            );
        }

        /*
         * Para modos de sincronización donde el archivo representa
         * el estado completo actual, inactivamos lo que ya no venga.
         */
        if ("UPSERT_INACTIVAR".equalsIgnoreCase(
                definicion.getModoSincronizacion())) {

            List<RegistroGenerico> activos =
                    registroRepository
                            .findByDefinicionIdAndActivoTrue(
                                    definicion.getId()
                            );

            for (RegistroGenerico registro : activos) {

                if (!clavesRecibidas.contains(
                        registro.getClaveNegocio())) {

                    registro.setActivo(
                            false
                    );

                    registro.setFechaActualizacion(
                            LocalDateTime.now()
                    );

                    registroRepository.save(
                            registro
                    );

                    inactivados++;
                }
            }
        }

        return new ResultadoSincronizacion(
                creados,
                actualizados,
                inactivados
        );
    }
}