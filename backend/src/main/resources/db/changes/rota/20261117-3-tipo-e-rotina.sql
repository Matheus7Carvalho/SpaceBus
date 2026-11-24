--liquibase formatted sql

-- Rota Única (viagens programadas uma a uma) ou Rota Fixa (rotina que gera as viagens).
--changeset spacebus:20261117-3-tipo-e-rotina
ALTER TABLE rota ADD COLUMN tipo VARCHAR(5) NOT NULL DEFAULT 'UNICA';
ALTER TABLE rota ADD CONSTRAINT ck_rota_tipo CHECK (tipo IN ('UNICA', 'FIXA'));

CREATE TABLE rotina
(
    id               VARCHAR(36)   NOT NULL,
    data_criacao     TIMESTAMP     NOT NULL,
    data_atualizacao TIMESTAMP,
    rota_id          VARCHAR(36)   NOT NULL,
    frequencia       VARCHAR(7)    NOT NULL,
    veiculo_id       VARCHAR(36)   NOT NULL,
    motorista_id     VARCHAR(36),
    preco            NUMERIC(6, 2) NOT NULL,
    capacidade       INTEGER       NOT NULL,
    inicio           DATE          NOT NULL,
    fim              DATE,
    publicar         BOOLEAN       NOT NULL DEFAULT FALSE,
    gerada_em        TIMESTAMP,
    geradas          INTEGER,
    conflitos        INTEGER,
    CONSTRAINT pk_rotina PRIMARY KEY (id),
    CONSTRAINT uk_rotina_rota UNIQUE (rota_id),
    CONSTRAINT fk_rotina_rota FOREIGN KEY (rota_id) REFERENCES rota (id) ON DELETE CASCADE,
    CONSTRAINT fk_rotina_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo (id),
    CONSTRAINT fk_rotina_motorista FOREIGN KEY (motorista_id) REFERENCES usuario (id),
    CONSTRAINT ck_rotina_frequencia CHECK (frequencia IN ('DIARIA', 'SEMANAL')),
    CONSTRAINT ck_rotina_preco CHECK (preco > 0),
    CONSTRAINT ck_rotina_capacidade CHECK (capacidade BETWEEN 1 AND 60),
    CONSTRAINT ck_rotina_vigencia CHECK (fim IS NULL OR fim >= inicio)
);

CREATE TABLE rotina_dia
(
    rotina_id VARCHAR(36) NOT NULL,
    dia       VARCHAR(9)  NOT NULL,
    CONSTRAINT pk_rotina_dia PRIMARY KEY (rotina_id, dia),
    CONSTRAINT fk_rotina_dia_rotina FOREIGN KEY (rotina_id) REFERENCES rotina (id) ON DELETE CASCADE
);

CREATE TABLE rotina_horario
(
    rotina_id VARCHAR(36) NOT NULL,
    horario   TIME        NOT NULL,
    CONSTRAINT pk_rotina_horario PRIMARY KEY (rotina_id, horario),
    CONSTRAINT fk_rotina_horario_rotina FOREIGN KEY (rotina_id) REFERENCES rotina (id) ON DELETE CASCADE
);

-- Viagem gerada por rotina: o mesmo horário da mesma rotina nunca é gerado duas vezes (nem depois de cancelado).
ALTER TABLE viagem ADD COLUMN rotina_id VARCHAR(36);
ALTER TABLE viagem ADD CONSTRAINT fk_viagem_rotina FOREIGN KEY (rotina_id) REFERENCES rotina (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX uk_viagem_rotina_partida ON viagem (rotina_id, data_hora_partida) WHERE rotina_id IS NOT NULL;
