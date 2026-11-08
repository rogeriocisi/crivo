-- V8: avaliacao cega (Epico E6).

CREATE TABLE research.scoring_packet (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(32) NOT NULL UNIQUE,
    study_id            BIGINT      NOT NULL REFERENCES research.study (id),
    sitter_code         VARCHAR(32) NOT NULL,
    reading_first_code  VARCHAR(32) NOT NULL REFERENCES research.redacted_reading (code),
    reading_second_code VARCHAR(32) NOT NULL REFERENCES research.redacted_reading (code),
    access_token_hash   CHAR(64)    NOT NULL UNIQUE,
    expires_at          TIMESTAMPTZ NOT NULL,
    opened_at           TIMESTAMPTZ,
    locked_at           TIMESTAMPTZ,
    CONSTRAINT ck_packet_distinct_readings CHECK (reading_first_code <> reading_second_code)
);

COMMENT ON TABLE research.scoring_packet IS
  'Pacote de avaliacao do sitter. A ordem de apresentacao vem do sorteio e nao indica qual leitura e a dele (P1).';
COMMENT ON COLUMN research.scoring_packet.access_token_hash IS
  'Hash do link de uso unico. O token em claro nunca e persistido.';

CREATE TABLE research.item_score (
    id              BIGSERIAL PRIMARY KEY,
    packet_code     VARCHAR(32) NOT NULL REFERENCES research.scoring_packet (code),
    item_id         BIGINT      NOT NULL REFERENCES research.reading_item (id),
    value           VARCHAR(16) NOT NULL,
    CONSTRAINT uk_item_score UNIQUE (packet_code, item_id)
);

CREATE TABLE research.global_score (
    id                    BIGSERIAL PRIMARY KEY,
    packet_code           VARCHAR(32) NOT NULL REFERENCES research.scoring_packet (code),
    redacted_reading_code VARCHAR(32) NOT NULL REFERENCES research.redacted_reading (code),
    value                 INTEGER     NOT NULL,
    CONSTRAINT uk_global_score UNIQUE (packet_code, redacted_reading_code),
    CONSTRAINT ck_global_score_range CHECK (value BETWEEN 0 AND 6)
);

CREATE TABLE research.forced_choice (
    id                  BIGSERIAL PRIMARY KEY,
    packet_code         VARCHAR(32) NOT NULL UNIQUE REFERENCES research.scoring_packet (code),
    chosen_reading_code VARCHAR(32) NOT NULL REFERENCES research.redacted_reading (code),
    confidence          INTEGER     NOT NULL,
    submitted_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_confidence_range CHECK (confidence BETWEEN 0 AND 100)
);
