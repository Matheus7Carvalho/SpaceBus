package com.spacebus.ops.domain.service.busca;

import com.spacebus.ops.domain.model.busca.BuscaViagemRequest;
import com.spacebus.ops.domain.model.busca.DetalheViagemResponse;
import com.spacebus.ops.domain.model.busca.ViagemEncontradaResponse;

import java.util.List;

/** Consulta de viagens pelo passageiro: só viagens publicadas com partida futura. */
public interface IBuscaViagemService {

    List<ViagemEncontradaResponse> buscar(BuscaViagemRequest busca);

    DetalheViagemResponse detalhar(String id);

    /** Nomes de locais atendidos (pontos de rotas com viagem publicada futura), para sugestão nos campos. */
    List<String> locais();
}
