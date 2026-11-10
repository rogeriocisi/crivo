-- V16: entropia do participante nos testes de autoatendimento (E11, E12).
--
-- Antes, a semente era inteiramente do servidor: quem nao copiasse o compromisso durante a
-- sessao dependia da palavra da aplicacao de que aquele compromisso fora mesmo publicado no
-- inicio. Agora a semente tem duas metades:
--
--   semente = SHA-256( semente do servidor || nonce do participante )
--
-- O servidor publica o compromisso da metade dele ANTES de ver o nonce; o navegador do
-- participante sorteia o nonce DEPOIS de receber o compromisso. Nenhum dos dois lados escolhe
-- o resultado, e a prova deixa de depender de ordem atestada por quem opera a plataforma.

ALTER TABLE research.test_session
    ADD COLUMN participant_nonce VARCHAR(64),
    ADD COLUMN server_seed_hex   CHAR(64);

COMMENT ON COLUMN research.test_session.seed_commitment IS
  'SHA-256 da semente DO SERVIDOR mais os parametros da sessao, publicado antes de o nonce chegar.';
COMMENT ON COLUMN research.test_session.participant_nonce IS
  'Metade do participante, sorteada no navegador dele. Publica desde o inicio: nao e segredo, e o que ela precisa ser e imprevisivel para o servidor ate chegar.';
COMMENT ON COLUMN research.test_session.server_seed_hex IS
  'Metade do servidor, em claro. Nula ate o fechamento, como a semente efetiva.';
COMMENT ON COLUMN research.test_session.seed_hex IS
  'Semente efetiva: SHA-256(metade do servidor || nonce). Nula ate o fechamento.';

-- Sessoes anteriores a esta migracao tinham uma semente so, e ela era inteiramente do
-- servidor. Preencher antes de impor a regra, em vez de apagar o passado: os dados sao
-- exploratorios, mas continuam sendo dados, e a trilha de auditoria ja registrou o que houve.
UPDATE research.test_session
   SET server_seed_hex = seed_hex
 WHERE seed_revealed_at IS NOT NULL
   AND server_seed_hex IS NULL;

ALTER TABLE research.test_session
    ADD CONSTRAINT ck_test_session_server_seed_reveal
        CHECK ((server_seed_hex IS NULL) = (seed_revealed_at IS NULL));

-- Sessao sem nonce e, daqui em diante, sessao que nunca teve tentativa; as antigas ficam
-- reconhecíveis justamente por terem tentativas e nao terem nonce.
COMMENT ON CONSTRAINT ck_test_session_server_seed_reveal ON research.test_session IS
  'As duas metades sao reveladas juntas, no fechamento da sessao.';

-- Nenhuma tentativa existe antes de o participante contribuir com a parte dele. Imposto aqui,
-- e nao so no codigo, porque e esta ordem que sustenta a afirmacao do protocolo.
CREATE OR REPLACE FUNCTION research.require_participant_nonce() RETURNS TRIGGER AS $$
DECLARE
    nonce TEXT;
BEGIN
    SELECT participant_nonce INTO nonce
      FROM research.test_session
     WHERE code = NEW.session_code;

    IF nonce IS NULL THEN
        RAISE EXCEPTION
            'session % has no participant nonce: no trial exists before the participant contributes entropy',
            NEW.session_code
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER test_trial_requires_nonce
    BEFORE INSERT OR UPDATE ON research.test_trial
    FOR EACH ROW EXECUTE FUNCTION research.require_participant_nonce();
