package io.github.rogeriocisi.airlab.participant;

/**
 * What a person fills in to create a P4 account (SPEC E13).
 *
 * <p>The password arrives in clear only to be hashed; it is never stored, logged or returned.
 *
 * @param fullName              name, stored encrypted
 * @param email                 e-mail, stored encrypted, compared only through its blind index
 * @param password              chosen password, at least
 *                              {@link AccountService#MINIMUM_PASSWORD_LENGTH} characters
 * @param profile               consulente or medium, fixed from here on
 * @param termVersion           version of the consent term the person accepted
 * @param sensitiveDataAccepted explicit acceptance of the sensitive data clause (LGPD)
 */
public record AccountSignUp(String fullName,
                            String email,
                            String password,
                            AccountProfile profile,
                            String termVersion,
                            boolean sensitiveDataAccepted) {

    @Override
    public String toString() {
        // Never print the personal data or the password, not even by accident in a log (P6).
        return "AccountSignUp[profile=" + profile + ", termVersion=" + termVersion + "]";
    }
}
