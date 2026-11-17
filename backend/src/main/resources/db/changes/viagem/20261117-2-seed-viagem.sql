--liquibase formatted sql

-- Partidas relativas à data da carga, para a seed sempre ter viagens futuras.
--changeset spacebus:20261117-2-seed-viagem context:dev
INSERT INTO viagem (id, data_criacao, rota_id, veiculo_id, motorista_id, data_hora_partida, preco, capacidade, status)
VALUES ('7d3e0000-0001-4e00-a000-000000000001', NOW(), '8b2c0000-0001-4d00-9000-000000000001', '9a1b0000-0001-4c00-8000-000000000001',
        '2b7d1e10-0001-4a3c-8f00-000000000003', CURRENT_DATE + INTERVAL '1 day' + TIME '07:00', 18.50, 15, 'PUBLICADA'),
       ('7d3e0000-0001-4e00-a000-000000000002', NOW(), '8b2c0000-0001-4d00-9000-000000000001', '9a1b0000-0001-4c00-8000-000000000002',
        '2b7d1e10-0001-4a3c-8f00-000000000005', CURRENT_DATE + INTERVAL '1 day' + TIME '12:30', 18.50, 12, 'PUBLICADA'),
       ('7d3e0000-0001-4e00-a000-000000000003', NOW(), '8b2c0000-0001-4d00-9000-000000000001', '9a1b0000-0001-4c00-8000-000000000001',
        NULL, CURRENT_DATE + INTERVAL '2 days' + TIME '18:00', 20.00, 15, 'RASCUNHO'),
       ('7d3e0000-0001-4e00-a000-000000000004', NOW(), '8b2c0000-0001-4d00-9000-000000000001', '9a1b0000-0001-4c00-8000-000000000002',
        NULL, CURRENT_DATE + INTERVAL '3 days' + TIME '07:00', 18.50, 12, 'CANCELADA');
