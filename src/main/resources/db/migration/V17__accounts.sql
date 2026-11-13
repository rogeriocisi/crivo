-- V17: contas do protocolo P4, com perfil fixo, e avisos ao administrador (SPEC 0.9, E13, fase A).
--
-- No P4 o consulente e o medium tem conta, porque o protocolo roda inteiro pelo site. A conta e
-- dado identificavel: fica no esquema identity, cifrada pela aplicacao, e so o modulo
-- participant a le. A pesquisa enxerga apenas o codigo pseudonimo (CON-..., MED-...).
--
-- Duas regras ficam no banco, e nao so no codigo:
--   1. o perfil e fixo: uma conta nunca deixa de ser de consulente para virar de medium, nem o
--      contrario (E10, trava 2 -- com um mesmo e-mail ninguem fica dos dois lados);
--   2. nenhuma conta e apagada: a desistencia apaga os dados identificaveis e o codigo
--      pseudonimo permanece, marcado como encerrado (P6).
-- Nao existe perfil de administrador aqui: o administrador vem da configuracao do servidor.

CREATE TABLE identity.account (
    id                       BIGSERIAL    PRIMARY KEY,
    code                     VARCHAR(32)  NOT NULL UNIQUE,
    profile                  VARCHAR(16)  NOT NULL,
    full_name_enc            TEXT         NOT NULL,
    email_enc                TEXT,
    email_fingerprint        CHAR(64),
    password_hash            VARCHAR(100),
    term_version             VARCHAR(32)  NOT NULL,
    sensitive_data_accepted  BOOLEAN      NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    email_confirmed_at       TIMESTAMPTZ,
    closed_at                TIMESTAMPTZ,
    CONSTRAINT ck_account_profile CHECK (profile IN ('SITTER', 'MEDIUM')),
    CONSTRAINT ck_account_term CHECK (sensitive_data_accepted),
    CONSTRAINT ck_account_closed_erased CHECK (
        closed_at IS NULL OR (email_enc IS NULL AND email_fingerprint IS NULL AND password_hash IS NULL))
);

COMMENT ON TABLE identity.account IS
  'Contas do P4 (consulente ou medium). Dado identificavel: somente o modulo participant le este esquema.';
COMMENT ON COLUMN identity.account.profile IS
  'Perfil fixo, escolhido na criacao da conta. Um trigger recusa qualquer mudanca. ADMIN nao e valor possivel.';
COMMENT ON COLUMN identity.account.email_fingerprint IS
  'HMAC-SHA256 do e-mail normalizado, sob chave da aplicacao: uma conta ativa por e-mail, sem guardar o e-mail em claro.';

-- Uma conta ativa por e-mail. Conta encerrada perde o indice e libera o endereco.
CREATE UNIQUE INDEX ux_account_email_fingerprint
    ON identity.account (email_fingerprint)
    WHERE closed_at IS NULL;

CREATE FUNCTION identity.account_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'conta % nao pode ser apagada: a desistencia apaga os dados e mantem o codigo (P6)',
            OLD.code;
    END IF;
    IF NEW.profile IS DISTINCT FROM OLD.profile THEN
        RAISE EXCEPTION 'conta %: o perfil e fixo e nao muda (E13, E10 trava 2)', OLD.code;
    END IF;
    IF NEW.code IS DISTINCT FROM OLD.code THEN
        RAISE EXCEPTION 'conta %: o codigo pseudonimo nao muda', OLD.code;
    END IF;
    IF OLD.closed_at IS NOT NULL THEN
        RAISE EXCEPTION 'conta % esta encerrada e nao volta', OLD.code;
    END IF;
    IF OLD.email_confirmed_at IS NOT NULL AND NEW.email_confirmed_at IS DISTINCT FROM OLD.email_confirmed_at THEN
        RAISE EXCEPTION 'conta %: a data de confirmacao do e-mail nao se reescreve', OLD.code;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_account_guard
    BEFORE UPDATE OR DELETE ON identity.account
    FOR EACH ROW EXECUTE FUNCTION identity.account_guard();

-- Link de confirmacao do e-mail: uso unico, expiravel, guardado so como hash (E10, trava 3).
CREATE TABLE identity.email_confirmation (
    id             BIGSERIAL    PRIMARY KEY,
    account_code   VARCHAR(32)  NOT NULL REFERENCES identity.account (code),
    token_sha256   CHAR(64)     NOT NULL UNIQUE,
    created_at     TIMESTAMPTZ  NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    used_at        TIMESTAMPTZ,
    CONSTRAINT ck_email_confirmation_window CHECK (expires_at > created_at)
);

COMMENT ON TABLE identity.email_confirmation IS
  'Links de confirmacao de e-mail. O token em claro so existe no e-mail enviado; aqui fica o SHA-256 dele.';

-- Avisos ao administrador: tipo, codigo pseudonimo e hora. Nunca nome, resposta ou nota.
CREATE TABLE research.admin_notice (
    id             BIGSERIAL    PRIMARY KEY,
    kind           VARCHAR(48)  NOT NULL,
    subject_code   VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL
);

COMMENT ON TABLE research.admin_notice IS
  'Avisos de etapa concluida ao administrador (E13). Painel e e-mail nao sao canais cegos: so tipo, codigo e hora.';

CREATE INDEX idx_admin_notice_created ON research.admin_notice (created_at DESC);

GRANT SELECT, INSERT, UPDATE ON identity.account TO airlab_app;
GRANT SELECT, INSERT, UPDATE ON identity.email_confirmation TO airlab_app;
GRANT SELECT, INSERT ON research.admin_notice TO airlab_app;
GRANT USAGE, SELECT ON SEQUENCE identity.account_id_seq, identity.email_confirmation_id_seq,
    research.admin_notice_id_seq TO airlab_app;
