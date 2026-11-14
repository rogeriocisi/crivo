package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.anchor.AnchorOutcome;
import io.github.rogeriocisi.airlab.anchor.AnchorService;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Checks every minute whether the head of the trail is due a stamp ({@code airlab.timestamp.check-ms}).
 * Only in the web application — the {@code airlab} CLI must not send requests — and only when
 * {@code airlab.timestamp.enabled} is on; the tests turn it off and call the service with their clock.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "airlab.timestamp.enabled", havingValue = "true", matchIfMissing = true)
class AnchorScheduling {

    private static final Logger log = LoggerFactory.getLogger(AnchorScheduling.class);

    private final AnchorService service;
    private final Clock clock;

    AnchorScheduling(AnchorService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "${airlab.timestamp.check-initial-ms:30000}",
            fixedDelayString = "${airlab.timestamp.check-ms:60000}")
    void run() {
        try {
            AnchorOutcome outcome = service.anchorIfDue(clock.instant());
            if (outcome.kind() == AnchorOutcome.Kind.GRANTED) {
                log.info("Carimbo de tempo concedido: {} na posicao {}", outcome.anchor().orElseThrow().code(),
                        outcome.anchor().orElseThrow().anchoredPosition());
            } else if (outcome.kind() == AnchorOutcome.Kind.FAILED) {
                log.warn("Carimbo de tempo falhou ({}); nova tentativa em seguida ao intervalo de espera",
                        outcome.reason().orElseThrow());
            }
        } catch (RuntimeException e) {
            log.warn("Verificacao do carimbo de tempo falhou e sera repetida: {}", e.getClass().getSimpleName());
        }
    }
}
