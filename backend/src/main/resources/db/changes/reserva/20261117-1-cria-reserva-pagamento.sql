--liquibase formatted sql

--changeset spacebus:20261117-1-cria-reserva-pagamento
CREATE TABLE reserva
(
    id                   VARCHAR(36)   NOT NULL,
    data_criacao         TIMESTAMP     NOT NULL,
    data_atualizacao     TIMESTAMP,
    codigo               VARCHAR(9)    NOT NULL,
    viagem_id            VARCHAR(36)   NOT NULL,
    passageiro_id        VARCHAR(36)   NOT NULL,
    -- Trecho copiado da rota no momento da reserva: editar a rota depois não muda a reserva.
    embarque_ordem       INTEGER       NOT NULL,
    embarque_nome        VARCHAR(100)  NOT NULL,
    embarque_referencia  VARCHAR(150),
    desembarque_ordem    INTEGER       NOT NULL,
    desembarque_nome     VARCHAR(100)  NOT NULL,
    quantidade           INTEGER       NOT NULL,
    valor_total          NUMERIC(8, 2) NOT NULL,
    status               VARCHAR(20)   NOT NULL,
    expira_em            TIMESTAMP     NOT NULL,
    confirmada_em        TIMESTAMP,
    cancelada_em         TIMESTAMP,
    motivo_cancelamento  VARCHAR(16),
    CONSTRAINT pk_reserva PRIMARY KEY (id),
    CONSTRAINT uk_reserva_codigo UNIQUE (codigo),
    CONSTRAINT fk_reserva_viagem FOREIGN KEY (viagem_id) REFERENCES viagem (id),
    CONSTRAINT fk_reserva_passageiro FOREIGN KEY (passageiro_id) REFERENCES usuario (id),
    CONSTRAINT ck_reserva_quantidade CHECK (quantidade BETWEEN 1 AND 6),
    CONSTRAINT ck_reserva_trecho CHECK (embarque_ordem < desembarque_ordem),
    CONSTRAINT ck_reserva_status CHECK (status IN ('AGUARDANDO_PAGAMENTO', 'CONFIRMADA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT ck_reserva_motivo CHECK (motivo_cancelamento IS NULL OR motivo_cancelamento IN ('PASSAGEIRO', 'VIAGEM_CANCELADA'))
);

CREATE INDEX ix_reserva_viagem_status ON reserva (viagem_id, status);
CREATE INDEX ix_reserva_passageiro ON reserva (passageiro_id, data_criacao);

CREATE TABLE pagamento
(
    id               VARCHAR(36)   NOT NULL,
    data_criacao     TIMESTAMP     NOT NULL,
    data_atualizacao TIMESTAMP,
    reserva_id       VARCHAR(36)   NOT NULL,
    provedor_id      VARCHAR(64)   NOT NULL,
    metodo           VARCHAR(20)   NOT NULL,
    valor            NUMERIC(8, 2) NOT NULL,
    status           VARCHAR(12)   NOT NULL,
    url_pagamento    VARCHAR(255)  NOT NULL,
    CONSTRAINT pk_pagamento PRIMARY KEY (id),
    CONSTRAINT uk_pagamento_provedor UNIQUE (provedor_id),
    CONSTRAINT fk_pagamento_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id),
    CONSTRAINT ck_pagamento_status CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'REEMBOLSADO'))
);

-- Nunca duas cobranças pendentes para a mesma reserva.
CREATE UNIQUE INDEX uk_pagamento_pendente ON pagamento (reserva_id) WHERE status = 'PENDENTE';
