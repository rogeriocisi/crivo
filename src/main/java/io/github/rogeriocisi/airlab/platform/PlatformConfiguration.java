package io.github.rogeriocisi.airlab.platform;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    /**
     * Password hashing for the researcher and for the P4 accounts. Here and not in the web
     * security configuration because the accounts service needs it in every context, including
     * the command line one, which has no web server.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
