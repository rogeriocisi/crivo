-- V14: indice cego do contato (Epico E10, trava 2).
--
-- Com inscricao aberta pela internet, o conluio e o risco de fraude mais serio: a mesma pessoa
-- se inscrevendo como mediun e como consulente no mesmo estudo destroi o resultado sem deixar
-- rastro obvio. Para recusar isso, o sistema precisa reconhecer que duas inscricoes sao da
-- mesma pessoa -- sem guardar o contato em claro.
--
-- O campo abaixo e um HMAC-SHA256 do contato normalizado, sob chave secreta da aplicacao.
-- Nao e reversivel sem a chave, nao serve para nada fora desta instalacao e nunca entra no
-- pacote de verificacao (P6, P7).

ALTER TABLE identity.participant ADD COLUMN contact_fingerprint CHAR(64);

COMMENT ON COLUMN identity.participant.contact_fingerprint IS
  'HMAC-SHA256 do contato normalizado, sob chave da aplicacao. Serve so para detectar papel duplo no mesmo estudo. Nulo quando a inscricao nao informou contato.';

CREATE INDEX idx_participant_fingerprint ON identity.participant (contact_fingerprint);
