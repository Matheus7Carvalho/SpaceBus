package com.spacebus.ops.domain.service.viagem;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.core.service.BaseService;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static com.spacebus.ops.domain.entity.enums.StatusViagem.CANCELADA;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.PUBLICADA;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.RASCUNHO;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.SUSPENSA;

@Service
public class ViagemService extends BaseService<Viagem> implements IViagemService {

    private final RotaRepository rotaRepository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    protected ViagemService(final ViagemRepository repository, final RotaRepository rotaRepository,
                            final VeiculoRepository veiculoRepository, final UsuarioRepository usuarioRepository,
                            final Clock clock) {
        super(repository);
        this.rotaRepository = rotaRepository;
        this.veiculoRepository = veiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Viagem create(Viagem viagem) throws DomainException {
        resolverReferencias(viagem);
        viagem.setStatus(RASCUNHO);
        return super.create(viagem);
    }

    /** Altera dados da viagem; o status só muda pelas transições. Cancelada não se altera. */
    @Override
    @Transactional
    public Viagem update(Viagem alterada, String id) throws DomainException {
        var existente = buscar(id);
        if (existente.getStatus() == CANCELADA) throw new DomainException("Viagem cancelada não pode ser alterada.");
        resolverReferencias(alterada);
        existente.setRota(alterada.getRota());
        existente.setVeiculo(alterada.getVeiculo());
        existente.setMotorista(alterada.getMotorista());
        existente.setPartida(alterada.getPartida());
        existente.setPreco(alterada.getPreco());
        existente.setCapacidade(alterada.getCapacidade());
        validate(existente);
        return getRepository().save(existente);
    }

    @Override
    public void validate(Viagem viagem) throws DomainException {
        if (!viagem.getPartida().isAfter(LocalDateTime.now(clock))) {
            throw new DomainException("A partida deve ser depois do horário atual.");
        }
        var veiculo = viagem.getVeiculo();
        if (!Boolean.TRUE.equals(veiculo.getAtivo())) {
            throw new DomainException("O veículo %s está inativo.", veiculo.getPlaca());
        }
        if (viagem.getCapacidade() > veiculo.getCapacidade()) {
            throw new DomainException("A capacidade não pode passar dos %d lugares do veículo %s.", veiculo.getCapacidade(), veiculo.getPlaca());
        }
        if (getRepository().existeConflitoVeiculo(veiculo.getId(), viagem.getPartida(), viagem.getId(), CANCELADA)) {
            throw new DomainException("O veículo %s já tem viagem nesse horário.", veiculo.getPlaca());
        }
    }

    @Override
    @Transactional
    public Viagem publicar(String id) {
        var viagem = buscar(id);
        exigirStatus(viagem, Set.of(RASCUNHO, SUSPENSA), "Só viagens em rascunho ou suspensas podem ser publicadas.");
        validate(viagem);
        return mudarStatus(viagem, PUBLICADA);
    }

    @Override
    @Transactional
    public Viagem suspender(String id) {
        var viagem = buscar(id);
        exigirStatus(viagem, Set.of(PUBLICADA), "Só viagens publicadas podem ser suspensas.");
        return mudarStatus(viagem, SUSPENSA);
    }

    @Override
    @Transactional
    public Viagem cancelar(String id) {
        var viagem = buscar(id);
        if (viagem.getStatus() == CANCELADA) throw new DomainException("A viagem já está cancelada.");
        return mudarStatus(viagem, CANCELADA);
    }

    /** Publicada ou suspensa já foi oferecida ao passageiro: sai do ar por cancelamento, não por exclusão. */
    @Override
    @Transactional
    public void delete(String id) {
        if (buscar(id).getStatus() != RASCUNHO) {
            throw new DomainException("Só viagens em rascunho podem ser excluídas. Cancele a viagem.");
        }
        super.delete(id);
    }

    /** Troca as referências (só id) pelas entidades reais. Partida guardada sem segundos. */
    private void resolverReferencias(Viagem viagem) {
        var rotaId = viagem.getRota() == null ? null : viagem.getRota().getId();
        viagem.setRota(rotaId == null ? null : rotaRepository.findById(rotaId)
                .orElseThrow(() -> new DomainException("Rota não encontrada.")));
        if (viagem.getRota() == null) throw new DomainException("Selecione a rota.");

        var veiculoId = viagem.getVeiculo() == null ? null : viagem.getVeiculo().getId();
        viagem.setVeiculo(veiculoId == null ? null : veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new DomainException("Veículo não encontrado.")));
        if (viagem.getVeiculo() == null) throw new DomainException("Selecione o veículo.");

        var motoristaId = viagem.getMotorista() == null ? null : viagem.getMotorista().getId();
        if (motoristaId == null) {
            viagem.setMotorista(null);
        } else {
            var motorista = usuarioRepository.findById(motoristaId)
                    .filter(u -> u.getPerfil() == Perfil.MOTORISTA && Boolean.TRUE.equals(u.getAtivo()))
                    .orElseThrow(() -> new DomainException("Selecione um motorista ativo."));
            viagem.setMotorista(motorista);
        }
        if (viagem.getPartida() != null) viagem.setPartida(viagem.getPartida().truncatedTo(ChronoUnit.MINUTES));
    }

    private Viagem buscar(String id) {
        return getRepository().findById(id).orElseThrow(() -> new EntityNotFoundException("Item não encontrado."));
    }

    private static void exigirStatus(Viagem viagem, Set<StatusViagem> permitidos, String mensagem) {
        if (!permitidos.contains(viagem.getStatus())) throw new DomainException(mensagem);
    }

    private Viagem mudarStatus(Viagem viagem, StatusViagem novo) {
        viagem.setStatus(novo);
        return getRepository().save(viagem);
    }

    @Override
    public ViagemRepository getRepository() {
        return (ViagemRepository) super.getRepository();
    }
}
