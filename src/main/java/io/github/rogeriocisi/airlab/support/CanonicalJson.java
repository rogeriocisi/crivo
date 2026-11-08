package io.github.rogeriocisi.airlab.support;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deterministic JSON writer: object keys sorted, no insignificant whitespace, no locale
 * dependent number formatting.
 *
 * <p>Hashes are only meaningful if everyone computes them over exactly the same bytes
 * (P2, P4, P5). A general purpose JSON library gives no such guarantee across versions,
 * so the canonical form is written here, in a few lines an auditor can read.
 */
public final class CanonicalJson {

    private CanonicalJson() {
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Map<?, ?> map) {
            writeObject(map, out);
        } else if (value instanceof Collection<?> collection) {
            writeArray(collection, out);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Number number) {
            writeNumber(number, out);
        } else {
            writeString(value.toString(), out);
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out) {
        Map<String, Object> sorted = new TreeMap<>();
        map.forEach((k, v) -> sorted.put(String.valueOf(k), v));
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(entry.getKey(), out);
            out.append(':');
            writeValue(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeArray(Collection<?> collection, StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeValue(item, out);
        }
        out.append(']');
    }

    private static void writeNumber(Number number, StringBuilder out) {
        double d = number.doubleValue();
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            out.append((long) d);
        } else {
            out.append(java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString());
        }
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
