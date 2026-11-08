-- V6: sessoes e leituras brutas (Epico E4).

CREATE TABLE research.study_session (
    id                BIGSERIAL PRIMARY KEY,
    study_id          BIGINT      NOT NULL REFERENCES research.study (id),
    code              VARCHAR(32) NOT NULL UNIQUE,
    pair_code         VARCHAR(32) NOT NULL REFERENCES research.pair (code),
    target_code       VARCHAR(32) NOT NULL REFERENCES research.target (code),
    medium_code       VARCHAR(32) NOT NULL,
    experimenter_code VARCHAR(32) NOT NULL,
    reading_order     INTEGER     NOT NULL,
    scheduled_at      TIMESTAMPTZ,
    conducted_at      TIMESTAMPTZ,
    status            VARCHAR(32) NOT NULL,
    compromised       BOOLEAN     NOT NULL DEFAULT FALSE
);

COMMENT ON COLUMN research.study_session.compromised IS
  'Marcada como comprometida por um incidente de cegamento (P1). Nunca e silenciada.';

CREATE TABLE research.reading (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(32) NOT NULL UNIQUE,
    session_code  VARCHAR(32) NOT NULL REFERENCES research.study_session (code),
    media_ref     VARCHAR(64),
    transcript    TEXT,
    status        VARCHAR(32) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

COMMENT ON COLUMN research.reading.media_ref IS
  'Identificador aleatorio do arquivo armazenado (P1, regra arquitetural 7). Nunca o nome original.';

CREATE TABLE research.session_occurrence (
    id            BIGSERIAL PRIMARY KEY,
    session_code  VARCHAR(32) NOT NULL REFERENCES research.study_session (code),
    kind          VARCHAR(32) NOT NULL,
    note          TEXT        NOT NULL,
    recorded_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE research.incident (
    id            BIGSERIAL PRIMARY KEY,
    study_id      BIGINT      NOT NULL REFERENCES research.study (id),
    code          VARCHAR(32) NOT NULL UNIQUE,
    kind          VARCHAR(32) NOT NULL,
    description   TEXT        NOT NULL,
    reported_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE research.incident_session (
    incident_code VARCHAR(32) NOT NULL REFERENCES research.incident (code),
    session_code  VARCHAR(32) NOT NULL REFERENCES research.study_session (code),
    PRIMARY KEY (incident_code, session_code)
);
