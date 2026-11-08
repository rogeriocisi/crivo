/**
 * Desmascaramento controlado (Epico E7).
 *
 * <p>Nao implementado ainda. A tabela existe (migracao V9), com triggers que impedem
 * UPDATE e DELETE: a operacao e explicita, unica e irreversivel. So pode ocorrer depois que
 * todos os pacotes de avaliacao estiverem bloqueados, e e ela que chama
 * {@code AllocationService.revealSeed}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "unblinding",
        allowedDependencies = {"audit", "allocation", "scoring", "study", "support"})
package io.github.rogeriocisi.airlab.unblinding;
