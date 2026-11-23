package com.spacebus.ops.domain.service.rota;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.core.service.BaseService;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Rotina;
import com.spacebus.ops.domain.entity.enums.FrequenciaRotina;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.TipoRota;
import com.spacebus.ops.domain.model.rota.RotaRequest;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.rotina.GeradorViagens;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class RotaService extends BaseService<Rota> implements IRotaService {

    private final ViagemRepository viagemRepository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeradorViagens gerador;

    protected RotaService(final RotaRepository repository, final ViagemRepository viagemRepository,
                          final VeiculoRepository veiculoRepository, final UsuarioRepository usuarioRepository,
                          final GeradorViagens gerador) {
        super(repository);
        this.viagemRepository = viagemRepository;
        this.veiculoRepository = veiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.gerador = gerador;
    }

    /** Rota Fixa: grava e já gera as viagens do horizonte. */
    @Override
    @Transactional
    public Rota create(Rota rota) throws DomainException {
        normalizar(rota);
        prepararRotina(rota);
        var salva = super.create(rota);
        if (salva.getRotina() != null) gerador.gerar(salva.getRotina());
        return salva;
    }

    /**
     * Substitui dados, pontos, tipo e rotina. Pontos removidos são apagados (orphanRemoval).
     * Rotina alterada refaz seus rascunhos futuros; rota que deixa de ser fixa perde a rotina e esses rascunhos.
     */
    @Override
    @Transactional
    public Rota update(Rota alterada, String id) throws DomainException {
        var existente = getRepository().findById(id).orElseThrow(() -> new EntityNotFoundException("Item não encontrado."));
        // Valida antes de alterar a entidade gerenciada (o flush da consulta de unicidade gravaria o nome repetido).
        alterada.setId(id);
        normalizar(alterada);
        prepararRotina(alterada);
        validate(alterada);

        existente.setNome(alterada.getNome());
        existente.setOrigem(alterada.getOrigem());
        existente.setDestino(alterada.getDestino());
        existente.substituirPontos(new ArrayList<>(alterada.getPontos()));
        existente.setTipo(alterada.getTipo());
        var programacaoMudou = trocarRotina(existente, alterada.getRotina());
        normalizar(existente);
        validate(existente);

        var salva = getRepository().saveAndFlush(existente);
        if (salva.getRotina() != null) {
            // Editar só nome ou pontos não refaz os rascunhos (preserva ajustes feitos neles); só completa o horizonte.
            if (programacaoMudou) gerador.regenerar(salva.getRotina());
            else gerador.gerar(salva.getRotina());
        }
        return salva;
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

    /** Mantém o id da rotina existente (as viagens geradas apontam para ela). Retorna se a programação mudou. */
    private boolean trocarRotina(Rota existente, Rotina nova) {
        var atual = existente.getRotina();
        if (nova == null) {
            if (atual != null) {
                gerador.descartarRascunhosFuturos(atual);
                existente.definirRotina(null);
            }
            return false;
        }
        if (atual == null) {
            existente.definirRotina(nova);
            return true;
        }
        if (atual.mesmaProgramacao(nova)) return false;
        atual.copiarProgramacao(nova);
        return true;
    }

    /**
     * Rota Única não tem rotina; Rota Fixa exige uma, com as mesmas regras da viagem
     * (veículo ativo, capacidade até a do veículo, motorista ativo) e vigência coerente.
     */
    private void prepararRotina(Rota rota) {
        if (rota.getTipo() != TipoRota.FIXA) {
            rota.definirRotina(null);
            return;
        }
        var rotina = rota.getRotina();
        if (rotina == null) throw new DomainException("Defina a rotina da rota fixa.");
        if (rotina.getFrequencia() == FrequenciaRotina.SEMANAL && rotina.getDias().isEmpty()) {
            throw new DomainException("Escolha ao menos um dia da semana.");
        }
        if (rotina.getFrequencia() == FrequenciaRotina.DIARIA) rotina.getDias().clear();
        if (rotina.getHorarios().isEmpty()) throw new DomainException("Adicione ao menos um horário.");
        if (rotina.getFim() != null && rotina.getFim().isBefore(rotina.getInicio())) {
            throw new DomainException("O fim da vigência não pode ser antes do início.");
        }
        if (rotina.getPublicar() == null) rotina.setPublicar(Boolean.FALSE);

        var veiculoId = rotina.getVeiculo() == null ? null : rotina.getVeiculo().getId();
        var veiculo = veiculoId == null ? null : veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new DomainException("Veículo não encontrado."));
        if (veiculo == null) throw new DomainException("Selecione o veículo.");
        if (!Boolean.TRUE.equals(veiculo.getAtivo())) throw new DomainException("O veículo %s está inativo.", veiculo.getPlaca());
        if (rotina.getCapacidade() > veiculo.getCapacidade()) {
            throw new DomainException("A capacidade não pode passar dos %d lugares do veículo %s.", veiculo.getCapacidade(), veiculo.getPlaca());
        }
        rotina.setVeiculo(veiculo);

        var motoristaId = rotina.getMotorista() == null ? null : rotina.getMotorista().getId();
        rotina.setMotorista(motoristaId == null ? null : usuarioRepository.findById(motoristaId)
                .filter(u -> u.getPerfil() == Perfil.MOTORISTA && Boolean.TRUE.equals(u.getAtivo()))
                .orElseThrow(() -> new DomainException("Selecione um motorista ativo.")));
    }

    /** Ordem segue a posição na lista; referência vazia vira nula. */
    private void normalizar(Rota rota) {
        rota.setNome(aparar(rota.getNome()));
        rota.setOrigem(aparar(rota.getOrigem()));
        rota.setDestino(aparar(rota.getDestino()));
        if (rota.getTipo() == null) rota.setTipo(TipoRota.UNICA);
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
