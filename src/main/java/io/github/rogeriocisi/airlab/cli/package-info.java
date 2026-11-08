/**
 * Command line entry points of the {@code airlab} CLI (Epic E8).
 *
 * <p>Same artifact as the web application, started with
 * {@code java -jar airlab.jar verify --spring.main.web-application-type=none}, so the
 * verification an external auditor runs is the very code that produced the data. Runs on
 * Windows and Linux alike: see {@code scripts/airlab.sh} and {@code scripts/airlab.ps1}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "cli",
        allowedDependencies = {"audit", "allocation", "study", "support"})
package io.github.rogeriocisi.airlab.cli;
