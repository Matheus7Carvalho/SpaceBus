--liquibase formatted sql

--changeset spacebus:20261117-1-cria-veiculo
CREATE TABLE veiculo
(
    id               VARCHAR(36)  NOT NULL,
    data_criacao     TIMESTAMP    NOT NULL,
    data_atualizacao TIMESTAMP,
    placa            VARCHAR(7)   NOT NULL,
    descricao        VARCHAR(100) NOT NULL,
    tipo             VARCHAR(10)  NOT NULL,
    capacidade       INTEGER      NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_veiculo PRIMARY KEY (id),
    CONSTRAINT uk_veiculo_placa UNIQUE (placa),
    CONSTRAINT ck_veiculo_tipo CHECK (tipo IN ('VAN', 'ONIBUS')),
    CONSTRAINT ck_veiculo_capacidade CHECK (capacidade BETWEEN 1 AND 60)
);
