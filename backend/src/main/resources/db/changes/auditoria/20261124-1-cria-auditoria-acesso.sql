--liquibase formatted sql

-- Acesso a dados pessoais (lista de passageiros) gravado no banco, não só no log (escopo, seção 3; ressalva N7 do módulo 3).
--changeset spacebus:20261124-1-cria-auditoria-acesso
CREATE TABLE auditoria_acesso
(
    id               VARCHAR(36) NOT NULL,
    data_criacao     TIMESTAMP   NOT NULL,
    data_atualizacao TIMESTAMP,
    usuario_id       VARCHAR(36) NOT NULL,
    perfil           VARCHAR(20) NOT NULL,
    acao             VARCHAR(40) NOT NULL,
    recurso_id       VARCHAR(36) NOT NULL,
    CONSTRAINT pk_auditoria_acesso PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_acesso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_auditoria_acesso_acao CHECK (acao IN ('CONSULTA_PASSAGEIROS_VIAGEM'))
);

CREATE INDEX ix_auditoria_acesso_recurso ON auditoria_acesso (recurso_id, data_criacao);
CREATE INDEX ix_auditoria_acesso_usuario ON auditoria_acesso (usuario_id, data_criacao);
