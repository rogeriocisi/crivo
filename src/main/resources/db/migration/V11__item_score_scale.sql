-- V11: escala de itens com as categorias que faltavam (Epico E6).
--
-- Acrescenta "encaixe em outra pessoa" e a justificativa escrita obrigatoria para os valores
-- interpretativos. A escala de quatro niveis da SPEC original obrigava o sitter a chamar de
-- acerto ou de erro um item que se encaixa em outra pessoa proxima, o que esconde justamente
-- a explicacao alternativa que a critica aponta (Hyman, 2003).

ALTER TABLE research.item_score ADD COLUMN justification TEXT;

COMMENT ON COLUMN research.item_score.justification IS
  'Explicacao escrita do avaliador. Obrigatoria para PARTIAL_OR_INTERPRETED_FIT e FITS_SOMEONE_ELSE, nula nos demais valores.';

ALTER TABLE research.item_score ADD CONSTRAINT ck_item_score_value
    CHECK (value IN ('OBVIOUS_FIT', 'PARTIAL_OR_INTERPRETED_FIT', 'NO_FIT',
                     'FITS_SOMEONE_ELSE', 'CANNOT_ASSESS'));

-- A regra vale no banco e nao so no codigo: um valor interpretativo sem explicacao e recusado.
ALTER TABLE research.item_score ADD CONSTRAINT ck_item_score_justification
    CHECK (
        (value IN ('PARTIAL_OR_INTERPRETED_FIT', 'FITS_SOMEONE_ELSE')
            AND justification IS NOT NULL AND length(btrim(justification)) > 0)
        OR
        (value IN ('OBVIOUS_FIT', 'NO_FIT', 'CANNOT_ASSESS') AND justification IS NULL)
    );
