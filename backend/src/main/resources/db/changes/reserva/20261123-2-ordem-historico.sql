--liquibase formatted sql

-- Desempate da ordem do histórico: ações gravadas na mesma transação podem ter a mesma data_criacao.
--changeset spacebus:20261123-2-ordem-historico
ALTER TABLE reserva_historico ADD COLUMN ordem BIGINT GENERATED ALWAYS AS IDENTITY;
DROP INDEX ix_reserva_historico_reserva;
CREATE INDEX ix_reserva_historico_reserva ON reserva_historico (reserva_id, ordem);
