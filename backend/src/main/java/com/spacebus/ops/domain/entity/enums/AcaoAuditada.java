package com.spacebus.ops.domain.entity.enums;

/** Acesso a dados pessoais que fica registrado. */
public enum AcaoAuditada {
    CONSULTA_PASSAGEIROS_VIAGEM,
    /** Administrador (equipe Space Bus) consultou a posição da van; registrado no máximo uma vez por hora. */
    CONSULTA_LOCALIZACAO_VIAGEM
}
