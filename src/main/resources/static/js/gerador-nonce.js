/*
 * Sorteia, no navegador do participante, a metade dele da semente — e guarda o compromisso
 * que o servidor publicou antes disso.
 *
 * As duas coisas são o que torna a sessão verificável sem depender da palavra da plataforma:
 *
 *   1. O nonce vem de crypto.getRandomValues, aqui, depois de a página já trazer o compromisso
 *      da metade do servidor. O servidor não tinha como prevê-lo quando se comprometeu.
 *   2. O compromisso fica em sessionStorage. No fim, o verificador compara o que está na tela
 *      com o que este navegador guardou lá atrás — a conferência que, de outro modo, dependeria
 *      de a pessoa ter copiado o hash à mão.
 */
(function () {
    'use strict';

    const BYTES_DO_NONCE = 16;

    function sorteia() {
        const bytes = new Uint8Array(BYTES_DO_NONCE);
        crypto.getRandomValues(bytes);
        return Array.from(bytes)
            .map(function (b) { return b.toString(16).padStart(2, '0'); })
            .join('');
    }

    function guardaCompromisso() {
        const sessao = document.querySelector('[data-sessao]');
        const compromisso = document.querySelector('[data-compromisso]');
        if (!sessao || !compromisso) {
            return;
        }
        try {
            sessionStorage.setItem(
                'crivo.compromisso.' + sessao.textContent.trim(),
                compromisso.textContent.trim());
        } catch (erro) {
            // Navegação privativa ou armazenamento bloqueado. A sessão continua válida: a
            // pessoa só perde a conferência automática no fim, e a página diz como fazer à mão.
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const form = document.querySelector('[data-gerador-nonce]');
        if (!form) {
            return;
        }
        const campo = form.querySelector('[data-campo="nonce"]');
        const botao = form.querySelector('[data-campo="sortear"]');

        if (!window.crypto || !window.crypto.getRandomValues) {
            campo.value = '';
            campo.placeholder = 'este navegador não oferece sorteio criptográfico';
            return;
        }

        guardaCompromisso();
        campo.value = sorteia();
        botao.addEventListener('click', function () {
            campo.value = sorteia();
        });
    });
}());
