package io.github.rogeriocisi.airlab.participant;

/** Domain event: a new linked-accounts flag awaits review. The flag code only. */
public record AccountsFlagged(String flagCode) {
}
