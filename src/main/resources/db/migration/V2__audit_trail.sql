-- V2: trilha de auditoria append-only encadeada por hash (Constitution P4).

CREATE TABLE audit.event (
    seq            BIGSERIAL PRIMARY KEY,
    occurred_at    TIMESTAMPTZ   NOT NULL,
    event_type     VARCHAR(64)   NOT NULL,
    actor_role     VARCHAR(32)   NOT NULL,
    actor_code     VARCHAR(64),
    subject_ref    VARCHAR(128),
    payload        TEXT          NOT NULL,
    prev_hash      CHAR(64)      NOT NULL,
    hash           CHAR(64)      NOT NULL UNIQUE
);

COMMENT ON TABLE audit.event IS
  'Append-only. UPDATE e DELETE sao bloqueados por trigger e por permissao de papel. Nunca remover.';
COMMENT ON COLUMN audit.event.payload IS
  'JSON canonico do evento. Nunca contem dado pessoal (P6): somente codigos pseudonimos e hashes.';
COMMENT ON COLUMN audit.event.prev_hash IS
  'Hash do evento anterior. O primeiro evento usa 64 zeros como ancora da cadeia.';

CREATE INDEX idx_audit_event_type ON audit.event (event_type);
CREATE INDEX idx_audit_event_subject ON audit.event (subject_ref);

-- Imposicao no banco, nao so no codigo (P4). O trigger alcanca tambem um superusuario.
CREATE OR REPLACE FUNCTION audit.reject_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit.event is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_event_no_update
    BEFORE UPDATE ON audit.event
    FOR EACH ROW EXECUTE FUNCTION audit.reject_mutation();

CREATE TRIGGER audit_event_no_delete
    BEFORE DELETE ON audit.event
    FOR EACH ROW EXECUTE FUNCTION audit.reject_mutation();

CREATE TRIGGER audit_event_no_truncate
    BEFORE TRUNCATE ON audit.event
    EXECUTE FUNCTION audit.reject_mutation();
