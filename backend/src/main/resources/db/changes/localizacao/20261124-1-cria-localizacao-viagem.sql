--liquibase formatted sql

-- Última posição da viagem (sem trajeto) e a qualidade do sinal. Coordenadas apagadas após a retenção.
--changeset spacebus:20261124-1-cria-localizacao-viagem
CREATE TABLE localizacao_viagem
(
    id                     VARCHAR(36)      NOT NULL,
    data_criacao           TIMESTAMP        NOT NULL,
    data_atualizacao       TIMESTAMP,
    viagem_id              VARCHAR(36)      NOT NULL,
    latitude               DOUBLE PRECISION,
    longitude              DOUBLE PRECISION,
    precisao_m             DOUBLE PRECISION,
    registrada_em          TIMESTAMP,
    recebida_em            TIMESTAMP,
    simulada               BOOLEAN          NOT NULL DEFAULT FALSE,
    aceitas                INTEGER          NOT NULL DEFAULT 0,
    rejeitadas_adiantada   INTEGER          NOT NULL DEFAULT 0,
    rejeitadas_antiga      INTEGER          NOT NULL DEFAULT 0,
    rejeitadas_imprecisa   INTEGER          NOT NULL DEFAULT 0,
    ignoradas              INTEGER          NOT NULL DEFAULT 0,
    lacunas                INTEGER          NOT NULL DEFAULT 0,
    maior_intervalo_s      INTEGER          NOT NULL DEFAULT 0,
    ultimo_motivo_rejeicao VARCHAR(20),
    ultima_rejeicao_em     TIMESTAMP,
    expurgada_em           TIMESTAMP,
    CONSTRAINT pk_localizacao_viagem PRIMARY KEY (id),
    CONSTRAINT uk_localizacao_viagem_viagem UNIQUE (viagem_id),
    CONSTRAINT fk_localizacao_viagem_viagem FOREIGN KEY (viagem_id) REFERENCES viagem (id),
    CONSTRAINT ck_localizacao_viagem_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_localizacao_viagem_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_localizacao_viagem_precisao CHECK (precisao_m BETWEEN 0 AND 5000),
    CONSTRAINT ck_localizacao_viagem_motivo CHECK (ultimo_motivo_rejeicao IN ('ADIANTADA', 'ANTIGA', 'IMPRECISA'))
);
