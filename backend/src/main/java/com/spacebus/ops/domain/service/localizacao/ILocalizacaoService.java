package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoOperacaoResponse;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoResponse;
import com.spacebus.ops.domain.model.localizacao.PosicaoRequest;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Localização da viagem ativa (escopo 4.2): só a última posição, enviada pelo celular do motorista.
 * Ordem de travas: viagem (FOR SHARE) → localização. Nenhuma posição é gravada depois do encerramento.
 */
public interface ILocalizacaoService {

    /**
     * Lote do motorista atribuído, com a viagem em andamento. Cada posição é aceita, rejeitada (adiantada, antiga,
     * imprecisa) ou ignorada (fora de ordem, frequente); o lote não falha por uma posição ruim.
     */
    ResultadoEnvioResponse registrar(String viagemId, String motoristaId, List<PosicaoRequest> posicoes);

    /** Posição simulada (demonstração e testes): mesmo caminho do motorista, sem conferir o motorista. */
    ResultadoEnvioResponse registrarSimulada(String viagemId, PosicaoRequest posicao);

    /** Localização da viagem de uma reserva confirmada do próprio passageiro. Outra reserva: não encontrada. */
    LocalizacaoResponse daReserva(String reservaId, String passageiroId);

    /** Localização e diagnóstico para a operação. Consulta do Administrador fica registrada (no máximo uma vez por hora). */
    LocalizacaoOperacaoResponse daViagem(String viagemId, String usuarioId, Perfil perfil);

    /** Viagens em andamento (entre as informadas) com localização desatualizada ou indisponível. */
    Set<String> semSinal(Collection<Viagem> viagens);

    /** Hora da última posição de cada viagem que tem posição (id → hora). */
    Map<String, LocalDateTime> ultimasPosicoes(Collection<String> viagemIds);

    /** Apaga as coordenadas das viagens encerradas há mais que a retenção. Retorna quantas. */
    int expurgar();
}
