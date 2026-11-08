-- V3: estudo e pre-registro (Epico E1, Constitution P2).

CREATE TABLE research.study (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    title       VARCHAR(200) NOT NULL,
    protocol    VARCHAR(32)  NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE research.preregistration_version (
    id                  BIGSERIAL PRIMARY KEY,
    study_id            BIGINT       NOT NULL REFERENCES research.study (id),
    version             INTEGER      NOT NULL,
    document            TEXT         NOT NULL,
    document_sha256     CHAR(64)     NOT NULL,
    frozen_at           TIMESTAMPTZ,
    timestamp_proof_ref VARCHAR(256),
    amendment_reason    TEXT,
    created_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_prereg_study_version UNIQUE (study_id, version)
);

COMMENT ON TABLE research.preregistration_version IS
  'Versoes do pre-registro. Uma versao congelada (frozen_at nao nulo) e imutavel; emendas criam nova versao.';

-- P2: depois de congelada, a versao nao pode mais ser alterada nem removida.
CREATE OR REPLACE FUNCTION research.reject_frozen_preregistration_change() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.frozen_at IS NOT NULL THEN
        RAISE EXCEPTION 'frozen preregistration version is immutable (study %, version %)',
            OLD.study_id, OLD.version
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prereg_frozen_no_update
    BEFORE UPDATE ON research.preregistration_version
    FOR EACH ROW EXECUTE FUNCTION research.reject_frozen_preregistration_change();

CREATE TRIGGER prereg_frozen_no_delete
    BEFORE DELETE ON research.preregistration_version
    FOR EACH ROW EXECUTE FUNCTION research.reject_frozen_preregistration_change();
