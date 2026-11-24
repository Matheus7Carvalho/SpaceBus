--liquibase formatted sql

-- Consulta da posição da van pelo Administrador também é registrada (módulo 4, H16).
--changeset spacebus:20261124-2-consulta-localizacao
ALTER TABLE auditoria_acesso DROP CONSTRAINT ck_auditoria_acesso_acao;
ALTER TABLE auditoria_acesso ADD CONSTRAINT ck_auditoria_acesso_acao
    CHECK (acao IN ('CONSULTA_PASSAGEIROS_VIAGEM', 'CONSULTA_LOCALIZACAO_VIAGEM'));
