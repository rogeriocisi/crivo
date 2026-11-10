-- V15: testes de autoatendimento, escolha forcada precognitiva e visao remota (Epicos E11 e E12).
--
-- Dados anonimos por desenho: nenhuma coluna identificavel, nenhum vinculo com o esquema
-- identity. Sao dados exploratorios e abertos (P7), e e por isso que podem ser publicos.

CREATE TABLE research.test_session (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(32)  NOT NULL UNIQUE,
    protocol            VARCHAR(48)  NOT NULL,
    declared_trials     INTEGER      NOT NULL,
    alternatives        INTEGER      NOT NULL,
    seed_commitment     CHAR(64)     NOT NULL,
    seed_escrow         TEXT         NOT NULL,
    seed_hex            CHAR(64),
    seed_revealed_at    TIMESTAMPTZ,
    target_pool_version VARCHAR(16)  NOT NULL,
    state               VARCHAR(16)  NOT NULL,
    started_at          TIMESTAMPTZ  NOT NULL,
    closed_at           TIMESTAMPTZ,
    CONSTRAINT ck_test_session_trials CHECK (declared_trials BETWEEN 1 AND 200),
    CONSTRAINT ck_test_session_alternatives CHECK (alternatives BETWEEN 2 AND 16),
    CONSTRAINT ck_test_session_seed_reveal CHECK ((seed_hex IS NULL) = (seed_revealed_at IS NULL))
);

COMMENT ON TABLE research.test_session IS
  'Sessao de teste de autoatendimento. Anonima: nao ha participante associado.';
COMMENT ON COLUMN research.test_session.declared_trials IS
  'Numero de tentativas fixado na abertura. Nunca muda (E11).';
COMMENT ON COLUMN research.test_session.seed_commitment IS
  'SHA-256 da semente, publicado antes da primeira tentativa.';
COMMENT ON COLUMN research.test_session.seed_escrow IS
  'Semente cifrada em custodia. Lida uma unica vez, no fechamento da sessao.';
COMMENT ON COLUMN research.test_session.seed_hex IS
  'Semente em claro. Nula enquanto a sessao esta aberta: antes do fechamento ninguem a le.';

CREATE TABLE research.test_trial (
    id                 BIGSERIAL PRIMARY KEY,
    session_code       VARCHAR(32)  NOT NULL REFERENCES research.test_session (code),
    trial_index        INTEGER      NOT NULL,
    candidate_slugs    TEXT,
    impression         TEXT,
    impression_sha256  CHAR(64),
    impression_locked_at TIMESTAMPTZ,
    chosen_position    INTEGER,
    ranked_positions   TEXT,
    target_position    INTEGER,
    hit                BOOLEAN,
    locked_at          TIMESTAMPTZ,
    CONSTRAINT uk_test_trial UNIQUE (session_code, trial_index),
    CONSTRAINT ck_test_trial_index CHECK (trial_index >= 1),
    -- O alvo e o desfecho so existem depois da resposta travada, nunca antes.
    CONSTRAINT ck_test_trial_target_after_lock
        CHECK ((locked_at IS NULL) = (target_position IS NULL)
               AND (locked_at IS NULL) = (hit IS NULL))
);

COMMENT ON COLUMN research.test_trial.impression_sha256 IS
  'Hash da impressao como travada, antes de as candidatas aparecerem (E12).';
COMMENT ON COLUMN research.test_trial.target_position IS
  'Posicao de apresentacao do alvo. Gravada no travamento e recomputavel da semente revelada.';

-- Uma tentativa travada nao muda. A resposta e o alvo ficam como estao, e a impressao
-- travada tambem: e ela que sustenta a afirmacao de que nao foi reescrita depois.
CREATE OR REPLACE FUNCTION research.reject_locked_trial_change() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        RAISE EXCEPTION 'locked trial % of session % is immutable',
            OLD.trial_index, OLD.session_code
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    IF OLD.impression_locked_at IS NOT NULL
       AND (NEW.impression IS DISTINCT FROM OLD.impression
            OR NEW.impression_sha256 IS DISTINCT FROM OLD.impression_sha256) THEN
        RAISE EXCEPTION 'locked impression of trial % cannot be rewritten', OLD.trial_index
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER test_trial_locked_immutable
    BEFORE UPDATE ON research.test_trial
    FOR EACH ROW EXECUTE FUNCTION research.reject_locked_trial_change();

CREATE TRIGGER test_trial_no_delete
    BEFORE DELETE ON research.test_trial
    FOR EACH ROW EXECUTE FUNCTION research.reject_locked_trial_change();

CREATE INDEX idx_test_trial_session ON research.test_trial (session_code);
CREATE INDEX idx_test_session_state ON research.test_session (state);

GRANT SELECT, INSERT, UPDATE ON research.test_session TO airlab_app;
GRANT SELECT, INSERT, UPDATE ON research.test_trial TO airlab_app;
