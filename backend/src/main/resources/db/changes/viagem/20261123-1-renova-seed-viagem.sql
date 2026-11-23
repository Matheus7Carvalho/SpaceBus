--liquibase formatted sql

-- Só em dev: a cada subida do backend, as viagens da seed que ficaram no passado voltam para os
-- próximos dias (mesmos horários). Mantém a demonstração e os testes E2E funcionando sem apagar o banco.
-- Não mexe em situação nem preço, só na data; viagem com reservas não é movida.
-- CURRENT_DATE/NOW() seguem o fuso do Postgres (TZ do compose.yaml, America/Araguaina).
--changeset spacebus:20261123-1-renova-seed-viagem context:dev runAlways:true
UPDATE viagem SET data_hora_partida = CURRENT_DATE + INTERVAL '1 day' + TIME '07:00'
WHERE id = '7d3e0000-0001-4e00-a000-000000000001' AND data_hora_partida < NOW() + INTERVAL '12 hours'
  AND NOT EXISTS (SELECT 1 FROM reserva r WHERE r.viagem_id = viagem.id);
UPDATE viagem SET data_hora_partida = CURRENT_DATE + INTERVAL '1 day' + TIME '12:30'
WHERE id = '7d3e0000-0001-4e00-a000-000000000002' AND data_hora_partida < NOW() + INTERVAL '12 hours'
  AND NOT EXISTS (SELECT 1 FROM reserva r WHERE r.viagem_id = viagem.id);
UPDATE viagem SET data_hora_partida = CURRENT_DATE + INTERVAL '2 days' + TIME '18:00'
WHERE id = '7d3e0000-0001-4e00-a000-000000000003' AND data_hora_partida < NOW() + INTERVAL '12 hours'
  AND NOT EXISTS (SELECT 1 FROM reserva r WHERE r.viagem_id = viagem.id);
UPDATE viagem SET data_hora_partida = CURRENT_DATE + INTERVAL '3 days' + TIME '07:00'
WHERE id = '7d3e0000-0001-4e00-a000-000000000004' AND data_hora_partida < NOW() + INTERVAL '12 hours'
  AND NOT EXISTS (SELECT 1 FROM reserva r WHERE r.viagem_id = viagem.id);
