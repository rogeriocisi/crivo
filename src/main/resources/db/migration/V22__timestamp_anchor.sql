-- V22: ancoragem externa da trilha por carimbo de tempo RFC 3161 (SPEC 0.15, E8, fase 1).
--
-- Cada linha e uma tentativa de carimbar a cabeca da trilha numa autoridade de carimbo do tempo
-- (FreeTSA). O valor carimbado e o proprio hash do evento: como cada evento contem o hash do
-- anterior, um carimbo atesta a trilha inteira ate ali. Concedida, a linha guarda a resposta DER
-- (.tsr) inteira, que qualquer pessoa confere por fora com `openssl ts -verify`. Falha fica
-- registrada aqui, com o motivo, e nao na trilha: ela nao muda o estado de nenhum estudo.
--
-- No esquema audit, onde o papel da aplicacao so tem SELECT e INSERT (V10). Append-only por
-- trigger, como a trilha, e o hash carimbado tem de ser o de um evento que existe.

CREATE TABLE audit.timestamp_anchor (
    id               BIGSERIAL    PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    anchored_seq     BIGINT       NOT NULL REFERENCES audit.event (seq),
    anchored_hash    CHAR(64)     NOT NULL,
    authority_url    VARCHAR(255) NOT NULL,
    requested_at     TIMESTAMPTZ  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    failure_reason   VARCHAR(32),
    attested_at      TIMESTAMPTZ,
    serial_hex       VARCHAR(80),
    response_der     BYTEA,
    response_sha256  CHAR(64),
    CONSTRAINT ck_timestamp_anchor_status CHECK (status IN ('GRANTED', 'FAILED')),
    CONSTRAINT ck_timestamp_anchor_hash CHECK (anchored_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_timestamp_anchor_granted CHECK (
        (status = 'GRANTED') = (response_der IS NOT NULL AND attested_at IS NOT NULL
                                AND serial_hex IS NOT NULL AND response_sha256 IS NOT NULL)),
    CONSTRAINT ck_timestamp_anchor_failed CHECK ((status = 'FAILED') = (failure_reason IS NOT NULL)),
    CONSTRAINT ck_timestamp_anchor_reason CHECK (
        failure_reason IS NULL OR failure_reason IN ('UNREACHABLE', 'REJECTED', 'INVALID_RESPONSE'))
);

CREATE INDEX ix_timestamp_anchor_status ON audit.timestamp_anchor (status, id);

COMMENT ON TABLE audit.timestamp_anchor IS
  'Carimbos RFC 3161 da cabeca da trilha (SPEC E8). Append-only; o hash carimbado e o de um evento da trilha.';
COMMENT ON COLUMN audit.timestamp_anchor.response_der IS
  'TimeStampResp DER inteiro, publicado em /carimbos/{codigo}.tsr para conferencia externa.';

CREATE FUNCTION audit.timestamp_anchor_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NOT EXISTS (SELECT 1 FROM audit.event e
                       WHERE e.seq = NEW.anchored_seq AND e.hash = NEW.anchored_hash) THEN
            RAISE EXCEPTION 'o hash carimbado nao e o do evento % da trilha', NEW.anchored_seq
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'carimbos de tempo sao append-only: % nao e permitido', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END
$$;

CREATE TRIGGER trg_timestamp_anchor_insert
    BEFORE INSERT ON audit.timestamp_anchor
    FOR EACH ROW EXECUTE FUNCTION audit.timestamp_anchor_guard();

CREATE TRIGGER trg_timestamp_anchor_no_update
    BEFORE UPDATE ON audit.timestamp_anchor
    FOR EACH ROW EXECUTE FUNCTION audit.timestamp_anchor_guard();

CREATE TRIGGER trg_timestamp_anchor_no_delete
    BEFORE DELETE ON audit.timestamp_anchor
    FOR EACH ROW EXECUTE FUNCTION audit.timestamp_anchor_guard();

CREATE TRIGGER trg_timestamp_anchor_no_truncate
    BEFORE TRUNCATE ON audit.timestamp_anchor
    FOR EACH STATEMENT EXECUTE FUNCTION audit.timestamp_anchor_guard();

-- O padrao do esquema audit (V10) ja da SELECT e INSERT na tabela; repetido aqui por clareza.
GRANT SELECT, INSERT ON audit.timestamp_anchor TO airlab_app;
GRANT USAGE, SELECT ON SEQUENCE audit.timestamp_anchor_id_seq TO airlab_app;
