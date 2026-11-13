package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.structured.CycleReport;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the P4 cycle on the server (SPEC E13, phase C): every minute by default
 * ({@code airlab.p4.cycle-ms}). Only in the web application — the {@code airlab} CLI runs the same
 * artifact and must not start pairing — and only when {@code airlab.p4.cycle-enabled} is on; the
 * tests turn it off and call the cycle themselves, with the clock they need.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "airlab.p4.cycle-enabled", havingValue = "true", matchIfMissing = true)
class StructuredCycleScheduling {

    private static final Logger log = LoggerFactory.getLogger(StructuredCycleScheduling.class);

    private final StructuredStudyService service;
    private final Clock clock;

    StructuredCycleScheduling(StructuredStudyService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "${airlab.p4.cycle-initial-ms:15000}", fixedDelayString = "${airlab.p4.cycle-ms:60000}")
    void run() {
        try {
            CycleReport report = service.runCycle(clock.instant());
            if (!report.idle()) {
                // Counts only: nothing here links a pair to a medium (P4 matrix).
                log.info("Ciclo do P4: {}", report);
            }
        } catch (RuntimeException e) {
            // The next cycle tries again; the message carries codes at most, never personal data.
            log.warn("Ciclo do P4 falhou e sera repetido: {}", e.getMessage());
        }
    }
}
