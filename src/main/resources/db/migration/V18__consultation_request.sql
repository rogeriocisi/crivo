-- V18: pedido de consulta do P4 (SPEC 0.10, E13, fase B).
--
-- O consulente informa o primeiro nome do falecido e responde ao questionario fechado. O pedido
-- chega ao banco uma vez, ja travado: o rascunho fica so na sessao do navegador.
--
-- Duas tabelas, pela mesma razao do P1 (V4):
--   identity.consultation_detail   primeiro nome, respostas e sal, cifrados pela aplicacao;
--   research.consultation_request  codigo, consulente, versao do questionario, os tres atributos
--                                  de pareamento e o compromisso salgado publicado na trilha.
--
-- O que o banco garante, alem do codigo:
--   1. pedido travado nao muda: so o estado avanca, e "retirado" e final;
--   2. nenhum pedido e apagado; o detalhe so admite o apagamento dos dados, na desistencia;
--   3. um pedido ativo por consulente.

CREATE TABLE research.consultation_request (
    id                     BIGSERIAL    PRIMARY KEY,
    code                   VARCHAR(32)  NOT NULL UNIQUE,
    sitter_code            VARCHAR(32)  NOT NULL,
    questionnaire_version  VARCHAR(32)  NOT NULL,
    gender                 VARCHAR(32)  NOT NULL,
    age_band               VARCHAR(32)  NOT NULL,
    death_period           VARCHAR(32)  NOT NULL,
    commitment             CHAR(64)     NOT NULL,
    state                  VARCHAR(24)  NOT NULL,
    locked_at              TIMESTAMPTZ  NOT NULL,
    withdrawn_at           TIMESTAMPTZ,
    CONSTRAINT ck_consultation_state CHECK (state IN
        ('LOCKED', 'PAIRED', 'SENT_TO_MEDIUM', 'READING_RECEIVED', 'UNBLINDED', 'WITHDRAWN')),
    CONSTRAINT ck_consultation_withdrawn CHECK ((state = 'WITHDRAWN') = (withdrawn_at IS NOT NULL)),
    CONSTRAINT ck_consultation_commitment CHECK (commitment ~ '^[0-9a-f]{64}$')
);

COMMENT ON TABLE research.consultation_request IS
  'Pedido de consulta do P4. Pseudonimo: o primeiro nome e as respostas ficam cifrados em identity.consultation_detail.';
COMMENT ON COLUMN research.consultation_request.commitment IS
  'SHA-256(JSON canonico do pedido || "|" || sal). Publicado na trilha antes de qualquer medium receber o pedido; o sal e revelado no desmascaramento.';
COMMENT ON COLUMN research.consultation_request.gender IS
  'Atributo de pareamento (fora da nota). Os tres atributos de pareamento tem as mesmas faixas do P1.';

-- Um pedido ativo por consulente: varios pedidos da mesma pessoa criariam dependencia entre
-- unidades de analise.
CREATE UNIQUE INDEX ux_consultation_active_sitter
    ON research.consultation_request (sitter_code)
    WHERE state <> 'WITHDRAWN';

CREATE FUNCTION research.consultation_request_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'pedido % nao pode ser apagado (P4, P6)', OLD.code;
    END IF;
    IF NEW.code IS DISTINCT FROM OLD.code
       OR NEW.sitter_code IS DISTINCT FROM OLD.sitter_code
       OR NEW.questionnaire_version IS DISTINCT FROM OLD.questionnaire_version
       OR NEW.gender IS DISTINCT FROM OLD.gender
       OR NEW.age_band IS DISTINCT FROM OLD.age_band
       OR NEW.death_period IS DISTINCT FROM OLD.death_period
       OR NEW.commitment IS DISTINCT FROM OLD.commitment
       OR NEW.locked_at IS DISTINCT FROM OLD.locked_at THEN
        RAISE EXCEPTION 'pedido % esta travado: so o estado avanca (E13)', OLD.code;
    END IF;
    IF OLD.state = 'WITHDRAWN' THEN
        RAISE EXCEPTION 'pedido % foi retirado e nao volta', OLD.code;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_consultation_request_guard
    BEFORE UPDATE OR DELETE ON research.consultation_request
    FOR EACH ROW EXECUTE FUNCTION research.consultation_request_guard();

CREATE TABLE identity.consultation_detail (
    id              BIGSERIAL    PRIMARY KEY,
    request_code    VARCHAR(32)  NOT NULL UNIQUE,
    first_name_enc  TEXT         NOT NULL,
    answers_enc     TEXT         NOT NULL,
    salt_enc        TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    erased_at       TIMESTAMPTZ
);

COMMENT ON TABLE identity.consultation_detail IS
  'Primeiro nome, respostas e sal do pedido P4, cifrados. Somente o modulo participant le este esquema.';

CREATE FUNCTION identity.consultation_detail_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'detalhe do pedido % nao pode ser apagado: a desistencia apaga os dados e mantem a linha',
            OLD.request_code;
    END IF;
    -- A unica alteracao admitida e o apagamento, uma vez, na desistencia (P6).
    IF OLD.erased_at IS NULL
       AND NEW.erased_at IS NOT NULL
       AND NEW.first_name_enc = '' AND NEW.answers_enc = '' AND NEW.salt_enc = ''
       AND NEW.request_code = OLD.request_code
       AND NEW.created_at = OLD.created_at THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'detalhe do pedido % esta travado; so se admite o apagamento na desistencia',
        OLD.request_code;
END
$$;

CREATE TRIGGER trg_consultation_detail_guard
    BEFORE UPDATE OR DELETE ON identity.consultation_detail
    FOR EACH ROW EXECUTE FUNCTION identity.consultation_detail_guard();

GRANT SELECT, INSERT, UPDATE ON research.consultation_request TO airlab_app;
GRANT SELECT, INSERT, UPDATE ON identity.consultation_detail TO airlab_app;
GRANT USAGE, SELECT ON SEQUENCE research.consultation_request_id_seq,
    identity.consultation_detail_id_seq TO airlab_app;
