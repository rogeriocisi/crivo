-- V7: redacao em itens avaliaveis (Epico E5).

CREATE TABLE research.redacted_reading (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(32) NOT NULL UNIQUE,
    reading_code   VARCHAR(32) NOT NULL REFERENCES research.reading (code),
    version        INTEGER     NOT NULL,
    redactor_code  VARCHAR(32) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_redacted_reading_version UNIQUE (reading_code, version)
);

CREATE TABLE research.reading_item (
    id                    BIGSERIAL PRIMARY KEY,
    redacted_reading_code VARCHAR(32) NOT NULL REFERENCES research.redacted_reading (code),
    ordinal               INTEGER     NOT NULL,
    statement             TEXT        NOT NULL,
    CONSTRAINT uk_reading_item_ordinal UNIQUE (redacted_reading_code, ordinal)
);

COMMENT ON TABLE research.reading_item IS
  'Um item = uma unica afirmacao verificavel (SPEC 2.1). Sem o primeiro nome do alvo e sem pistas de sessao.';
