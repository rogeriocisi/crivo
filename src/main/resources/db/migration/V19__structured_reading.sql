-- V19: P4, fase C (SPEC 0.11, E13): estudo P4, pares, pedidos ao medium, leituras travadas e
-- liberacao dos primeiros nomes.
--
--   research.structured_study    o que o estudo P4 congelou e o ciclo precisa consultar: versao do
--                                questionario, N de pares e prazo do medium (o documento com hash
--                                fica em research.preregistration_version);
--   research.structured_pair     dois pedidos travados do mesmo estrato; desfeito, nunca apagado;
--   research.reading_assignment  o envio de um par a um medium sorteado, com a lista de elegiveis
--                                e a ordem dos nomes, para que o sorteio se refaca depois;
--   research.structured_reading  a leitura do medium para um dos nomes, travada com compromisso
--                                salgado; respostas e sal cifrados;
--   identity.first_name_release  a liberacao do primeiro nome a um medium. Sem ela, o modulo
--                                participant nao entrega o nome a ninguem.
--
-- O que o banco garante, alem do codigo:
--   1. nao existe par nem envio sobre pedido que nao esteja travado: as chaves estrangeiras apontam
--      para research.consultation_request, que so existe travado e com compromisso (V18);
--   2. um pedido esta em no maximo um par ativo, e um par tem no maximo um envio aberto;
--   3. envio, par e liberacao so mudam para encerrar; leitura travada nao muda nem some.

CREATE TABLE research.structured_study (
    study_id               BIGINT       PRIMARY KEY REFERENCES research.study (id),
    questionnaire_version  VARCHAR(32)  NOT NULL,
    target_pairs           INTEGER      NOT NULL CHECK (target_pairs >= 1),
    deadline_days          INTEGER      NOT NULL CHECK (deadline_days BETWEEN 1 AND 90),
    created_at             TIMESTAMPTZ  NOT NULL
);

COMMENT ON TABLE research.structured_study IS
  'Estudo P4: o que o pre-registro congelou e o ciclo automatico consulta. Imutavel.';

CREATE FUNCTION research.structured_study_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'estudo P4 % esta congelado (P2)', OLD.study_id;
END
$$;

CREATE TRIGGER trg_structured_study_guard
    BEFORE UPDATE OR DELETE ON research.structured_study
    FOR EACH ROW EXECUTE FUNCTION research.structured_study_guard();

CREATE TABLE research.structured_pair (
    id                  BIGSERIAL    PRIMARY KEY,
    code                VARCHAR(32)  NOT NULL UNIQUE,
    study_id            BIGINT       NOT NULL REFERENCES research.structured_study (study_id),
    request_a           VARCHAR(32)  NOT NULL REFERENCES research.consultation_request (code),
    request_b           VARCHAR(32)  NOT NULL REFERENCES research.consultation_request (code),
    formed_at           TIMESTAMPTZ  NOT NULL,
    dissolved_at        TIMESTAMPTZ,
    dissolution_reason  VARCHAR(32),
    CONSTRAINT ck_structured_pair_distinct CHECK (request_a <> request_b),
    CONSTRAINT ck_structured_pair_dissolved CHECK ((dissolved_at IS NULL) = (dissolution_reason IS NULL))
);

COMMENT ON TABLE research.structured_pair IS
  'Par do P4: dois pedidos travados do mesmo estrato; cada um e o chamariz do outro.';

CREATE UNIQUE INDEX ux_structured_pair_active_a ON research.structured_pair (request_a) WHERE dissolved_at IS NULL;
CREATE UNIQUE INDEX ux_structured_pair_active_b ON research.structured_pair (request_b) WHERE dissolved_at IS NULL;

CREATE FUNCTION research.structured_pair_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'par % nao pode ser apagado (P4)', OLD.code;
    END IF;
    IF OLD.dissolved_at IS NULL
       AND NEW.dissolved_at IS NOT NULL
       AND NEW.code = OLD.code AND NEW.study_id = OLD.study_id
       AND NEW.request_a = OLD.request_a AND NEW.request_b = OLD.request_b
       AND NEW.formed_at = OLD.formed_at THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'par % so admite ser desfeito, uma vez', OLD.code;
END
$$;

CREATE TRIGGER trg_structured_pair_guard
    BEFORE UPDATE OR DELETE ON research.structured_pair
    FOR EACH ROW EXECUTE FUNCTION research.structured_pair_guard();

CREATE TABLE research.reading_assignment (
    id              BIGSERIAL    PRIMARY KEY,
    code            VARCHAR(32)  NOT NULL UNIQUE,
    pair_code       VARCHAR(32)  NOT NULL REFERENCES research.structured_pair (code),
    attempt         INTEGER      NOT NULL CHECK (attempt >= 1),
    medium_code     VARCHAR(32)  NOT NULL,
    pool            TEXT         NOT NULL,
    pool_sha256     CHAR(64)     NOT NULL,
    first_request   VARCHAR(32)  NOT NULL REFERENCES research.consultation_request (code),
    second_request  VARCHAR(32)  NOT NULL REFERENCES research.consultation_request (code),
    assigned_at     TIMESTAMPTZ  NOT NULL,
    due_at          TIMESTAMPTZ  NOT NULL,
    state           VARCHAR(16)  NOT NULL,
    closed_at       TIMESTAMPTZ,
    CONSTRAINT uk_reading_assignment_attempt UNIQUE (pair_code, attempt),
    CONSTRAINT ck_reading_assignment_state CHECK (state IN ('OPEN', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_reading_assignment_closed CHECK ((state = 'OPEN') = (closed_at IS NULL)),
    CONSTRAINT ck_reading_assignment_order CHECK (first_request <> second_request),
    CONSTRAINT ck_reading_assignment_due CHECK (due_at > assigned_at),
    CONSTRAINT ck_reading_assignment_pool CHECK (pool_sha256 ~ '^[0-9a-f]{64}$')
);

COMMENT ON TABLE research.reading_assignment IS
  'Envio de um par do P4 a um medium sorteado. A ligacao par -> medium e cega ate o desmascaramento: nenhuma tela do administrador a mostra e a trilha nao a registra.';
COMMENT ON COLUMN research.reading_assignment.pool IS
  'JSON canonico dos mediuns elegiveis no momento do sorteio. Com a semente revelada, refaz a escolha.';

CREATE UNIQUE INDEX ux_reading_assignment_open ON research.reading_assignment (pair_code) WHERE state = 'OPEN';

CREATE FUNCTION research.reading_assignment_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'envio % nao pode ser apagado (P4)', OLD.code;
    END IF;
    IF OLD.state = 'OPEN'
       AND NEW.state <> 'OPEN'
       AND NEW.code = OLD.code AND NEW.pair_code = OLD.pair_code AND NEW.attempt = OLD.attempt
       AND NEW.medium_code = OLD.medium_code AND NEW.pool = OLD.pool AND NEW.pool_sha256 = OLD.pool_sha256
       AND NEW.first_request = OLD.first_request AND NEW.second_request = OLD.second_request
       AND NEW.assigned_at = OLD.assigned_at AND NEW.due_at = OLD.due_at THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'envio % so admite ser encerrado, uma vez', OLD.code;
END
$$;

CREATE TRIGGER trg_reading_assignment_guard
    BEFORE UPDATE OR DELETE ON research.reading_assignment
    FOR EACH ROW EXECUTE FUNCTION research.reading_assignment_guard();

CREATE TABLE research.structured_reading (
    id               BIGSERIAL    PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    assignment_code  VARCHAR(32)  NOT NULL REFERENCES research.reading_assignment (code),
    slot             SMALLINT     NOT NULL CHECK (slot IN (1, 2)),
    request_code     VARCHAR(32)  NOT NULL REFERENCES research.consultation_request (code),
    questionnaire_version VARCHAR(32) NOT NULL,
    answers_enc      TEXT         NOT NULL,
    salt_enc         TEXT         NOT NULL,
    commitment       CHAR(64)     NOT NULL,
    locked_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_structured_reading_slot UNIQUE (assignment_code, slot),
    CONSTRAINT uk_structured_reading_request UNIQUE (assignment_code, request_code),
    CONSTRAINT ck_structured_reading_commitment CHECK (commitment ~ '^[0-9a-f]{64}$')
);

COMMENT ON TABLE research.structured_reading IS
  'Leitura do medium no P4, para um dos nomes do par. Nasce travada; respostas e sal cifrados ate o desmascaramento.';

CREATE FUNCTION research.structured_reading_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'leitura % esta travada (E13, trava 3 do P4)', OLD.code;
END
$$;

CREATE TRIGGER trg_structured_reading_guard
    BEFORE UPDATE OR DELETE ON research.structured_reading
    FOR EACH ROW EXECUTE FUNCTION research.structured_reading_guard();

CREATE TABLE identity.first_name_release (
    id               BIGSERIAL    PRIMARY KEY,
    assignment_code  VARCHAR(32)  NOT NULL,
    request_code     VARCHAR(32)  NOT NULL,
    medium_code      VARCHAR(32)  NOT NULL,
    released_at      TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ,
    CONSTRAINT uk_first_name_release UNIQUE (assignment_code, request_code)
);

COMMENT ON TABLE identity.first_name_release IS
  'A quem o primeiro nome de um pedido P4 foi liberado. Somente o modulo participant le este esquema.';

CREATE FUNCTION identity.first_name_release_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'liberacao de nome nao pode ser apagada (P4)';
    END IF;
    IF OLD.revoked_at IS NULL
       AND NEW.revoked_at IS NOT NULL
       AND NEW.assignment_code = OLD.assignment_code AND NEW.request_code = OLD.request_code
       AND NEW.medium_code = OLD.medium_code AND NEW.released_at = OLD.released_at THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'liberacao de nome so admite ser revogada, uma vez';
END
$$;

CREATE TRIGGER trg_first_name_release_guard
    BEFORE UPDATE OR DELETE ON identity.first_name_release
    FOR EACH ROW EXECUTE FUNCTION identity.first_name_release_guard();

GRANT SELECT, INSERT, UPDATE ON research.structured_study, research.structured_pair,
    research.reading_assignment, research.structured_reading TO airlab_app;
GRANT SELECT, INSERT, UPDATE ON identity.first_name_release TO airlab_app;
GRANT USAGE, SELECT ON SEQUENCE research.structured_pair_id_seq, research.reading_assignment_id_seq,
    research.structured_reading_id_seq, identity.first_name_release_id_seq TO airlab_app;
