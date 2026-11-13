/**
 * E-mail and notices to the administrator (SPEC E13).
 *
 * <p>Neither e-mail nor the administrator panel is a blind channel, so a notice carries only a
 * kind, a pseudonymous code and a time: never a name, an answer or a score. In the local
 * profile no e-mail leaves the machine; messages stay in an in-memory outbox that only the
 * administrator can open.
 *
 * <p>Dependency justified in {@code docs/PLAN.md}: Spring Boot Mail (Jakarta Mail), for the
 * account confirmation link and the notices, sent through an external SMTP server.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "notification",
        allowedDependencies = {"participant", "structured", "support"})
package io.github.rogeriocisi.airlab.notification;
