package com.spacebus.ops.domain.model.localizacao;

import com.spacebus.ops.domain.entity.enums.StatusViagem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Viagem como o motorista vê em "Minhas viagens". */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViagemMotoristaResponse {
    private String id;
    private String rotaNome;
    private String origem;
    private String destino;
    private LocalDateTime partida;
    private String veiculoPlaca;
    private String veiculoDescricao;
    private StatusViagem status;
    private LocalDateTime iniciadaEm;
    /** Pode iniciar agora (publicada e dentro da janela). */
    private boolean podeIniciar;
    /** Por que não pode iniciar (publicada fora da janela); nulo se pode ou se a situação já explica. */
    private String motivo;
    /** Hora da última posição gravada desta viagem (retomar a tela depois de recarregar). */
    private LocalDateTime ultimaPosicaoEm;
}
