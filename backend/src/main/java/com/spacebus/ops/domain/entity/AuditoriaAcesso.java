package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.Perfil;
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

/** Quem acessou dados pessoais, com qual perfil, o quê e quando (data = dataCriacao). Só inserção. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AUDITORIA_ACESSO")
public class AuditoriaAcesso extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false, updatable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 20, updatable = false)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACAO", nullable = false, length = 40, updatable = false)
    private AcaoAuditada acao;

    @Column(name = "RECURSO_ID", nullable = false, length = 36, updatable = false)
    private String recursoId;
}
