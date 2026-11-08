-- V4: participantes, consentimento e alvos (Epico E2, Constitution P6).
-- Dados identificaveis: somente no esquema identity, com campos criptografados pela aplicacao.

CREATE TABLE identity.participant (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(32) NOT NULL UNIQUE,
    full_name_enc    TEXT        NOT NULL,
    contact_enc      TEXT,
    created_at       TIMESTAMPTZ NOT NULL,
    withdrawn_at     TIMESTAMPTZ
);

COMMENT ON TABLE identity.participant IS
  'Dado identificavel e sensivel (LGPD). Somente o modulo participant acessa este esquema.';
COMMENT ON COLUMN identity.participant.code IS
  'Codigo pseudonimo. E a unica referencia que aparece no esquema research.';

CREATE TABLE identity.target_detail (
    id               BIGSERIAL PRIMARY KEY,
    target_code      VARCHAR(32) NOT NULL UNIQUE,
    first_name_enc   TEXT        NOT NULL,
    notes_enc        TEXT,
    created_at       TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE identity.target_detail IS
  'Detalhes do falecido-alvo. O primeiro nome e entregue ao experimentador em tempo de sessao, nunca ao redator.';

CREATE TABLE identity.consent (
    id                BIGSERIAL PRIMARY KEY,
    participant_code  VARCHAR(32)  NOT NULL,
    consent_version   VARCHAR(32)  NOT NULL,
    sensitive_data_ok BOOLEAN      NOT NULL,
    granted_at        TIMESTAMPTZ  NOT NULL,
    revoked_at        TIMESTAMPTZ
);

-- Dados de pesquisa: so codigos e atributos usados no pareamento.
CREATE TABLE research.participant_role (
    id          BIGSERIAL PRIMARY KEY,
    study_id    BIGINT      NOT NULL REFERENCES research.study (id),
    code        VARCHAR(32) NOT NULL,
    role        VARCHAR(32) NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_participant_role UNIQUE (study_id, code, role)
);

CREATE TABLE research.target (
    id           BIGSERIAL PRIMARY KEY,
    study_id     BIGINT      NOT NULL REFERENCES research.study (id),
    code         VARCHAR(32) NOT NULL UNIQUE,
    sitter_code  VARCHAR(32) NOT NULL,
    gender       VARCHAR(16) NOT NULL,
    age_band     VARCHAR(16) NOT NULL,
    death_period VARCHAR(16) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE research.target IS
  'Alvo pseudonimizado. Nenhum nome aqui: somente os atributos pre-registrados de pareamento.';

CREATE TABLE research.pair (
    id          BIGSERIAL PRIMARY KEY,
    study_id    BIGINT      NOT NULL REFERENCES research.study (id),
    code        VARCHAR(32) NOT NULL UNIQUE,
    target_a    VARCHAR(32) NOT NULL REFERENCES research.target (code),
    target_b    VARCHAR(32) NOT NULL REFERENCES research.target (code),
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_pair_distinct CHECK (target_a <> target_b)
);

COMMENT ON TABLE research.pair IS
  'Par de alvos de perfil semelhante. Cada alvo e o chamariz do outro.';
