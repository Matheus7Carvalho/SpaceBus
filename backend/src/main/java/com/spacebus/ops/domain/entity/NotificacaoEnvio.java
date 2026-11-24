package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import com.spacebus.ops.domain.entity.enums.StatusEnvio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Resultado de um envio externo (e-mail, WhatsApp). Motivo da falha sem dados pessoais. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "NOTIFICACAO_ENVIO")
public class NotificacaoEnvio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "NOTIFICACAO_ID", nullable = false, updatable = false)
    private Notificacao notificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "CANAL", nullable = false, length = 10, updatable = false)
    private CanalEnvio canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10, updatable = false)
    private StatusEnvio status;

    @Column(name = "MOTIVO", length = 200, updatable = false)
    private String motivo;
}
