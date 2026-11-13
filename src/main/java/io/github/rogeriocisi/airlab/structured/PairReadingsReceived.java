package io.github.rogeriocisi.airlab.structured;

/** Domain event: the medium of a pair locked both readings. Pair code only. */
public record PairReadingsReceived(String pairCode) {
}
