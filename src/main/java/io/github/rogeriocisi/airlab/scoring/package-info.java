/**
 * Avaliacao cega pelo sitter (Epico E6).
 *
 * <p>Nao implementado ainda. As tabelas existem (migracao V8). Regras que valem aqui:
 * acesso por link de uso unico e expiravel, sem conta permanente; a ordem de apresentacao
 * vem do sorteio; nenhuma resposta da API expoe identificador que permita inferir qual
 * leitura e a do proprio sitter (SPEC, secao 4, e aceite do E6).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "scoring",
        allowedDependencies = {"audit", "redaction", "support"})
package io.github.rogeriocisi.airlab.scoring;
