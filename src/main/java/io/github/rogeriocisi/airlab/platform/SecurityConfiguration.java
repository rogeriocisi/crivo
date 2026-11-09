package io.github.rogeriocisi.airlab.platform;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Access rules.
 *
 * <p>Deliberately strict for now: everything but the public pages and the health endpoint
 * requires a researcher login. Two factor authentication for researchers and single use,
 * expiring links for sitters and raters belong to Epics E2 and E6 and are not here yet; the
 * public surface stays small until they are.
 *
 * <p>Only applies to the web application. The {@code airlab} CLI runs the same artifact with
 * no web server, where there is no HTTP surface to protect and no {@code HttpSecurity} to
 * configure; its access control is the operating system account that can read the database.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/sobre", "/referencias", "/login",
                                "/css/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                // Custom sign-in page: it has to explain what the account is for and where the
                // credential comes from, which the default page cannot.
                .formLogin(login -> login.loginPage("/login").permitAll())
                .logout(logout -> logout.permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Researcher account. In production the hash comes from the environment
     * ({@code AIRLAB_RESEARCHER_PASSWORD_HASH}); in the local profile a random password is
     * generated at start up and printed once, so no credential is ever committed.
     */
    @Bean
    UserDetailsService users(Environment environment, PasswordEncoder encoder) {
        String username = environment.getProperty("AIRLAB_RESEARCHER_USER", "pesquisador");
        String hash = environment.getProperty("AIRLAB_RESEARCHER_PASSWORD_HASH", "");
        if (hash.isBlank()) {
            if (!environment.matchesProfiles("local", "test")) {
                throw new IllegalStateException(
                        "AIRLAB_RESEARCHER_PASSWORD_HASH nao definida. Em producao a credencial "
                                + "do pesquisador vem do ambiente; a aplicacao nao inicia sem ela.");
            }
            byte[] random = new byte[18];
            new SecureRandom().nextBytes(random);
            String password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            hash = encoder.encode(password);
            log.warn("Perfil local: usuario \"{}\" com senha temporaria desta execucao: {}",
                    username, password);
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username).password(hash).roles("RESEARCHER").build());
    }
}
