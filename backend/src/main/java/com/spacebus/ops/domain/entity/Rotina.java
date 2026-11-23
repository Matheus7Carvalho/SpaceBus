package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.FrequenciaRotina;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/** Programação de uma Rota Fixa: em que dias e horários as viagens são geradas, e com quais dados. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ROTINA")
public class Rotina extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ROTA_ID", nullable = false)
    private Rota rota;

    @Enumerated(EnumType.STRING)
    @Column(name = "FREQUENCIA", nullable = false, length = 7)
    private FrequenciaRotina frequencia;

    /** Só para SEMANAL; vazio na DIARIA. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ROTINA_DIA", joinColumns = @JoinColumn(name = "ROTINA_ID"))
    @Enumerated(EnumType.STRING)
    @Column(name = "DIA", nullable = false, length = 9)
    private Set<DayOfWeek> dias = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ROTINA_HORARIO", joinColumns = @JoinColumn(name = "ROTINA_ID"))
    @Column(name = "HORARIO", nullable = false, columnDefinition = "TIME")
    private Set<LocalTime> horarios = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "VEICULO_ID", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MOTORISTA_ID")
    private Usuario motorista;

    @Column(name = "PRECO", nullable = false, precision = 6, scale = 2)
    private BigDecimal preco;

    @Column(name = "CAPACIDADE", nullable = false)
    private Integer capacidade;

    @Column(name = "INICIO", nullable = false)
    private LocalDate inicio;

    /** Sem fim: vale até a rota deixar de ser fixa. */
    @Column(name = "FIM")
    private LocalDate fim;

    /** Viagens geradas já nascem publicadas (senão, em rascunho). */
    @Column(name = "PUBLICAR", nullable = false)
    private Boolean publicar;

    @Column(name = "GERADA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime geradaEm;

    /** Viagens criadas na última geração. */
    @Column(name = "GERADAS")
    private Integer geradas;

    /** Horários pulados na última geração porque o veículo já tinha viagem. */
    @Column(name = "CONFLITOS")
    private Integer conflitos;

    /** A rotina vale para o dia (vigência e dia da semana). */
    public boolean valeEm(LocalDate dia) {
        if (dia.isBefore(inicio) || (fim != null && dia.isAfter(fim))) return false;
        return frequencia == FrequenciaRotina.DIARIA || dias.contains(dia.getDayOfWeek());
    }

    /** Mesmos dias, horários, veículo, motorista, preço, capacidade, vigência e publicação. */
    public boolean mesmaProgramacao(Rotina outra) {
        return frequencia == outra.frequencia
                && dias.equals(outra.dias)
                && horarios.equals(outra.horarios)
                && java.util.Objects.equals(veiculo.getId(), outra.veiculo.getId())
                && java.util.Objects.equals(motorista == null ? null : motorista.getId(), outra.motorista == null ? null : outra.motorista.getId())
                && preco.compareTo(outra.preco) == 0
                && capacidade.equals(outra.capacidade)
                && inicio.equals(outra.inicio)
                && java.util.Objects.equals(fim, outra.fim)
                && java.util.Objects.equals(publicar, outra.publicar);
    }

    /** Copia a programação de outra rotina, mantendo id, rota e o resultado da última geração. */
    public void copiarProgramacao(Rotina outra) {
        frequencia = outra.frequencia;
        dias.clear();
        dias.addAll(outra.dias);
        horarios.clear();
        horarios.addAll(outra.horarios);
        veiculo = outra.veiculo;
        motorista = outra.motorista;
        preco = outra.preco;
        capacidade = outra.capacidade;
        inicio = outra.inicio;
        fim = outra.fim;
        publicar = outra.publicar;
    }
}
