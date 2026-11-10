package io.github.rogeriocisi.airlab.trials;

import java.util.List;

/**
 * The versioned pool of figures used as targets by the remote viewing protocol (E12).
 *
 * <p>Versioned, and the version goes into the session row: changing the figures later does not
 * change what was analysed. The pool lives in the repository rather than in the database so
 * that the exported verification package and the running application cannot disagree about
 * what figure index 7 was.
 *
 * <p>The figures are drawings made for this project, each one a different subject and a
 * different dominant shape, so that an impression about one of them is not generic enough to
 * fit the other three by accident. They carry no words: a label on the image would turn the
 * task into word matching.
 */
public final class TargetPool {

    /** Bump this when a figure changes, is added or is removed. Never edit in place. */
    public static final String VERSION = "v1";

    private static final List<Figure> FIGURES = List.of(
            new Figure("farol", "Farol sobre rochas, com feixe de luz"),
            new Figure("ponte", "Ponte suspensa sobre agua"),
            new Figure("piramide", "Piramide no deserto, com sol alto"),
            new Figure("cachoeira", "Cachoeira entre paredes de pedra"),
            new Figure("espiral", "Escada em espiral vista de cima"),
            new Figure("floresta", "Arvore solitaria num campo aberto"),
            new Figure("cidade", "Torres de uma cidade ao anoitecer"),
            new Figure("barco", "Barco a vela em mar aberto"),
            new Figure("relogio", "Relogio de bolso aberto"),
            new Figure("labirinto", "Labirinto de muros baixos"),
            new Figure("fogueira", "Fogueira acesa na noite"),
            new Figure("montanha", "Montanha com cume coberto de neve"));

    private TargetPool() {
    }

    public static int size() {
        return FIGURES.size();
    }

    public static Figure figure(int index) {
        if (index < 0 || index >= FIGURES.size()) {
            throw new IllegalArgumentException("figure index out of the pool");
        }
        return FIGURES.get(index);
    }

    public static List<Figure> figures() {
        return FIGURES;
    }

    /**
     * One figure of the pool.
     *
     * @param slug  file name under {@code /targets/<version>/}, and the identifier exported
     * @param label description for assistive technology; never shown next to the image during
     *              a trial, because reading the label would replace looking at the figure
     */
    public record Figure(String slug, String label) {

        public String imagePath() {
            return "/targets/" + VERSION + "/" + slug + ".svg";
        }
    }
}
