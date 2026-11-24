package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "USUARIO")
public class Usuario extends BaseEntity {

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "EMAIL", nullable = false, length = 150)
    private String email;

    /** Hash BCrypt. Nunca exposto em Response. */
    @Column(name = "SENHA", nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo;

    /** Opt-in do WhatsApp (passageiro); nulo = sem consentimento. */
    @Column(name = "WHATSAPP_CONSENTIDO_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime whatsappConsentidoEm;

    public Usuario(String nome, String email, String senha, Perfil perfil, Boolean ativo) {
        this(nome, email, senha, perfil, ativo, null);
    }
}
