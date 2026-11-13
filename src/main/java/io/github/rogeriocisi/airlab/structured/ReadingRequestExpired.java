package io.github.rogeriocisi.airlab.structured;

/** Domain event: a request to a medium ran past its deadline and the pair goes back to the draw. */
public record ReadingRequestExpired(String pairCode) {
}
