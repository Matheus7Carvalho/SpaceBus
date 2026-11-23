package com.spacebus.ops.domain.service.veiculo;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.core.service.BaseService;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.repository.rota.RotinaRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class VeiculoService extends BaseService<Veiculo> implements IVeiculoService {

    /** Padrão antigo (ABC1234) ou Mercosul (ABC1D23), já sem hífen. */
    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    private final ViagemRepository viagemRepository;
    private final RotinaRepository rotinaRepository;
    private final Clock clock;

    protected VeiculoService(final VeiculoRepository repository, final ViagemRepository viagemRepository,
                             final RotinaRepository rotinaRepository, final Clock clock) {
        super(repository);
        this.viagemRepository = viagemRepository;
        this.rotinaRepository = rotinaRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Veiculo create(Veiculo veiculo) throws DomainException {
        normalizar(veiculo);
        return super.create(veiculo);
    }

    @Override
    @Transactional
    public Veiculo update(Veiculo veiculo, String id) throws DomainException {
        normalizar(veiculo);
        // Valida antes de alterar a entidade gerenciada: senão o flush automático da consulta de unicidade
        // grava a placa repetida e o banco responde com erro genérico em vez da mensagem da regra.
        veiculo.setId(id);
        validate(veiculo);
        return super.update(veiculo, id);
    }

    @Override
    public void validate(Veiculo veiculo) throws DomainException {
        if (veiculo.getPlaca() == null || !PLACA.matcher(veiculo.getPlaca()).matches()) {
            throw new DomainException("Informe uma placa válida (ex.: ABC1D23).");
        }
        if (getRepository().existePlaca(veiculo.getPlaca(), veiculo.getId())) {
            throw new DomainException("Já existe um veículo com a placa %s.", veiculo.getPlaca());
        }
        if (veiculo.getId() != null) validarViagensFuturas(veiculo);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (viagemRepository.existsByVeiculoId(id)) {
            throw new DomainException("Veículo com viagens não pode ser excluído. Inative o veículo.");
        }
        super.delete(id);
    }

    /**
     * Viagens futuras não canceladas dependem do veículo: ele não pode ser inativado nem ter a capacidade
     * reduzida abaixo dos lugares já oferecidos.
     */
    private void validarViagensFuturas(Veiculo veiculo) {
        var agora = LocalDateTime.now(clock);
        if (Boolean.FALSE.equals(veiculo.getAtivo())) {
            if (viagemRepository.existeViagemFutura(veiculo.getId(), agora, StatusViagem.CANCELADA)) {
                throw new DomainException("Veículo com viagens futuras não pode ser inativado. Cancele as viagens ou troque o veículo delas.");
            }
            if (rotinaRepository.existeVigenteComVeiculo(veiculo.getId(), agora.toLocalDate())) {
                throw new DomainException("Veículo usado na rotina de uma rota fixa não pode ser inativado. Troque o veículo da rotina.");
            }
        }
        var maior = Math.max(viagemRepository.maiorCapacidadeFutura(veiculo.getId(), agora, StatusViagem.CANCELADA),
                rotinaRepository.maiorCapacidadeVigente(veiculo.getId(), agora.toLocalDate()));
        if (veiculo.getCapacidade() != null && maior > veiculo.getCapacidade()) {
            throw new DomainException("Há viagens futuras ou rotinas com %d lugares neste veículo. Ajuste-as antes de reduzir a capacidade.", maior);
        }
    }

    private void normalizar(Veiculo veiculo) {
        if (veiculo.getPlaca() != null) veiculo.setPlaca(veiculo.getPlaca().replaceAll("[\\s-]", "").toUpperCase());
        if (veiculo.getDescricao() != null) veiculo.setDescricao(veiculo.getDescricao().trim());
        if (veiculo.getAtivo() == null) veiculo.setAtivo(Boolean.TRUE);
    }

    @Override
    public VeiculoRepository getRepository() {
        return (VeiculoRepository) super.getRepository();
    }
}
