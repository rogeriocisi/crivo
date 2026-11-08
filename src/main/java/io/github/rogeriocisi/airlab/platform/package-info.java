/**
 * Technical configuration: clock, crypto key, security and the embedded PostgreSQL used in
 * development. No domain rule lives here.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "platform")
package io.github.rogeriocisi.airlab.platform;
