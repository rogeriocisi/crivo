package io.github.rogeriocisi.airlab.platform;

import io.github.rogeriocisi.airlab.participant.AccountCredentials;
import io.github.rogeriocisi.airlab.participant.AccountService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;

/**
 * Access rules.
 *
 * <p>Three kinds of signed-in person (SPEC E13):
 * <ul>
 *   <li><strong>administrator</strong> — the researcher, whose credential comes from the server
 *       configuration. Roles {@code ADMIN} and {@code RESEARCHER}. No path of the site creates
 *       one;</li>
 *   <li><strong>consulente</strong> and <strong>médium</strong> — P4 accounts with a fixed
 *       profile, signed in by e-mail. Roles {@code SITTER} and {@code MEDIUM}, never both.</li>
 * </ul>
 * Each profile reaches only its own panel; the rule is here, on the server, and not in a menu.
 *
 * <p>Single use, expiring links for the P1 raters belong to Epic E6 and are not here yet.
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
        ExceptionMappingAuthenticationFailureHandler failures = new ExceptionMappingAuthenticationFailureHandler();
        failures.setDefaultFailureUrl("/login?error");
        // An account that never confirmed its e-mail gets its own message, but only after the
        // right password: the check is the post-authentication one (credentials expired), so a
        // wrong guess says nothing about whether the address is registered.
        failures.setExceptionMappings(Map.of(CredentialsExpiredException.class.getName(), "/login?naoConfirmada"));

        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/sobre", "/historia", "/referencias",
                                "/consulente", "/login", "/css/**", "/js/**", "/targets/**",
                                "/testes", "/testes/**", "/error", "/actuator/health",
                                "/conta/criar", "/conta/verifique-seu-email", "/conta/confirmar",
                                "/estudos", "/estudos/**")
                        .permitAll()
                        .requestMatchers("/painel/adm", "/painel/adm/**", "/caixa-de-saida").hasRole("ADMIN")
                        .requestMatchers("/painel/consulente", "/painel/consulente/**").hasRole("SITTER")
                        .requestMatchers("/painel/medium", "/painel/medium/**").hasRole("MEDIUM")
                        .requestMatchers("/conta", "/conta/**").hasAnyRole("SITTER", "MEDIUM")
                        .anyRequest().authenticated())
                // Custom sign-in page: it has to explain who signs in and with what, which the
                // default page cannot.
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/painel")
                        .failureHandler(failures)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")));
        return http.build();
    }

    /**
     * The researcher first, by user name; otherwise a P4 account, by e-mail.
     *
     * <p>For an account the principal is named after the pseudonymous code, not the e-mail, so
     * the address never sits in the session or in anything that logs the principal (P6).
     *
     * <p>Researcher account: in production the hash comes from the environment
     * ({@code AIRLAB_RESEARCHER_PASSWORD_HASH}); in the local profile a random password is
     * generated at start up and printed once, so no credential is ever committed.
     */
    @Bean
    UserDetailsService users(Environment environment, PasswordEncoder encoder, AccountService accounts) {
        String researcherName = environment.getProperty("AIRLAB_RESEARCHER_USER", "pesquisador");
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
                    researcherName, password);
        }
        UserDetails researcher = User.withUsername(researcherName).password(hash)
                .roles("ADMIN", "RESEARCHER").build();

        return username -> {
            if (researcherName.equals(username)) {
                // A fresh copy each time: the framework erases the credentials of what it gets.
                return User.withUserDetails(researcher).build();
            }
            AccountCredentials account = accounts.credentials(username)
                    .orElseThrow(() -> new UsernameNotFoundException("unknown account"));
            return User.withUsername(account.code())
                    .password(account.passwordHash())
                    .roles(account.profile().name())
                    // Checked after the password, unlike "disabled", which is checked before it.
                    .credentialsExpired(!account.confirmed())
                    .build();
        };
    }
}
