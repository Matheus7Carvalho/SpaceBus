package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.TipoNotificacao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aviso ao passageiro sobre uma reserva. Criada só por insert condicional na chave única
 * ({@code NotificacaoRepository.inserirSeNova}): o mesmo evento repetido não duplica.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "NOTIFICACAO")
public class Notificacao extends BaseEntity {

    @Column(name = "USUARIO_ID", nullable = false, length = 36, updatable = false)
    private String usuarioId;

    @Column(name = "RESERVA_ID", nullable = false, length = 36, updatable = false)
    private String reservaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20, updatable = false)
    private TipoNotificacao tipo;

    @Column(name = "TITULO", nullable = false, length = 120, updatable = false)
    private String titulo;

    @Column(name = "MENSAGEM", nullable = false, length = 300, updatable = false)
    private String mensagem;

    @Column(name = "LINK", nullable = false, length = 200, updatable = false)
    private String link;

    @Column(name = "CHAVE", nullable = false, length = 120, updatable = false)
    private String chave;

    @Column(name = "LIDA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime lidaEm;

    @OneToMany(mappedBy = "notificacao", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @org.hibernate.annotations.BatchSize(size = 50)
    @OrderBy("canal")
    private List<NotificacaoEnvio> envios = new ArrayList<>();
}
