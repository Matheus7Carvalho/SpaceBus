--liquibase formatted sql

--changeset spacebus:20261117-2-seed-veiculo context:dev
INSERT INTO veiculo (id, data_criacao, placa, descricao, tipo, capacidade, ativo)
VALUES ('9a1b0000-0001-4c00-8000-000000000001', NOW(), 'SBV1A23', 'Van Sprinter 416', 'VAN', 15, TRUE),
       ('9a1b0000-0001-4c00-8000-000000000002', NOW(), 'SBV2B34', 'Van Master Executiva', 'VAN', 12, TRUE),
       ('9a1b0000-0001-4c00-8000-000000000003', NOW(), 'SBO3C45', 'Micro-ônibus Volare', 'ONIBUS', 28, FALSE);
