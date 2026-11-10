/*
 * Verificador de alvos do Crivo.
 *
 * Reimplementa, no navegador de quem fez o teste, a regra que o servidor usou para derivar o
 * alvo de cada tentativa. Nenhuma chamada de rede: a semente revelada entra, os alvos saem.
 *
 * A regra, inteira:
 *
 *   bloco(i) = SHA-256( semente || "trial:"+n (UTF-8) || i como inteiro de 32 bits big-endian )
 *   fluxo    = bloco(0) || bloco(1) || ...
 *   inteiro  = primeiros 4 bytes do fluxo, big-endian, com o bit de sinal zerado
 *   alvo(n)  = inteiro % alternativas, reamostrando enquanto inteiro >= limite
 *   limite   = 2147483647 - (2147483647 % alternativas)
 *
 * A reamostragem existe para o sorteio não ficar viciado quando o intervalo não divide
 * exatamente o espaço do inteiro. Com 4 alternativas ela quase nunca dispara, mas faz parte
 * da regra e está aqui.
 *
 * Vetor de teste, o mesmo que trava a implementação em Java (TargetDerivationTest):
 *   semente de 64 zeros, 12 tentativas, 4 alternativas
 *   alvos, base 1: 1, 1, 3, 3, 2, 3, 2, 1, 1, 4, 1, 3
 */
(function () {
    'use strict';

    const MAX_INT = 2147483647;

    function hexParaBytes(hex) {
        const limpo = hex.trim().toLowerCase().replace(/\s+/g, '');
        if (!/^[0-9a-f]+$/.test(limpo) || limpo.length % 2 !== 0) {
            throw new Error('a semente precisa ser uma sequência hexadecimal');
        }
        const bytes = new Uint8Array(limpo.length / 2);
        for (let i = 0; i < bytes.length; i++) {
            bytes[i] = parseInt(limpo.substr(i * 2, 2), 16);
        }
        return bytes;
    }

    function concatena(a, b, c) {
        const fora = new Uint8Array(a.length + b.length + c.length);
        fora.set(a, 0);
        fora.set(b, a.length);
        fora.set(c, a.length + b.length);
        return fora;
    }

    function inteiro32BigEndian(valor) {
        const bytes = new Uint8Array(4);
        new DataView(bytes.buffer).setInt32(0, valor, false);
        return bytes;
    }

    /** Fluxo de bytes determinístico, idêntico ao support/SeedStream.java. */
    function criaFluxo(semente, dominio) {
        const dominioBytes = new TextEncoder().encode(dominio);
        let bloco = new Uint8Array(0);
        let posicao = 0;
        let contador = 0;

        async function proximoByte() {
            if (posicao >= bloco.length) {
                const entrada = concatena(semente, dominioBytes, inteiro32BigEndian(contador));
                contador += 1;
                const digest = await crypto.subtle.digest('SHA-256', entrada);
                bloco = new Uint8Array(digest);
                posicao = 0;
            }
            const b = bloco[posicao];
            posicao += 1;
            return b;
        }

        return {
            async proximoInteiroNaoNegativo() {
                const quatro = new Uint8Array(4);
                for (let i = 0; i < 4; i++) {
                    quatro[i] = await proximoByte();
                }
                return new DataView(quatro.buffer).getInt32(0, false) & 0x7fffffff;
            }
        };
    }

    function bytesParaHex(bytes) {
        return Array.from(bytes)
            .map(function (b) { return b.toString(16).padStart(2, '0'); })
            .join('');
    }

    /**
     * Refaz o compromisso publicado no início da sessão:
     *
     *   compromisso = SHA-256( semente | protocolo | tentativas | alternativas | banco )
     *
     * É isto que liga a semente revelada no fim àquela que estava valendo antes da primeira
     * resposta. Sem esta conferência, recomputar os alvos só mostra que a semente mostrada
     * gera os alvos mostrados — o que qualquer semente faria, se tivesse sido escolhida depois.
     */
    async function compromissoDe(sementeHex, caixa) {
        const partes = [
            sementeHex.trim().toLowerCase(),
            caixa.dataset.protocolo,
            caixa.dataset.tentativasDeclaradas,
            caixa.dataset.alternativas,
            caixa.dataset.bancoFiguras
        ];
        const entrada = new TextEncoder().encode(partes.join('|'));
        const digest = await crypto.subtle.digest('SHA-256', entrada);
        return bytesParaHex(new Uint8Array(digest));
    }

    async function alvoDaTentativa(semente, tentativa, alternativas) {
        const fluxo = criaFluxo(semente, 'trial:' + tentativa);
        const limite = MAX_INT - (MAX_INT % alternativas);
        for (;;) {
            const candidato = await fluxo.proximoInteiroNaoNegativo();
            if (candidato < limite) {
                return candidato % alternativas;
            }
        }
    }

    function linha(tentativa, alvo, gravado) {
        const tr = document.createElement('tr');
        const confere = gravado === null || gravado === alvo;

        [String(tentativa), String(alvo), gravado === null ? '—' : String(gravado)]
            .forEach(function (texto) {
                const td = document.createElement('td');
                td.textContent = texto;
                tr.appendChild(td);
            });

        const td = document.createElement('td');
        td.textContent = gravado === null ? '—' : (confere ? 'confere' : 'NÃO CONFERE');
        if (!confere) {
            td.className = 'nao-confere';
        }
        tr.appendChild(td);
        return tr;
    }

    async function verificar(caixa) {
        const semente = caixa.querySelector('[data-campo="semente"]').value;
        const tentativas = parseInt(caixa.querySelector('[data-campo="tentativas"]').value, 10);
        const alternativas = parseInt(caixa.dataset.alternativas, 10);
        const gravados = (caixa.dataset.alvos || '')
            .split(',')
            .map(function (v) { return v.trim(); })
            .filter(function (v) { return v.length > 0; })
            .map(Number);

        const saida = caixa.querySelector('[data-campo="saida"]');
        const corpo = caixa.querySelector('[data-campo="corpo"]');
        const resumo = caixa.querySelector('[data-campo="resumo"]');
        const compromissoLinhaInicial = caixa.querySelector('[data-campo="compromisso"]');
        corpo.replaceChildren();
        // Limpa o veredito anterior: deixar uma conferencia antiga na tela enquanto a nova
        // falha seria pior do que nao mostrar nada.
        compromissoLinhaInicial.textContent = '';
        compromissoLinhaInicial.classList.remove('falhou');

        if (!window.crypto || !window.crypto.subtle) {
            resumo.textContent = 'Este navegador não expõe SHA-256 nesta página. '
                + 'Abra por HTTPS ou por 127.0.0.1, ou rode a implementação de referência em Java.';
            saida.hidden = false;
            return;
        }

        let sementeBytes;
        try {
            sementeBytes = hexParaBytes(semente);
        } catch (erro) {
            resumo.textContent = 'Semente inválida: ' + erro.message + '.';
            saida.hidden = false;
            return;
        }

        if (!Number.isInteger(tentativas) || tentativas < 1 || tentativas > 200) {
            resumo.textContent = 'O número de tentativas precisa estar entre 1 e 200.';
            saida.hidden = false;
            return;
        }

        const compromissoEsperado = (caixa.dataset.compromisso || '').trim().toLowerCase();
        const compromissoLinha = caixa.querySelector('[data-campo="compromisso"]');
        const compromissoRefeito = await compromissoDe(semente, caixa);
        if (!compromissoEsperado) {
            compromissoLinha.textContent =
                'Compromisso recomputado: ' + compromissoRefeito + '.';
        } else if (compromissoRefeito === compromissoEsperado) {
            compromissoLinha.textContent =
                'A semente confere com o compromisso publicado no início da sessão. '
                + 'Se você anotou aquele valor antes de responder, está provado que esta '
                + 'semente já estava valendo naquele momento.';
            compromissoLinha.classList.remove('falhou');
        } else {
            compromissoLinha.textContent =
                'A semente NÃO confere com o compromisso publicado no início: recomputado '
                + compromissoRefeito + ', publicado ' + compromissoEsperado + '.';
            compromissoLinha.classList.add('falhou');
        }

        let divergencias = 0;
        for (let n = 1; n <= tentativas; n++) {
            const alvo = (await alvoDaTentativa(sementeBytes, n, alternativas)) + 1;
            const gravado = n <= gravados.length ? gravados[n - 1] : null;
            if (gravado !== null && gravado !== alvo) {
                divergencias += 1;
            }
            corpo.appendChild(linha(n, alvo, gravado));
        }

        if (gravados.length === 0) {
            resumo.textContent = 'Alvos recomputados nesta página, a partir da semente informada.';
        } else if (divergencias === 0) {
            resumo.textContent = 'Os ' + Math.min(tentativas, gravados.length)
                + ' alvo(s) recomputados aqui batem com os que a sessão gravou.';
        } else {
            resumo.textContent = divergencias + ' alvo(s) recomputados NÃO batem com os gravados. '
                + 'Isso é uma falha séria: por favor, abra uma issue no repositório com o código '
                + 'da sessão e a semente.';
        }
        saida.hidden = false;
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-verificador]').forEach(function (caixa) {
            const botao = caixa.querySelector('[data-campo="gerar"]');
            botao.addEventListener('click', function () {
                botao.disabled = true;
                verificar(caixa).catch(function (erro) {
                    caixa.querySelector('[data-campo="resumo"]').textContent =
                        'Falha ao recomputar: ' + erro.message;
                    caixa.querySelector('[data-campo="saida"]').hidden = false;
                }).finally(function () {
                    botao.disabled = false;
                });
            });
        });
    });
}());
