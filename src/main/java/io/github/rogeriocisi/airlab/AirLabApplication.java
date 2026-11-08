package io.github.rogeriocisi.airlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Crivo (codiname AIR Lab): a modular monolith for blind mediumship research protocols.
 *
 * <p>Started from the command line on Windows and Linux alike, with no container runtime
 * involved (Constitution, architectural rule 8). See {@code scripts/start.sh} and
 * {@code scripts/start.ps1}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AirLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirLabApplication.class, args);
    }
}
