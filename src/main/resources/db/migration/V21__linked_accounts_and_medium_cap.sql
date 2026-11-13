-- V21: P4, fase E (SPEC 0.13, E13): sinalizacao de contas ligadas e teto de pares por medium.
--
--   identity.account_signal  sinais de cada conta confirmada, so como indice cego (HMAC): e-mail
--                            canonico e nome completo normalizado. Apagados na desistencia (P6);
--   identity.account_flag    duas contas que compartilham um sinal, para revisao humana. Nao
--                            bloqueia ninguem; enquanto pendente ou confirmada, o sorteio evita que
--                            as duas se encontrem num par. Decidida uma vez, nunca apagada;
--   research.structured_study.max_pairs_per_medium
--                            teto declarado no pre-registro. Nulo nos estudos anteriores a ele.

CREATE TABLE identity.account_signal (
    id            BIGSERIAL    PRIMARY KEY,
    account_code  VARCHAR(32)  NOT NULL,
    kind          VARCHAR(24)  NOT NULL,
    fingerprint   CHAR(64)     NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_account_signal UNIQUE (account_code, kind),
    CONSTRAINT ck_account_signal_kind CHECK (kind IN ('EMAIL_CANONICAL', 'FULL_NAME')),
    CONSTRAINT ck_account_signal_fingerprint CHECK (fingerprint ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_account_signal_fingerprint ON identity.account_signal (kind, fingerprint);

COMMENT ON TABLE identity.account_signal IS
  'Indice cego (HMAC) do e-mail canonico e do nome normalizado de cada conta confirmada. Nada em claro. Apagado na desistencia.';

CREATE FUNCTION identity.account_signal_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'sinal de conta nao muda; a desistencia o apaga';
END
$$;

CREATE TRIGGER trg_account_signal_guard
    BEFORE UPDATE ON identity.account_signal
    FOR EACH ROW EXECUTE FUNCTION identity.account_signal_guard();

CREATE TABLE identity.account_flag (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL UNIQUE,
    account_a    VARCHAR(32)  NOT NULL,
    account_b    VARCHAR(32)  NOT NULL,
    kind         VARCHAR(24)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    decision     VARCHAR(16),
    reviewed_at  TIMESTAMPTZ,
    reviewer     VARCHAR(64),
    CONSTRAINT uk_account_flag UNIQUE (account_a, account_b, kind),
    CONSTRAINT ck_account_flag_order CHECK (account_a < account_b),
    CONSTRAINT ck_account_flag_kind CHECK (kind IN ('EMAIL_CANONICAL', 'FULL_NAME')),
    CONSTRAINT ck_account_flag_decision CHECK (decision IS NULL OR decision IN ('LINKED', 'UNRELATED')),
    CONSTRAINT ck_account_flag_reviewed CHECK ((decision IS NULL) = (reviewed_at IS NULL))
);

COMMENT ON TABLE identity.account_flag IS
  'Contas que compartilham um sinal, para revisao humana. Sinalizar nao e bloquear (SPEC E13, fase E).';

CREATE FUNCTION identity.account_flag_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'sinalizacao % nao pode ser apagada', OLD.code;
    END IF;
    IF OLD.decision IS NULL
       AND NEW.decision IS NOT NULL
       AND NEW.code = OLD.code AND NEW.account_a = OLD.account_a AND NEW.account_b = OLD.account_b
       AND NEW.kind = OLD.kind AND NEW.created_at = OLD.created_at THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'sinalizacao % so admite uma decisao, uma vez', OLD.code;
END
$$;

CREATE TRIGGER trg_account_flag_guard
    BEFORE UPDATE OR DELETE ON identity.account_flag
    FOR EACH ROW EXECUTE FUNCTION identity.account_flag_guard();

ALTER TABLE research.structured_study
    ADD COLUMN max_pairs_per_medium INTEGER CHECK (max_pairs_per_medium IS NULL OR max_pairs_per_medium >= 1);

COMMENT ON COLUMN research.structured_study.max_pairs_per_medium IS
  'Teto de pares por medium declarado no pre-registro (E13, fase E). Nulo: estudo anterior ao teto, sem limite.';

GRANT SELECT, INSERT, DELETE ON identity.account_signal TO airlab_app;
GRANT SELECT, INSERT, UPDATE ON identity.account_flag TO airlab_app;
GRANT USAGE, SELECT ON SEQUENCE identity.account_signal_id_seq, identity.account_flag_id_seq TO airlab_app;
