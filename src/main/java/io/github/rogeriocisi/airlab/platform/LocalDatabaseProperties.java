package io.github.rogeriocisi.airlab.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The PostgreSQL the application starts by itself in development and dry-run.
 *
 * @param enabled       true in the {@code local} profile only; never in production
 * @param port          loopback port the embedded instance listens on
 * @param dataDirectory where the cluster lives, relative to the working directory, so the
 *                      same value works on Windows and on Linux
 */
@ConfigurationProperties(prefix = "airlab.local-database")
public record LocalDatabaseProperties(boolean enabled, int port, String dataDirectory) {
}
