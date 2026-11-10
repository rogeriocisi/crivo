/**
 * The protocol catalog of the platform (SPEC, section 1.1).
 *
 * <p>No state and no dependency on any other module: the catalog is specification turned into
 * code, so the pages, the study creation and the verification package all read the same list.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "catalog",
        allowedDependencies = {})
package io.github.rogeriocisi.airlab.catalog;
