package io.github.rogeriocisi.airlab.participant;

/**
 * What the sign-in needs about an account, and nothing more: no name, no e-mail.
 *
 * @param code         pseudonymous code; it becomes the name of the signed-in principal, so the
 *                     e-mail never sits in the session
 * @param passwordHash bcrypt hash of the password
 * @param profile      fixed profile
 * @param confirmed    whether the e-mail was confirmed; an unconfirmed account cannot sign in
 */
public record AccountCredentials(String code,
                                 String passwordHash,
                                 AccountProfile profile,
                                 boolean confirmed) {

    @Override
    public String toString() {
        return "AccountCredentials[code=" + code + ", profile=" + profile
                + ", confirmed=" + confirmed + "]";
    }
}
