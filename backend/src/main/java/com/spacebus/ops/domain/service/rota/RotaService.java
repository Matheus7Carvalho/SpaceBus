package com.spacebus.ops.domain.service.rota;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.core.service.BaseService;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.model.rota.RotaRequest;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class RotaService extends BaseService<Rota> implements IRotaService {

    private final ViagemRepository viagemRepository;

    protected RotaService(final RotaRepository repository, final ViagemRepository viagemRepository) {
        super(repository);
        this.viagemRepository = viagemRepository;
    }

    @Override
    @Transactional
    public Rota create(Rota rota) throws DomainException {
        normalizar(rota);
        return super.create(rota);
    }

    /** Substitui dados e pontos. Pontos removidos são apagados (orphanRemoval). */
    @Override
    @Transactional
    public Rota update(Rota alterada, String id) throws DomainException {
        var existente = getRepository().findById(id).orElseThrow(() -> new EntityNotFoundException("Item não encontrado."));
        // Valida antes de alterar a entidade gerenciada (o flush da consulta de unicidade gravaria o nome repetido).
        alterada.setId(id);
        normalizar(alterada);
        validate(alterada);
        existente.setNome(alterada.getNome());
        existente.setOrigem(alterada.getOrigem());
        existente.setDestino(alterada.getDestino());
        existente.substituirPontos(new ArrayList<>(alterada.getPontos()));
        normalizar(existente);
        validate(existente);
        return getRepository().save(existente);
    }

    @Override
    public void validate(Rota rota) throws DomainException {
        var pontos = rota.getPontos().size();
        if (pontos < RotaRequest.MIN_PONTOS || pontos > RotaRequest.MAX_PONTOS) {
            throw new DomainException("A rota deve ter de %d a %d pontos de parada.", RotaRequest.MIN_PONTOS, RotaRequest.MAX_PONTOS);
        }
        if (getRepository().existeNome(rota.getNome(), rota.getId())) {
            throw new DomainException("Já existe uma rota com o nome %s.", rota.getNome());
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (viagemRepository.existsByRotaId(id)) {
            throw new DomainException("Rota com viagens não pode ser excluída.");
        }
        super.delete(id);
    }

    /** Ordem segue a posição na lista; referência vazia vira nula. */
    private void normalizar(Rota rota) {
        rota.setNome(aparar(rota.getNome()));
        rota.setOrigem(aparar(rota.getOrigem()));
        rota.setDestino(aparar(rota.getDestino()));
        var ordem = 1;
        for (PontoParada ponto : rota.getPontos()) {
            ponto.setOrdem(ordem++);
            ponto.setNome(aparar(ponto.getNome()));
            var referencia = aparar(ponto.getReferencia());
            ponto.setReferencia(referencia == null || referencia.isEmpty() ? null : referencia);
        }
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }

    @Override
    public RotaRepository getRepository() {
        return (RotaRepository) super.getRepository();
    }
}
