--liquibase formatted sql

-- Usuários de demonstração. Senha de todos: demo123 (hash BCrypt).
--changeset spacebus:20261117-2-seed-usuario context:dev
INSERT INTO usuario (id, data_criacao, nome, email, senha, perfil, ativo)
VALUES ('2b7d1e10-0001-4a3c-8f00-000000000001', NOW(), 'Rafael Mendes', 'admin@spacebus.local',
        '$2a$10$Sv6PEjrPDPtkzxombJkbSO5RwpavlyeaB30ZpRKg0t0s83ra0iU1a', 'ADMINISTRADOR', TRUE),
       ('2b7d1e10-0001-4a3c-8f00-000000000002', NOW(), 'Ana Operadora', 'operador@spacebus.local',
        '$2a$10$Sv6PEjrPDPtkzxombJkbSO5RwpavlyeaB30ZpRKg0t0s83ra0iU1a', 'OPERADOR', TRUE),
       ('2b7d1e10-0001-4a3c-8f00-000000000003', NOW(), 'João Costa', 'motorista@spacebus.local',
        '$2a$10$Sv6PEjrPDPtkzxombJkbSO5RwpavlyeaB30ZpRKg0t0s83ra0iU1a', 'MOTORISTA', TRUE),
       ('2b7d1e10-0001-4a3c-8f00-000000000004', NOW(), 'Paula Lima', 'passageiro@spacebus.local',
        '$2a$10$Sv6PEjrPDPtkzxombJkbSO5RwpavlyeaB30ZpRKg0t0s83ra0iU1a', 'PASSAGEIRO', TRUE),
       ('2b7d1e10-0001-4a3c-8f00-000000000005', NOW(), 'Caio Nunes', 'caio.nunes@spacebus.local',
        '$2a$10$Sv6PEjrPDPtkzxombJkbSO5RwpavlyeaB30ZpRKg0t0s83ra0iU1a', 'MOTORISTA', TRUE);
