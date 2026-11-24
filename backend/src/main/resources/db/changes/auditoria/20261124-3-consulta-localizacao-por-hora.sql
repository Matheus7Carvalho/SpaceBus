--liquibase formatted sql

-- Consulta da localização pelo Administrador: no máximo um registro por usuário, viagem e hora cheia, garantido no
-- banco (consultas simultâneas da atualização automática). Remove duplicatas antigas antes de criar o índice.
--changeset spacebus:20261124-3-consulta-localizacao-por-hora
DELETE FROM auditoria_acesso a
USING auditoria_acesso b
WHERE a.acao = 'CONSULTA_LOCALIZACAO_VIAGEM' AND b.acao = a.acao
  AND a.usuario_id = b.usuario_id AND a.recurso_id = b.recurso_id
  AND date_trunc('hour', a.data_criacao) = date_trunc('hour', b.data_criacao)
  AND (a.data_criacao, a.id) > (b.data_criacao, b.id);

CREATE UNIQUE INDEX uk_auditoria_acesso_por_hora
    ON auditoria_acesso (usuario_id, acao, recurso_id, date_trunc('hour', data_criacao))
    WHERE acao = 'CONSULTA_LOCALIZACAO_VIAGEM';
