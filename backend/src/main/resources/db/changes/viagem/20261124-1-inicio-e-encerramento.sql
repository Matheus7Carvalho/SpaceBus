--liquibase formatted sql

-- Módulo 4: a viagem é iniciada (EM_ANDAMENTO) e encerrada (ENCERRADA), com hora e responsável.
--changeset spacebus:20261124-1-inicio-e-encerramento
ALTER TABLE viagem ALTER COLUMN status TYPE VARCHAR(12);
ALTER TABLE viagem DROP CONSTRAINT ck_viagem_status;
ALTER TABLE viagem ADD CONSTRAINT ck_viagem_status
    CHECK (status IN ('RASCUNHO', 'PUBLICADA', 'SUSPENSA', 'CANCELADA', 'EM_ANDAMENTO', 'ENCERRADA'));

ALTER TABLE viagem ADD COLUMN iniciada_em TIMESTAMP;
ALTER TABLE viagem ADD COLUMN iniciada_por VARCHAR(36);
ALTER TABLE viagem ADD COLUMN encerrada_em TIMESTAMP;
ALTER TABLE viagem ADD COLUMN encerrada_por VARCHAR(36);
ALTER TABLE viagem ADD CONSTRAINT fk_viagem_iniciada_por FOREIGN KEY (iniciada_por) REFERENCES usuario (id);
ALTER TABLE viagem ADD CONSTRAINT fk_viagem_encerrada_por FOREIGN KEY (encerrada_por) REFERENCES usuario (id);

-- Um motorista e um veículo têm no máximo uma viagem em andamento, mesmo com inícios simultâneos.
CREATE UNIQUE INDEX uk_viagem_motorista_em_andamento ON viagem (motorista_id) WHERE status = 'EM_ANDAMENTO';
CREATE UNIQUE INDEX uk_viagem_veiculo_em_andamento ON viagem (veiculo_id) WHERE status = 'EM_ANDAMENTO';
CREATE INDEX ix_viagem_em_andamento ON viagem (iniciada_em) WHERE status = 'EM_ANDAMENTO';
