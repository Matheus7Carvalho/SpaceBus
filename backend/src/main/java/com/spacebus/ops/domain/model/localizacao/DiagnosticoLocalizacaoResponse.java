package com.spacebus.ops.domain.model.localizacao;

import com.spacebus.ops.domain.entity.enums.MotivoRejeicaoPosicao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Qualidade do sinal da viagem, para a operação agir quando a localização falhar. Sem coordenadas. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticoLocalizacaoResponse {
    private LocalDateTime iniciadaEm;
    private LocalDateTime encerradaEm;
    /** Hora em que o servidor recebeu a última posição aceita. */
    private LocalDateTime ultimaRecebidaEm;
    private int aceitas;
    private int rejeitadasAdiantada;
    private int rejeitadasAntiga;
    private int rejeitadasImprecisa;
    private int ignoradas;
    private int lacunas;
    private int maiorIntervaloSegundos;
    private MotivoRejeicaoPosicao ultimoMotivoRejeicao;
    private LocalDateTime ultimaRejeicaoEm;
}
