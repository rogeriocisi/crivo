-- V5: sorteio verificavel, compromisso -> revelacao (Epico E3, Constitution P5).

CREATE TABLE research.allocation_set (
    id                BIGSERIAL PRIMARY KEY,
    study_id          BIGINT      NOT NULL REFERENCES research.study (id),
    seed_commitment   CHAR(64)    NOT NULL,
    draw_input        TEXT        NOT NULL,
    seed_escrow       TEXT        NOT NULL,
    allocations       TEXT        NOT NULL,
    drawn_at          TIMESTAMPTZ NOT NULL,
    seed_hex          CHAR(64),
    seed_revealed_at  TIMESTAMPTZ,
    CONSTRAINT uk_allocation_set_study UNIQUE (study_id),
    CONSTRAINT ck_seed_reveal_consistent
        CHECK ((seed_hex IS NULL) = (seed_revealed_at IS NULL))
);

COMMENT ON COLUMN research.allocation_set.seed_commitment IS
  'SHA-256 de (semente em hex + | + alocacoes em JSON canonico). Publicado antes da primeira sessao.';
COMMENT ON COLUMN research.allocation_set.draw_input IS
  'JSON canonico das entradas do sorteio (pares, mediuns, experimentadores). Permite reproduzir o sorteio.';
COMMENT ON COLUMN research.allocation_set.seed_escrow IS
  'Semente sob custodia, cifrada (AES-GCM) com a chave da aplicacao. Escrita no sorteio e lida uma unica vez, no desmascaramento. Nenhuma consulta de dominio le esta coluna.';
COMMENT ON COLUMN research.allocation_set.seed_hex IS
  'Semente em claro. Fica nula ate o desmascaramento: antes disso ninguem, nem o pesquisador, pode le-la.';

-- P5: as alocacoes e o compromisso nunca mudam depois do sorteio; so a revelacao da semente pode escrever.
CREATE OR REPLACE FUNCTION research.reject_allocation_rewrite() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.seed_commitment <> OLD.seed_commitment OR NEW.allocations <> OLD.allocations
       OR NEW.draw_input <> OLD.draw_input OR NEW.seed_escrow <> OLD.seed_escrow
       OR NEW.drawn_at <> OLD.drawn_at OR NEW.study_id <> OLD.study_id THEN
        RAISE EXCEPTION 'allocation set is immutable after the draw (study %)', OLD.study_id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    IF OLD.seed_hex IS NOT NULL AND NEW.seed_hex <> OLD.seed_hex THEN
        RAISE EXCEPTION 'revealed seed cannot be changed (study %)', OLD.study_id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER allocation_set_immutable
    BEFORE UPDATE ON research.allocation_set
    FOR EACH ROW EXECUTE FUNCTION research.reject_allocation_rewrite();
