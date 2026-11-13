package io.github.rogeriocisi.airlab.participant;

/**
 * Accounts by profile and situation, for the administrator panel. Counts only: no code, no
 * name, nothing that ties a person to a request.
 */
public record AccountCounts(long sittersConfirmed,
                            long sittersPending,
                            long mediumsConfirmed,
                            long mediumsPending) {
}
