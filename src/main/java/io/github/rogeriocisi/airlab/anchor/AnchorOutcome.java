package io.github.rogeriocisi.airlab.anchor;

import java.util.Optional;

/**
 * What one call of {@link AnchorService#anchorIfDue} did.
 *
 * @param kind    whether a stamp was due, granted or failed
 * @param anchor  the stamp, when granted
 * @param reason  why it failed, when it failed
 */
public record AnchorOutcome(Kind kind, Optional<AnchorView> anchor, Optional<FailureReason> reason) {

    public enum Kind {
        /** Nothing to stamp now: no milestone, interval not elapsed, or backing off after a failure. */
        NOT_DUE,
        GRANTED,
        FAILED
    }

    /** Why a request did not produce a stored stamp. Never carries any data from the trail. */
    public enum FailureReason {
        /** The authority could not be reached, or answered with an HTTP error. */
        UNREACHABLE,
        /** The authority answered and refused the request. */
        REJECTED,
        /** The answer did not check out: wrong hash, wrong nonce, bad signature or certificate. */
        INVALID_RESPONSE
    }

    public static AnchorOutcome notDue() {
        return new AnchorOutcome(Kind.NOT_DUE, Optional.empty(), Optional.empty());
    }

    public static AnchorOutcome granted(AnchorView anchor) {
        return new AnchorOutcome(Kind.GRANTED, Optional.of(anchor), Optional.empty());
    }

    public static AnchorOutcome failed(FailureReason reason) {
        return new AnchorOutcome(Kind.FAILED, Optional.empty(), Optional.of(reason));
    }
}
