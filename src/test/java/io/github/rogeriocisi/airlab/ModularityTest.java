package io.github.rogeriocisi.airlab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Architectural rule 1: a module reaches another only through its public API or through
 * domain events, and that is verified by a test, not by discipline.
 *
 * <p>Architectural rule 2 as well: only {@code participant} touches the {@code identity}
 * schema, which here means only its own {@code internal} package declares those entities.
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(AirLabApplication.class);

    @Test
    @DisplayName("as fronteiras de modulo e as dependencias declaradas sao respeitadas")
    void module_boundaries_hold() {
        MODULES.verify();
    }

    @Test
    @DisplayName("os modulos esperados existem")
    void expected_modules_are_present() {
        for (String name : new String[]{"audit", "allocation", "study", "participant",
                "redaction", "analysis", "session", "scoring", "unblinding", "cli", "web",
                "catalog", "trials", "notification", "structured", "anchor"}) {
            assertThat(MODULES.getModuleByName(name))
                    .describedAs("modulo %s", name)
                    .isPresent();
        }
    }
}
