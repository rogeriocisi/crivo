/**
 * Sessions, raw readings and incidents (Epico E4).
 *
 * <p>Nao implementado ainda. As tabelas existem (migracao V6) e o modulo consome o evento
 * {@code AllocationDrawn} para criar as sessoes. Regras de cegamento que valem aqui
 * (SPEC, secao 4): o experimentador ve apenas o primeiro nome do alvo, liberado pela unica
 * via auditada ({@code ParticipantService.releaseFirstName}); o arquivo de midia e gravado
 * com identificador aleatorio, sem nome original nem metadados.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "session",
        allowedDependencies = {"audit", "participant", "allocation", "support"})
package io.github.rogeriocisi.airlab.session;
