-- V9: desmascaramento e analise (Epico E7).

CREATE TABLE research.unblinding (
    id            BIGSERIAL PRIMARY KEY,
    study_id      BIGINT      NOT NULL UNIQUE REFERENCES research.study (id),
    performed_at  TIMESTAMPTZ NOT NULL,
    performed_by  VARCHAR(32) NOT NULL
);

COMMENT ON TABLE research.unblinding IS
  'Operacao explicita e irreversivel (regra arquitetural 5). Uma linha por estudo, sem UPDATE nem DELETE.';

CREATE OR REPLACE FUNCTION research.reject_unblinding_change() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'unblinding is irreversible: % is not allowed', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER unblinding_no_update
    BEFORE UPDATE ON research.unblinding
    FOR EACH ROW EXECUTE FUNCTION research.reject_unblinding_change();

CREATE TRIGGER unblinding_no_delete
    BEFORE DELETE ON research.unblinding
    FOR EACH ROW EXECUTE FUNCTION research.reject_unblinding_change();

CREATE TABLE research.analysis_run (
    id                     BIGSERIAL PRIMARY KEY,
    study_id               BIGINT      NOT NULL REFERENCES research.study (id),
    preregistration_sha256 CHAR(64)    NOT NULL,
    kind                   VARCHAR(32) NOT NULL,
    result                 TEXT        NOT NULL,
    executed_at            TIMESTAMPTZ NOT NULL
);

COMMENT ON COLUMN research.analysis_run.kind IS
  'PRIMARY (pre-registrada) ou EXPLORATORY. Analise fora do pre-registro e sempre rotulada (P2).';
