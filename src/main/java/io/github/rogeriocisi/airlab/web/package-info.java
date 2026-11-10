/**
 * Server rendered web layer (Thymeleaf + HTMX).
 *
 * <p>Views render projections built for one role; a field forbidden to that role is absent
 * from the projection, never filtered in the template (architectural rule 3).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "web",
        allowedDependencies = {"audit", "study", "participant", "allocation", "analysis",
                "catalog", "trials", "support"})
package io.github.rogeriocisi.airlab.web;
