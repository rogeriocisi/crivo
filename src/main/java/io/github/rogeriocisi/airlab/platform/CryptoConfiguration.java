package io.github.rogeriocisi.airlab.platform;

import io.github.rogeriocisi.airlab.support.FieldCipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The key for data at rest: identifiable fields of the {@code identity} schema and the
 * escrowed draw seed (P5, P6).
 *
 * <p>In production the key comes from the environment and its absence stops the start up:
 * running with a default key would be worse than not running. In the {@code local} profile,
 * where the data is fictitious by definition, a key is generated once and kept in
 * {@code ./data/dev-identity.key}, which is not versioned.
 */
@Configuration(proxyBeanMethods = false)
class CryptoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CryptoConfiguration.class);
    private static final Path DEV_KEY_FILE = Path.of("./data/dev-identity.key");

    @Bean
    FieldCipher fieldCipher(Environment environment) throws IOException {
        String configured = environment.getProperty("airlab.identity.encryption-key", "");
        if (!configured.isBlank()) {
            return new FieldCipher(Base64.getDecoder().decode(configured.trim()));
        }
        boolean local = environment.matchesProfiles("local", "test");
        if (!local) {
            throw new IllegalStateException(
                    "AIRLAB_IDENTITY_KEY nao definida. Em producao a chave de criptografia dos "
                            + "dados identificaveis vem do ambiente; a aplicacao nao inicia sem ela.");
        }
        return new FieldCipher(developmentKey());
    }

    private byte[] developmentKey() throws IOException {
        if (Files.exists(DEV_KEY_FILE)) {
            return Base64.getDecoder().decode(Files.readString(DEV_KEY_FILE, StandardCharsets.UTF_8).trim());
        }
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        Files.createDirectories(DEV_KEY_FILE.toAbsolutePath().getParent());
        Files.writeString(DEV_KEY_FILE, Base64.getEncoder().encodeToString(key), StandardCharsets.UTF_8);
        log.warn("Chave de desenvolvimento gerada em {}. Vale somente para dados ficticios.",
                DEV_KEY_FILE.toAbsolutePath().normalize());
        return key;
    }
}
