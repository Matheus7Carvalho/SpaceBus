--liquibase formatted sql

--changeset spacebus:20261117-1-cria-viagem
CREATE TABLE viagem
(
    id                VARCHAR(36)   NOT NULL,
    data_criacao      TIMESTAMP     NOT NULL,
    data_atualizacao  TIMESTAMP,
    rota_id           VARCHAR(36)   NOT NULL,
    veiculo_id        VARCHAR(36)   NOT NULL,
    motorista_id      VARCHAR(36),
    data_hora_partida TIMESTAMP     NOT NULL,
    preco             NUMERIC(6, 2) NOT NULL,
    capacidade        INTEGER       NOT NULL,
    status            VARCHAR(10)   NOT NULL,
    CONSTRAINT pk_viagem PRIMARY KEY (id),
    CONSTRAINT fk_viagem_rota FOREIGN KEY (rota_id) REFERENCES rota (id),
    CONSTRAINT fk_viagem_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo (id),
    CONSTRAINT fk_viagem_motorista FOREIGN KEY (motorista_id) REFERENCES usuario (id),
    CONSTRAINT ck_viagem_status CHECK (status IN ('RASCUNHO', 'PUBLICADA', 'SUSPENSA', 'CANCELADA')),
    CONSTRAINT ck_viagem_preco CHECK (preco > 0),
    CONSTRAINT ck_viagem_capacidade CHECK (capacidade BETWEEN 1 AND 60)
);

CREATE INDEX ix_viagem_partida ON viagem (data_hora_partida);
CREATE INDEX ix_viagem_veiculo ON viagem (veiculo_id, data_hora_partida);
