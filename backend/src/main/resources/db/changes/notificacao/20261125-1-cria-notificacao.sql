--liquibase formatted sql

-- Módulo 5: notificações na aplicação, envios simulados (e-mail, WhatsApp) e consentimento do WhatsApp.
--changeset spacebus:20261125-1-cria-notificacao
ALTER TABLE usuario ADD COLUMN whatsapp_consentido_em TIMESTAMP;

CREATE TABLE notificacao
(
    id               VARCHAR(36)  NOT NULL,
    data_criacao     TIMESTAMP    NOT NULL,
    data_atualizacao TIMESTAMP,
    usuario_id       VARCHAR(36)  NOT NULL,
    reserva_id       VARCHAR(36)  NOT NULL,
    tipo             VARCHAR(20)  NOT NULL,
    titulo           VARCHAR(120) NOT NULL,
    mensagem         VARCHAR(300) NOT NULL,
    link             VARCHAR(200) NOT NULL,
    chave            VARCHAR(120) NOT NULL,
    lida_em          TIMESTAMP,
    CONSTRAINT pk_notificacao PRIMARY KEY (id),
    CONSTRAINT uk_notificacao_chave UNIQUE (chave),
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_notificacao_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id),
    CONSTRAINT ck_notificacao_tipo CHECK (tipo IN ('RESERVA_CONFIRMADA', 'VIAGEM_ALTERADA', 'VIAGEM_CANCELADA', 'VIAGEM_INICIADA', 'LEMBRETE'))
);
CREATE INDEX ix_notificacao_usuario ON notificacao (usuario_id, lida_em);
CREATE INDEX ix_notificacao_usuario_data ON notificacao (usuario_id, data_criacao);

CREATE TABLE notificacao_envio
(
    id               VARCHAR(36)  NOT NULL,
    data_criacao     TIMESTAMP    NOT NULL,
    data_atualizacao TIMESTAMP,
    notificacao_id   VARCHAR(36)  NOT NULL,
    canal            VARCHAR(10)  NOT NULL,
    status           VARCHAR(10)  NOT NULL,
    motivo           VARCHAR(200),
    CONSTRAINT pk_notificacao_envio PRIMARY KEY (id),
    CONSTRAINT uk_notificacao_envio_canal UNIQUE (notificacao_id, canal),
    CONSTRAINT fk_notificacao_envio_notificacao FOREIGN KEY (notificacao_id) REFERENCES notificacao (id),
    CONSTRAINT ck_notificacao_envio_canal CHECK (canal IN ('EMAIL', 'WHATSAPP')),
    CONSTRAINT ck_notificacao_envio_status CHECK (status IN ('ENVIADA', 'FALHOU'))
);
