-- V13: vocabulario em portugues -- "sitter" passa a "consulente" (SPEC 0.5).
--
-- Os comentarios em portugues gravados no banco pelas migracoes anteriores diziam "sitter".
-- As migracoes ja aplicadas nao sao editadas: mudar o arquivo quebraria o checksum do Flyway
-- em qualquer banco existente. A correcao vem aqui, como migracao nova.
--
-- Os identificadores continuam em ingles (sitter_code, ParticipantRole.SITTER), pela regra
-- de codigo em ingles e documentacao em portugues.

COMMENT ON TABLE research.scoring_packet IS
  'Pacote de avaliacao do consulente (sitter). A ordem de apresentacao vem do sorteio e nao indica qual leitura e a dele (P1).';

COMMENT ON COLUMN research.scoring_packet.sitter_code IS
  'Codigo pseudonimo do consulente. "Sitter" e a forma inglesa, mantida no identificador.';

COMMENT ON COLUMN research.target.sitter_code IS
  'Codigo pseudonimo do consulente que indicou este alvo.';

COMMENT ON COLUMN research.item_score.justification IS
  'Explicacao escrita do consulente. Obrigatoria para PARTIAL_OR_INTERPRETED_FIT e FITS_SOMEONE_ELSE, nula nos demais valores.';
