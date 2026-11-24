--liquibase formatted sql

-- Histórico de toda mudança de reserva e pagamento (CLAUDE.md: entidade, ação, responsável, data, resultado).
--changeset spacebus:20261123-1-historico-e-reembolso-pendente
CREATE TABLE reserva_historico
(
    id               VARCHAR(36)  NOT NULL,
    data_criacao     TIMESTAMP    NOT NULL,
    data_atualizacao TIMESTAMP,
    reserva_id       VARCHAR(36)  NOT NULL,
    acao             VARCHAR(20)  NOT NULL,
    origem           VARCHAR(10)  NOT NULL,
    -- Nulo quando quem age é o provedor de pagamento ou o sistema (expiração, nova tentativa de reembolso).
    responsavel_id   VARCHAR(36),
    detalhe          VARCHAR(200),
    CONSTRAINT pk_reserva_historico PRIMARY KEY (id),
    CONSTRAINT fk_reserva_historico_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id),
    CONSTRAINT fk_reserva_historico_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuario (id),
    CONSTRAINT ck_reserva_historico_origem CHECK (origem IN ('PASSAGEIRO', 'OPERADOR', 'PROVEDOR', 'SISTEMA'))
);

CREATE INDEX ix_reserva_historico_reserva ON reserva_historico (reserva_id, data_criacao);

-- Reembolso que o provedor não concluiu fica marcado e é tentado de novo por uma tarefa agendada.
ALTER TABLE pagamento ALTER COLUMN status TYPE VARCHAR(18);
ALTER TABLE pagamento DROP CONSTRAINT ck_pagamento_status;
ALTER TABLE pagamento ADD CONSTRAINT ck_pagamento_status
    CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'REEMBOLSO_PENDENTE', 'REEMBOLSADO'));
