package io.github.rogeriocisi.airlab.support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Deterministic byte stream expanded from a seed with SHA-256 in counter mode.
 *
 * <p>Why not {@code SecureRandom} here: the seed itself is drawn with {@code SecureRandom}
 * (P5), but the expansion from seed to draw must be <em>reproducible by a third party</em>,
 * on another machine, another JDK and another operating system. No {@code SecureRandom}
 * algorithm guarantees that. SHA-256 in counter mode does, and an auditor can reimplement
 * it in a dozen lines of any language:
 *
 * <pre>
 * block(i) = SHA-256(seedBytes || domain(UTF-8) || bigEndianInt32(i))
 * stream   = block(0) || block(1) || block(2) ...
 * </pre>
 *
 * <p>{@code domain} separates independent draws made from the same seed, so that the order
 * in which the application happens to ask for numbers cannot change an earlier result.
 */
public final class SeedStream {

    private final byte[] seed;
    private final byte[] domain;
    private byte[] block = new byte[0];
    private int blockOffset;
    private int counter;

    public SeedStream(byte[] seed, String domain) {
        this.seed = seed.clone();
        this.domain = domain.getBytes(StandardCharsets.UTF_8);
    }

    /** Uniform integer in {@code [0, bound)}, rejection sampled to stay unbiased. */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int limit = Integer.MAX_VALUE - (Integer.MAX_VALUE % bound);
        while (true) {
            int candidate = nextNonNegativeInt();
            if (candidate < limit) {
                return candidate % bound;
            }
        }
    }

    public boolean nextBoolean() {
        return (nextByte() & 1) == 1;
    }

    /** Fisher-Yates shuffle driven by this stream, so the permutation is reproducible. */
    public <T> void shuffle(java.util.List<T> items) {
        for (int i = items.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T tmp = items.get(i);
            items.set(i, items.get(j));
            items.set(j, tmp);
        }
    }

    private int nextNonNegativeInt() {
        byte[] four = new byte[4];
        for (int i = 0; i < 4; i++) {
            four[i] = nextByte();
        }
        return ByteBuffer.wrap(four).getInt() & 0x7fffffff;
    }

    private byte nextByte() {
        if (blockOffset >= block.length) {
            block = nextBlock();
            blockOffset = 0;
        }
        return block[blockOffset++];
    }

    private byte[] nextBlock() {
        ByteBuffer input = ByteBuffer.allocate(seed.length + domain.length + 4);
        input.put(seed).put(domain).putInt(counter++);
        return Hashes.sha256(input.array());
    }
}
