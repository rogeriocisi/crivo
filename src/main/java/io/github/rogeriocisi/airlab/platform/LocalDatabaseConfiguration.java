package io.github.rogeriocisi.airlab.platform;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Brings up PostgreSQL inside the application process, from the real PostgreSQL binaries,
 * with no Docker and no manual installation (Constitution, architectural rule 8).
 *
 * <p>Active only when {@code airlab.local-database.enabled} is true, which the {@code local}
 * profile sets and the {@code prod} profile does not. In production the database is the one
 * installed on the operating system and this class does nothing.
 *
 * <p>The cluster lives under a directory outside the project, so stopping the application does
 * not lose the dry-run data, and deleting that directory is a clean reset.
 *
 * <p><b>One cluster per JVM, not per application context.</b> A PostgreSQL cluster is a process
 * level resource: it holds a port and a lock file in its data directory. The test suite loads
 * several Spring contexts in one JVM, and a second context starting its own instance on the
 * same port fails with {@code could not lock}. So the instance is shared and closed by a JVM
 * shutdown hook rather than by context shutdown, which is also what happens in production,
 * where the context and the JVM end together.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "airlab.local-database", name = "enabled", havingValue = "true")
class LocalDatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LocalDatabaseConfiguration.class);

    private static final Object MONITOR = new Object();
    private static EmbeddedPostgres shared;
    private static String sharedKey;

    @Bean
    EmbeddedPostgres embeddedPostgres(LocalDatabaseProperties properties) throws IOException {
        Path dataDirectory = Path.of(properties.dataDirectory()).toAbsolutePath().normalize();
        String key = properties.port() + "@" + dataDirectory;

        synchronized (MONITOR) {
            if (shared != null) {
                if (!key.equals(sharedKey)) {
                    throw new IllegalStateException(
                            "este processo ja subiu um PostgreSQL embarcado em " + sharedKey
                                    + " e nao pode subir outro em " + key
                                    + "; use o mesmo porto e diretorio, ou um processo separado");
                }
                log.debug("Reaproveitando o PostgreSQL embarcado de {}", sharedKey);
                return shared;
            }

            Files.createDirectories(dataDirectory.getParent() == null
                    ? dataDirectory
                    : dataDirectory.getParent());

            log.info("Subindo PostgreSQL embarcado na porta {} (dados em {})",
                    properties.port(), dataDirectory);

            EmbeddedPostgres instance = EmbeddedPostgres.builder()
                    .setPort(properties.port())
                    .setDataDirectory(dataDirectory.toFile())
                    .setCleanDataDirectory(false)
                    .start();

            shared = instance;
            sharedKey = key;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> close(instance), "airlab-pg-stop"));
            return instance;
        }
    }

    @Bean
    DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }

    private static void close(EmbeddedPostgres instance) {
        try {
            instance.close();
        } catch (IOException e) {
            // Nothing useful is left to do while the JVM is going down.
            log.warn("Falha ao encerrar o PostgreSQL embarcado: {}", e.getMessage());
        }
    }
}
