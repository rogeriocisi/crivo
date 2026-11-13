-- V20: P4, fase D (SPEC 0.12, E13): notas por par e relatorio do estudo.
--
--   research.structured_score   a conferencia de cada par ativo: campos comparaveis, as quatro
--                               contagens s(R, X) e o d; com o medium, agora revelado;
--   research.structured_report  o relatorio do estudo: numeros da analise e o documento canonico
--                               com hash.
--
-- O que o banco garante: nenhuma nota e nenhum relatorio existem antes do desmascaramento. As duas
-- tabelas recusam linha de estudo que nao esteja desmascarado (UNBLINDED ou REPORTED), e nenhuma
-- linha muda nem some depois de escrita (E10, trava 1; E13, trava 5 do P4).

CREATE TABLE research.structured_score (
    id                 BIGSERIAL    PRIMARY KEY,
    study_id           BIGINT       NOT NULL REFERENCES research.structured_study (study_id),
    pair_code          VARCHAR(32)  NOT NULL UNIQUE REFERENCES research.structured_pair (code),
    assignment_code    VARCHAR(32)  NOT NULL REFERENCES research.reading_assignment (code),
    medium_code        VARCHAR(32)  NOT NULL,
    comparable_fields  TEXT         NOT NULL,
    comparable_count   INTEGER      NOT NULL CHECK (comparable_count >= 0),
    s_aa               INTEGER      NOT NULL CHECK (s_aa BETWEEN 0 AND comparable_count),
    s_ab               INTEGER      NOT NULL CHECK (s_ab BETWEEN 0 AND comparable_count),
    s_bb               INTEGER      NOT NULL CHECK (s_bb BETWEEN 0 AND comparable_count),
    s_ba               INTEGER      NOT NULL CHECK (s_ba BETWEEN 0 AND comparable_count),
    d                  INTEGER      NOT NULL,
    computed_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_structured_score_d CHECK (d = (s_aa - s_ab) + (s_bb - s_ba))
);

COMMENT ON TABLE research.structured_score IS
  'Conferencia de um par do P4. So existe depois do desmascaramento: a nota nao e escondida antes, ela nao e calculada.';

CREATE TABLE research.structured_report (
    study_id            BIGINT       PRIMARY KEY REFERENCES research.structured_study (study_id),
    pairs               INTEGER      NOT NULL CHECK (pairs >= 0),
    primary_p           VARCHAR(64)  NOT NULL,
    primary_method      TEXT         NOT NULL,
    sign_p              VARCHAR(64)  NOT NULL,
    permutation_p       VARCHAR(64)  NOT NULL,
    sitter_verified     INTEGER      NOT NULL,
    sitter_total        INTEGER      NOT NULL,
    reading_verified    INTEGER      NOT NULL,
    reading_total       INTEGER      NOT NULL,
    seed_hex            CHAR(64)     NOT NULL,
    document            TEXT         NOT NULL,
    document_sha256     CHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_structured_report_sha CHECK (document_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_structured_report_seed CHECK (seed_hex ~ '^[0-9a-f]{64}$')
);

COMMENT ON TABLE research.structured_report IS
  'Relatorio do estudo P4, escrito uma vez, no desmascaramento. O hash do documento vai a trilha.';

CREATE FUNCTION research.structured_unblinded_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'nota e relatorio do P4 nao mudam nem somem (E13)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM research.study
                   WHERE id = NEW.study_id AND status IN ('UNBLINDED', 'REPORTED')) THEN
        RAISE EXCEPTION 'estudo % nao esta desmascarado: nenhuma nota existe antes (E10, trava 1)', NEW.study_id;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_structured_score_unblinded_only
    BEFORE INSERT OR UPDATE OR DELETE ON research.structured_score
    FOR EACH ROW EXECUTE FUNCTION research.structured_unblinded_only();

CREATE TRIGGER trg_structured_report_unblinded_only
    BEFORE INSERT OR UPDATE OR DELETE ON research.structured_report
    FOR EACH ROW EXECUTE FUNCTION research.structured_unblinded_only();

GRANT SELECT, INSERT ON research.structured_score, research.structured_report TO airlab_app;
GRANT USAGE, SELECT ON SEQUENCE research.structured_score_id_seq TO airlab_app;
