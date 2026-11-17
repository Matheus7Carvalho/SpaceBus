--liquibase formatted sql

--changeset spacebus:20261117-1-cria-rota
CREATE TABLE rota
(
    id               VARCHAR(36)  NOT NULL,
    data_criacao     TIMESTAMP    NOT NULL,
    data_atualizacao TIMESTAMP,
    nome             VARCHAR(100) NOT NULL,
    origem           VARCHAR(100) NOT NULL,
    destino          VARCHAR(100) NOT NULL,
    CONSTRAINT pk_rota PRIMARY KEY (id),
    CONSTRAINT uk_rota_nome UNIQUE (nome)
);

CREATE TABLE ponto_parada
(
    id               VARCHAR(36)  NOT NULL,
    data_criacao     TIMESTAMP    NOT NULL,
    data_atualizacao TIMESTAMP,
    rota_id          VARCHAR(36)  NOT NULL,
    ordem            INTEGER      NOT NULL,
    nome             VARCHAR(100) NOT NULL,
    referencia       VARCHAR(150),
    CONSTRAINT pk_ponto_parada PRIMARY KEY (id),
    CONSTRAINT fk_ponto_parada_rota FOREIGN KEY (rota_id) REFERENCES rota (id) ON DELETE CASCADE
);

CREATE INDEX ix_ponto_parada_rota ON ponto_parada (rota_id, ordem);
