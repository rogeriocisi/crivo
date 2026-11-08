package io.github.rogeriocisi.airlab.participant;

/** A formed pair, in codes only. Each target is the decoy of the other. */
public record PairView(String pairCode, String targetA, String targetB) {
}
