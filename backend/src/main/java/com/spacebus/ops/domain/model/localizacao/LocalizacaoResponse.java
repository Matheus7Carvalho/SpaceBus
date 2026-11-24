package com.spacebus.ops.domain.model.localizacao;

import com.spacebus.ops.domain.entity.enums.StatusViagem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Localização da van como o passageiro vê. Sem motorista nem passageiros. Posição só com a viagem em andamento;
 * "desatualizada" é decidida aqui, pelo relógio do servidor.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizacaoResponse {
    private StatusViagem situacaoViagem;
    /** Publicada, dentro da janela de início: "a localização aparece quando a viagem começar". */
    private boolean aguardandoInicio;
    /** Há posição para mostrar (viagem em andamento com ao menos uma posição). */
    private boolean disponivel;
    private Double latitude;
    private Double longitude;
    private Double precisaoM;
    private LocalDateTime registradaEm;
    private Long segundosDesdeAtualizacao;
    private boolean desatualizada;
    private boolean simulada;
}
