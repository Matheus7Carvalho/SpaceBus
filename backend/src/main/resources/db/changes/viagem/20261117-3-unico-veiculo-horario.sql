--liquibase formatted sql

-- Garante no banco a regra "mesmo veículo, mesmo horário" (ViagemService), inclusive sob requisições simultâneas.
--changeset spacebus:20261117-3-unico-veiculo-horario
CREATE UNIQUE INDEX uk_viagem_veiculo_partida_ativa ON viagem (veiculo_id, data_hora_partida) WHERE status <> 'CANCELADA';
