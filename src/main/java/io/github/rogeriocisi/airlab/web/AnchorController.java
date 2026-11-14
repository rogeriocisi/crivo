package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.anchor.AnchorService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The public list of external time stamps (SPEC E8, v0.15) and the {@code .tsr} of each one, so
 * that anyone can check it with {@code openssl ts -verify} without trusting this installation.
 * Positions, hashes and times only: the trail carries nothing personal (P6, P7).
 */
@Controller
class AnchorController {

    private static final MediaType TIMESTAMP_REPLY = MediaType.parseMediaType("application/timestamp-reply");

    private final AnchorService anchors;

    AnchorController(AnchorService anchors) {
        this.anchors = anchors;
    }

    @GetMapping("/carimbos")
    String list(Model model) {
        model.addAttribute("carimbos", anchors.granted());
        model.addAttribute("situacao", anchors.status());
        return "carimbos";
    }

    @GetMapping("/carimbos/{code}.tsr")
    ResponseEntity<byte[]> download(@PathVariable String code) {
        return anchors.response(code)
                .map(der -> ResponseEntity.ok()
                        .contentType(TIMESTAMP_REPLY)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment().filename(code + ".tsr").build().toString())
                        .body(der))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
