package io.github.rogeriocisi.airlab;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Names for test accounts. The first name is the one a test may look for ("Olá, Pessoa"); the rest
 * is random, so two test accounts never share a full name — which the linked-accounts signal of the
 * P4 (SPEC E13, phase E) would rightly flag, and which on the shared, persistent test database
 * would flag every account against every other.
 */
public final class TestNames {

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private TestNames() {
    }

    public static String unique(String firstName) {
        return firstName + " " + word() + " " + word();
    }

    private static String word() {
        StringBuilder out = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        out.append(Character.toUpperCase(LETTERS.charAt(random.nextInt(LETTERS.length()))));
        for (int i = 0; i < 7; i++) {
            out.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        return out.toString();
    }
}
