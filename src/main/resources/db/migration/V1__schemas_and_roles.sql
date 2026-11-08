-- V1: esquemas de dados e papel da aplicacao.
-- Constitution P1/P6: dados identificaveis ficam em esquema separado dos dados de pesquisa.
-- Constitution P4: o esquema audit e append-only.

CREATE SCHEMA IF NOT EXISTS research;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS audit;

COMMENT ON SCHEMA research IS 'Dados de pesquisa, pseudonimizados. Nenhum dado identificavel aqui.';
COMMENT ON SCHEMA identity IS 'Dados identificaveis e sensiveis (LGPD). Acessado somente pelo modulo participant.';
COMMENT ON SCHEMA audit  IS 'Trilha de auditoria append-only, encadeada por hash.';
