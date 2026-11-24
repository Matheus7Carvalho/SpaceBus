--liquibase formatted sql

-- Reembolso é pedido ao provedor depois do commit, sem trava; as tentativas são limitadas (ressalva N2 do módulo 3).
--changeset spacebus:20261124-1-tentativas-reembolso
ALTER TABLE pagamento ADD COLUMN tentativas_reembolso INTEGER NOT NULL DEFAULT 0;
