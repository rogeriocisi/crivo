package io.github.rogeriocisi.airlab.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedded PostgreSQL settings, for the {@code local} and {@code test} profiles only.
 *
 * @param databaseName     database inside the cluster; empty means the default {@code postgres}.
 *                         A test that needs a database of its own — the P4 unblinding scenario,
 *                         which must start from no open study — names one here
 * @param recreateDatabase drop and recreate {@code databaseName} when the context starts, so the
 *                         scenario starts clean on every run. Never applies to {@code postgres}
 */
@ConfigurationProperties(prefix = "airlab.local-database")
public record LocalDatabaseProperties(boolean enabled, int port, String dataDirectory,
                                      String databaseName, boolean recreateDatabase) {
}
