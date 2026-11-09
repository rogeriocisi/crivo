-- V12: desvios de protocolo (Nosek et al., 2018).
--
-- Um desvio nao e uma emenda. A emenda muda o plano e gera nova versao do pre-registro; o
-- desvio e o registro de que a execucao se afastou do plano congelado -- a coleta parou em 60
-- de 100, apareceu uma exclusao que ninguem previu -- e precisa aparecer no relatorio junto
-- com o resultado, com data e motivo. Nosek et al.: o pre-registro com desvios relatados de
-- forma transparente da mais confianca do que nao ter pre-registro nenhum.

CREATE TABLE research.protocol_deviation (
    id                      BIGSERIAL PRIMARY KEY,
    study_id                BIGINT       NOT NULL REFERENCES research.study (id),
    preregistration_version INTEGER      NOT NULL,
    kind                    VARCHAR(32)  NOT NULL,
    description             TEXT         NOT NULL,
    occurred_at             TIMESTAMPTZ  NOT NULL,
    recorded_at             TIMESTAMPTZ  NOT NULL,
    recorded_by             VARCHAR(32)  NOT NULL
);

COMMENT ON TABLE research.protocol_deviation IS
  'Append-only: um desvio registrado nunca e apagado nem reescrito, senao o relatorio poderia omiti-lo.';
COMMENT ON COLUMN research.protocol_deviation.kind IS
  'SAMPLE_SIZE, EXCLUSION, SCHEDULE, BLINDING, SCORING, ANALYSIS ou OTHER.';

CREATE INDEX idx_protocol_deviation_study ON research.protocol_deviation (study_id);

CREATE OR REPLACE FUNCTION research.reject_deviation_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'protocol deviations are append-only: % is not allowed', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER protocol_deviation_no_update
    BEFORE UPDATE ON research.protocol_deviation
    FOR EACH ROW EXECUTE FUNCTION research.reject_deviation_mutation();

CREATE TRIGGER protocol_deviation_no_delete
    BEFORE DELETE ON research.protocol_deviation
    FOR EACH ROW EXECUTE FUNCTION research.reject_deviation_mutation();
