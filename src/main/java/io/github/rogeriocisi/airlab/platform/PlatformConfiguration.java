package io.github.rogeriocisi.airlab.platform;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross cutting beans. The clock is injected everywhere instead of being read statically,
 * so that time dependent rules (freeze, lock, unblinding) are testable without waiting.
 */
@Configuration(proxyBeanMethods = false)
class PlatformConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
