package cl.farmaciasahumada.campannas.config;

import org.opencv.core.Core;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;

@Configuration
public class OpenCvConfig {

    @PostConstruct
    public void cargarOpenCv() {

        /*
         * OpenPnP empaqueta las librerías nativas de OpenCV.
         *
         * En Java 12 o superior se recomienda loadLocally().
         */
        OpenCV.loadLocally();

        System.out.println(
                "OpenCV cargado correctamente. Versión: "
                        + Core.VERSION);
    }
}