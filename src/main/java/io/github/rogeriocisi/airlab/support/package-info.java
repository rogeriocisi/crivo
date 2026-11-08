/**
 * Shared primitives with no domain knowledge: hashing, canonical JSON, pseudonymous codes
 * and the deterministic seed stream.
 *
 * <p>Declared as an open module so that domain modules may use it freely. Nothing here may
 * depend on a domain module, and nothing here may touch personal data.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "support")
package io.github.rogeriocisi.airlab.support;
