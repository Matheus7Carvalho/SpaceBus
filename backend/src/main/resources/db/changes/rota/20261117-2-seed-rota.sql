--liquibase formatted sql

-- Rota demonstrativa. A rota real do piloto depende da transportadora parceira.
--changeset spacebus:20261117-2-seed-rota context:dev
INSERT INTO rota (id, data_criacao, nome, origem, destino)
VALUES ('8b2c0000-0001-4d00-9000-000000000001', NOW(), 'Centro – Aeroporto', 'Centro', 'Aeroporto');

INSERT INTO ponto_parada (id, data_criacao, rota_id, ordem, nome, referencia)
VALUES ('8b2c0000-0002-4d00-9000-000000000001', NOW(), '8b2c0000-0001-4d00-9000-000000000001', 1, 'Terminal Central', 'Plataforma 3'),
       ('8b2c0000-0002-4d00-9000-000000000002', NOW(), '8b2c0000-0001-4d00-9000-000000000001', 2, 'Praça Principal', 'Em frente à farmácia'),
       ('8b2c0000-0002-4d00-9000-000000000003', NOW(), '8b2c0000-0001-4d00-9000-000000000001', 3, 'Shopping', 'Portão B'),
       ('8b2c0000-0002-4d00-9000-000000000004', NOW(), '8b2c0000-0001-4d00-9000-000000000001', 4, 'Aeroporto', 'Desembarque');
