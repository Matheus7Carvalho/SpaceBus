package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.MotivoRejeicaoPosicao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Última posição de uma viagem (uma linha por viagem, sem trajeto) e a qualidade do sinal.
 * Gravada só por SQL condicional ({@code LocalizacaoViagemRepository}): a posição mais antiga nunca sobrescreve
 * a mais nova. As coordenadas são apagadas depois da retenção; contadores e horários ficam.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "LOCALIZACAO_VIAGEM")
public class LocalizacaoViagem extends BaseEntity {

    @Column(name = "VIAGEM_ID", nullable = false, length = 36, updatable = false)
    private String viagemId;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    @Column(name = "PRECISAO_M")
    private Double precisaoM;

    /** Hora da posição no aparelho, no fuso da aplicação. */
    @Column(name = "REGISTRADA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime registradaEm;

    /** Hora em que o servidor recebeu a posição. */
    @Column(name = "RECEBIDA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime recebidaEm;

    @Column(name = "SIMULADA", nullable = false)
    private boolean simulada;

    @Column(name = "ACEITAS", nullable = false)
    private int aceitas;

    @Column(name = "REJEITADAS_ADIANTADA", nullable = false)
    private int rejeitadasAdiantada;

    @Column(name = "REJEITADAS_ANTIGA", nullable = false)
    private int rejeitadasAntiga;

    @Column(name = "REJEITADAS_IMPRECISA", nullable = false)
    private int rejeitadasImprecisa;

    @Column(name = "IGNORADAS", nullable = false)
    private int ignoradas;

    /** Intervalos entre posições aceitas acima do limite de lacuna. */
    @Column(name = "LACUNAS", nullable = false)
    private int lacunas;

    @Column(name = "MAIOR_INTERVALO_S", nullable = false)
    private int maiorIntervaloSegundos;

    @Enumerated(EnumType.STRING)
    @Column(name = "ULTIMO_MOTIVO_REJEICAO", length = 20)
    private MotivoRejeicaoPosicao ultimoMotivoRejeicao;

    @Column(name = "ULTIMA_REJEICAO_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime ultimaRejeicaoEm;

    /** Quando as coordenadas foram apagadas (retenção). */
    @Column(name = "EXPURGADA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime expurgadaEm;

    public boolean temPosicao() {
        return latitude != null && longitude != null && registradaEm != null;
    }
}
