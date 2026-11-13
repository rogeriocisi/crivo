package io.github.rogeriocisi.airlab.participant;

/**
 * Result of a sign-up. The confirmation token is the only copy in clear: the database keeps its
 * SHA-256, and the token goes to the person by e-mail and nowhere else.
 *
 * @param code              pseudonymous code of the account
 * @param profile           the account profile; for an existing account, the one it already had
 * @param confirmationToken one-time token for the confirmation link, or {@code null} when the
 *                          account already existed and was confirmed
 * @param alreadyExisted    the e-mail already had an active account. The page shown to the
 *                          visitor is the same in both cases, so the form does not reveal who has
 *                          an account; the e-mail sent says what happened
 */
public record AccountRegistration(String code,
                                  AccountProfile profile,
                                  String confirmationToken,
                                  boolean alreadyExisted) {

    @Override
    public String toString() {
        return "AccountRegistration[code=" + code + ", profile=" + profile
                + ", alreadyExisted=" + alreadyExisted + "]";
    }
}
