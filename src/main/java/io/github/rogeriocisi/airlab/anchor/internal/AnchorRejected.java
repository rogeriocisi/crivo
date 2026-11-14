package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.anchor.AnchorOutcome.FailureReason;

/**
 * A request that did not produce a stamp worth storing. The message is for the log and the CLI and
 * never carries data from the trail beyond a hash.
 */
class AnchorRejected extends Exception {

    private final FailureReason reason;

    AnchorRejected(FailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    AnchorRejected(FailureReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    FailureReason reason() {
        return reason;
    }
}
