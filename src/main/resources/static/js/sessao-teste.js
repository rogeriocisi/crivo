/*
 * Ergonomia das sessões de autoatendimento (E11, E12) e da página de resultado.
 *
 * Tudo aqui é melhoria progressiva: sem JavaScript, cada página continua funcionando com os
 * formulários comuns. Nada deste arquivo fala com o servidor nem lê desfecho algum — durante a
 * sessão não existe desfecho na página para ler.
 */
(function () {
  "use strict";

  /* Envio único: impede o clique duplo e mostra que a resposta está sendo gravada.
   * Os botões não são desabilitados no próprio evento de envio, porque um botão desabilitado
   * nesse instante sai dos dados do formulário e a escolha se perderia. */
  function travarEnvio(form) {
    var enviando = false;
    form.addEventListener("submit", function (evento) {
      if (enviando) {
        evento.preventDefault();
        return;
      }
      enviando = true;
      form.classList.add("enviando");
      form.setAttribute("aria-busy", "true");
      var aviso = form.querySelector("[data-enviando]");
      if (aviso) {
        aviso.hidden = false;
      }
      window.setTimeout(function () {
        form.querySelectorAll("button").forEach(function (b) { b.disabled = true; });
      }, 0);
    });
  }

  document.querySelectorAll("form[data-envio-unico]").forEach(travarEnvio);

  /* P2: teclas 1 a N escolhem a porta correspondente. */
  var portas = document.querySelector("[data-portas]");
  if (portas) {
    document.addEventListener("keydown", function (evento) {
      if (evento.altKey || evento.ctrlKey || evento.metaKey || evento.repeat) {
        return;
      }
      var alvo = evento.target;
      if (alvo && (alvo.tagName === "INPUT" || alvo.tagName === "TEXTAREA" || alvo.isContentEditable)) {
        return;
      }
      var botao = portas.querySelector('button[value="' + evento.key + '"]');
      if (botao && !botao.disabled) {
        evento.preventDefault();
        botao.focus();
        botao.click();
      }
    });
  }

  /* P3: cada figura entra uma vez só. A figura já escolhida num lugar fica indisponível nos
   * demais, e quando sobra um lugar só, a figura que sobrou entra nele. */
  var ordenacao = document.querySelector("[data-ordenacao]");
  if (ordenacao) {
    var selects = Array.prototype.slice.call(ordenacao.querySelectorAll("select"));
    var atualizar = function () {
      var usados = selects.map(function (s) { return s.value; }).filter(Boolean);
      selects.forEach(function (s) {
        Array.prototype.forEach.call(s.options, function (opcao) {
          if (!opcao.value) {
            return;
          }
          opcao.disabled = opcao.value !== s.value && usados.indexOf(opcao.value) >= 0;
        });
      });
    };
    var completarUltimo = function () {
      var vazios = selects.filter(function (s) { return !s.value; });
      if (vazios.length !== 1) {
        return;
      }
      var usados = selects.map(function (s) { return s.value; });
      var sobra = Array.prototype.filter.call(vazios[0].options, function (o) {
        return o.value && usados.indexOf(o.value) < 0;
      });
      if (sobra.length === 1) {
        vazios[0].value = sobra[0].value;
      }
    };
    selects.forEach(function (s) {
      s.addEventListener("change", function () {
        completarUltimo();
        atualizar();
      });
    });
    atualizar();
  }

  /* Resultado: botão de copiar ao lado de cada valor da cadeia de verificação. */
  if (navigator.clipboard && window.isSecureContext) {
    document.querySelectorAll("[data-copiavel] dd > code").forEach(function (code) {
      var botao = document.createElement("button");
      botao.type = "button";
      botao.className = "copiar";
      botao.textContent = "copiar";
      botao.setAttribute("aria-label", "Copiar " + (code.parentElement.previousElementSibling
        ? code.parentElement.previousElementSibling.textContent.trim() : "valor"));
      botao.addEventListener("click", function () {
        navigator.clipboard.writeText(code.textContent.trim()).then(function () {
          botao.textContent = "copiado";
          window.setTimeout(function () { botao.textContent = "copiar"; }, 1600);
        }, function () {
          botao.textContent = "não copiou";
        });
      });
      code.insertAdjacentElement("afterend", botao);
    });
  }

  /* Resultado: baixar o comprovante da sessão encerrada em JSON (P7). O conteúdo é o mesmo
   * que a página já mostra, gerado pelo servidor; aqui ele só vira arquivo. */
  var dados = document.getElementById("comprovante-sessao");
  var baixar = document.querySelector("[data-baixar-comprovante]");
  if (dados && baixar) {
    baixar.hidden = false;
    baixar.addEventListener("click", function () {
      var texto = dados.textContent;
      var codigo = "sessao";
      try {
        codigo = JSON.parse(texto).sessao || codigo;
      } catch (e) { /* o arquivo sai mesmo assim */ }
      var blob = new Blob([texto + "\n"], { type: "application/json" });
      var url = URL.createObjectURL(blob);
      var link = document.createElement("a");
      link.href = url;
      link.download = "crivo-" + String(codigo).replace(/[^A-Za-z0-9_-]/g, "") + ".json";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    });
  }
})();
