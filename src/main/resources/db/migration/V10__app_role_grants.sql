-- V10: papel da aplicacao com privilegio minimo (Constitution P4).
--
-- O papel airlab_app e criado sem login; no perfil prod a senha e atribuida fora do
-- repositorio (ver README, secao de producao). No perfil local a aplicacao conecta como
-- proprietaria do banco, e o append-only continua garantido pelos triggers de V2,
-- que alcancam qualquer papel, inclusive superusuario.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'airlab_app') THEN
        CREATE ROLE airlab_app NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA research, identity, audit TO airlab_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA research TO airlab_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA identity TO airlab_app;

-- Append-only: somente leitura e insercao na trilha de auditoria.
REVOKE ALL ON ALL TABLES IN SCHEMA audit FROM airlab_app;
GRANT SELECT, INSERT ON audit.event TO airlab_app;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA audit TO airlab_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA research TO airlab_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA identity TO airlab_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT, INSERT ON TABLES TO airlab_app;
