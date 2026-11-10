package io.github.rogeriocisi.airlab.trials;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Everything about a session, available only once it is over (E11).
 *
 * <p>This is also the unit of the verification package: with {@code seedHex} and the published
 * derivation rule, a third party recomputes every target and checks the hits, without taking
 * the platform's word for anything.
 *
 * @param chanceNumerator   declared chance level of the protocol, not estimated from the data
 * @param pValue            exact one sided binomial tail against that chance level
 * @param exploratoryReason why this session is exploratory data; never empty for public
 *                          self-service sessions
 */
public record SessionOutcome(String sessionCode,
                             TrialProtocol protocol,
                             SessionState state,
                             int declaredTrials,
                             int lockedTrials,
                             int hits,
                             int chanceNumerator,
                             int chanceDenominator,
                             BigDecimal pValue,
                             String seedCommitment,
                             String seedHex,
                             String targetPoolVersion,
                             Instant startedAt,
                             Instant closedAt,
                             List<TrialOutcome> trials,
                             String exploratoryReason) {

    public SessionOutcome {
        trials = List.copyOf(trials);
    }

    /** Hits expected by chance alone, for display next to the observed count. */
    public BigDecimal expectedByChance() {
        return BigDecimal.valueOf((long) lockedTrials * chanceNumerator)
                .divide(BigDecimal.valueOf(chanceDenominator), 2, java.math.RoundingMode.HALF_EVEN);
    }
}
