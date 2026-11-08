# paper/ — Artigo técnico do Crivo

| Arquivo | Conteúdo |
|---|---|
| `artigo-tecnico.pt-BR.md` | Artigo de desenho/arquitetura, em português (rascunho) |
| `referencias.bib` | Referências em BibTeX, as mesmas de `docs/REFERENCES.md` |

## Tipo de artigo

**Artigo de desenho (design paper):** apresenta os requisitos, o protocolo, a arquitetura, as garantias e os limites do Crivo. Enquanto não houver implementação, o texto **não faz nenhuma afirmação empírica nem de desempenho**.

## Gerar PDF ou HTML

```bash
cd paper
pandoc artigo-tecnico.pt-BR.md --citeproc -o artigo-tecnico.pt-BR.pdf    # requer LaTeX
pandoc artigo-tecnico.pt-BR.md --citeproc -s -o artigo-tecnico.pt-BR.html
```

## Possíveis destinos (avaliar quando o texto amadurecer)

- **Preprint em português:** SciELO Preprints ou OSF Preprints, para registrar a precedência do desenho.
- **Revistas da área (em inglês):** *Journal of Scientific Exploration* e *Explore*, onde está boa parte da literatura citada.
- **Artigo de software (em inglês):** *Journal of Open Source Software* (JOSS), depois que existir software funcional, com testes e documentação.

## Pendências principais

- [ ] Ler o texto integral das referências e conferir os números citados
- [ ] Volume e páginas de Sarraf et al. (2021) e o tamanho de efeito para o cálculo de poder
- [ ] Levantamento sistemático de ferramentas existentes (Seção 2.5) antes de afirmar qualquer lacuna
- [ ] Ancoragem externa periódica da trilha de auditoria (Seções 5.5 e 6)
- [ ] Citar a LGPD e as resoluções do CNS conferindo o texto vigente (Seção 7)
- [ ] Afiliação do autor e licença do software
- [ ] Abstract em inglês
- [ ] Discussão (Seção 9), depois do MVP
