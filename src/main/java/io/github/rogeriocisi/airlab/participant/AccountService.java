package io.github.rogeriocisi.airlab.participant;

import java.util.List;
import java.util.Optional;

/**
 * Accounts of the automated AIR protocol, P4 (SPEC E13, phase A).
 *
 * <p>Lives in {@code participant} because an account is identifiable data, and this is the only
 * module allowed to read the {@code identity} schema (architectural rule 2). Everything that
 * leaves this interface is pseudonymous, except {@link #account}, which is built for the owner
 * of the account alone.
 */
public interface AccountService {

    /** Version of the account consent term currently in force (E10, lock 6). */
    String CURRENT_TERM_VERSION = "conta-v1";

    int MINIMUM_PASSWORD_LENGTH = 10;

    /** Hours a confirmation link stays valid. */
    int CONFIRMATION_HOURS = 48;

    /**
     * Creates an account with a fixed profile, pending e-mail confirmation.
     *
     * <p>When the e-mail already has an active account, nothing is created and the result says
     * so, carrying a fresh confirmation token if that account was never confirmed. The caller
     * shows the same page either way, so the form does not reveal who has an account.
     *
     * @throws IllegalArgumentException when a field is invalid, with a message in Portuguese
     *                                  meant for the person filling in the form
     */
    AccountRegistration register(AccountSignUp signUp);

    /** Confirms the e-mail behind a one-time token, and publishes {@link AccountConfirmed}. */
    EmailConfirmationResult confirmEmail(String token);

    /** Credentials for the sign-in, looked up by e-mail through its blind index. */
    Optional<AccountCredentials> credentials(String email);

    /** The account as its owner sees it. */
    Optional<AccountView> account(String code);

    /** Accounts by profile and situation, for the administrator. */
    AccountCounts counts();

    /**
     * Codes of the confirmed, active medium accounts, in code order: the pool the P4 draw picks
     * from (SPEC E13, phase C). Codes only.
     */
    List<String> activeMediumCodes();

    /**
     * The e-mail address of an active, confirmed account, for the mailer and nothing else: the
     * medium is told by e-mail that a request arrived (SPEC E13). Never logged, never shown.
     */
    Optional<String> mailAddress(String code);

    /**
     * Closes the account at the owner's request, without justification (P6): name, e-mail and
     * password hash are erased, and the pseudonymous code stays, marked as closed.
     */
    void close(String code);
}
