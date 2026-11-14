package io.github.rogeriocisi.airlab.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The site runs under a context path in production (psique.tec.br/crivo), so no page may point
 * at an absolute path that ignores it: every link, form and asset goes through a Thymeleaf link
 * expression ({@code @{...}}), which adds the context path.
 */
class ContextRelativeLinksTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    /** A plain attribute, or a th: attribute built by string concatenation, starting at the root. */
    private static final Pattern ABSOLUTE = Pattern.compile(
            "(?<![:\\w-])(href|src|action)=\"/|th:(href|src|action)=\"'/");

    @Test
    void no_template_links_to_an_absolute_path() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".html")).toList()) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (ABSOLUTE.matcher(lines.get(i)).find()) {
                        offenders.add(TEMPLATES.relativize(file) + ":" + (i + 1));
                    }
                }
            }
        }
        assertThat(offenders).as("use th:href=\"@{/...}\" instead").isEmpty();
    }
}
