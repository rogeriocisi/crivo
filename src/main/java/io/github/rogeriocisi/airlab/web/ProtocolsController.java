package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.catalog.Protocol;
import io.github.rogeriocisi.airlab.catalog.ProtocolCatalog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * The tests section: the catalog and one page per protocol (SPEC, section 1.1).
 *
 * <p>The catalog is shown in two sections: the AIR protocols, with the automated one featured,
 * and the extra experiments, which are the ESP tests (SPEC 1.1).
 *
 * <p>Each protocol page prints what that protocol's result does not support, straight from the
 * catalog, because catalog rule 2 makes it mandatory text rather than a footnote.
 */
@Controller
class ProtocolsController {

    @GetMapping("/testes")
    String catalog(Model model) {
        model.addAttribute("air", ProtocolCatalog.air());
        model.addAttribute("extras", ProtocolCatalog.extras());
        return "testes";
    }

    @GetMapping("/testes/{slug}")
    String protocol(@PathVariable String slug, Model model) {
        Protocol protocol = ProtocolCatalog.bySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("protocolo", protocol);
        return "teste-detalhe";
    }
}
