package io.github.rogeriocisi.airlab.anchor;

/**
 * Result of re-checking one stored stamp, for {@code airlab verify}.
 *
 * @param code            the stamp
 * @param anchoredPosition position of the stamped event
 * @param tokenValid      signature by the pinned authority certificate, issued by the pinned root,
 *                        over exactly the stored hash
 * @param matchesTrail    the stored hash is the hash of the event at that position in the trail
 * @param detail          short, auditor facing explanation; never personal data
 */
public record AnchorCheck(String code, long anchoredPosition, boolean tokenValid, boolean matchesTrail,
                          String detail) {

    public boolean ok() {
        return tokenValid && matchesTrail;
    }
}
