package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.localizacao.ViagemMotoristaResponse;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** As transições passam por {@link IViagemService} (mesma trava e regras da operação), conferindo o motorista. */
@Service
public class ViagensMotoristaService implements IViagensMotoristaService {

    private final IViagemService viagens;
    private final ViagemRepository viagemRepository;
    private final ILocalizacaoService localizacao;
    private final Clock clock;

    public ViagensMotoristaService(IViagemService viagens, ViagemRepository viagemRepository, ILocalizacaoService localizacao, Clock clock) {
        this.viagens = viagens;
        this.viagemRepository = viagemRepository;
        this.localizacao = localizacao;
        this.clock = clock;
    }

    @Override
    public List<ViagemMotoristaResponse> deHoje(String motoristaId) {
        var hoje = LocalDate.now(clock).atStartOfDay();
        var lista = viagemRepository.doMotorista(motoristaId, hoje, hoje.plusDays(1), StatusViagem.RASCUNHO, StatusViagem.EM_ANDAMENTO);
        var ultimas = localizacao.ultimasPosicoes(lista.stream().map(Viagem::getId).toList());
        return lista.stream().map(v -> resposta(v, ultimas)).toList();
    }

    @Override
    public ViagemMotoristaResponse iniciar(String viagemId, String motoristaId) {
        return resposta(viagens.iniciar(viagemId, motoristaId, motoristaId), Map.of());
    }

    @Override
    public ViagemMotoristaResponse encerrar(String viagemId, String motoristaId) {
        return resposta(viagens.encerrar(viagemId, motoristaId, motoristaId), Map.of());
    }

    /** O motivo só aparece para viagem publicada fora da janela; nas outras situações, o selo já explica. */
    private ViagemMotoristaResponse resposta(Viagem v, Map<String, LocalDateTime> ultimas) {
        var impedimento = viagens.impedimentoParaIniciar(v);
        return ViagemMotoristaResponse.builder()
                .id(v.getId())
                .rotaNome(v.getRota().getNome())
                .origem(v.getRota().getOrigem())
                .destino(v.getRota().getDestino())
                .partida(v.getPartida())
                .veiculoPlaca(v.getVeiculo().getPlaca())
                .veiculoDescricao(v.getVeiculo().getDescricao())
                .status(v.getStatus())
                .iniciadaEm(v.getIniciadaEm())
                .podeIniciar(impedimento.isEmpty())
                .motivo(v.getStatus() == StatusViagem.PUBLICADA ? impedimento.orElse(null) : null)
                .ultimaPosicaoEm(ultimas.get(v.getId()))
                .build();
    }
}
