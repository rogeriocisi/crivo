package io.github.rogeriocisi.airlab.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tells the site header which section the page belongs to, so the main navigation can mark
 * it with {@code aria-current="page"}.
 *
 * <p>Only the top level section is exposed, never the full path: a test page under
 * {@code /testes/...} marks "Testes", and nothing about the session leaks into the model.
 */
@ControllerAdvice
class NavigationAdvice {

    @ModelAttribute("secaoAtual")
    String currentSection(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (path.isEmpty() || path.equals("/")) {
            return "inicio";
        }
        String first = path.substring(1);
        int slash = first.indexOf('/');
        return slash < 0 ? first : first.substring(0, slash);
    }
}
