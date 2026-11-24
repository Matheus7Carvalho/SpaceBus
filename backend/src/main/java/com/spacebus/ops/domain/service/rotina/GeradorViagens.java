package com.spacebus.ops.domain.service.rotina;

import com.spacebus.ops.domain.entity.Rotina;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.repository.rota.RotinaRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;

/**
 * Gera as viagens de uma Rota Fixa a partir da rotina, até {@link #HORIZONTE_DIAS} dias à frente.
 * Nunca duplica um horário já gerado (mesmo cancelado) e pula horários em que o veículo já tem viagem.
 */
@Slf4j
@Service
public class GeradorViagens {

    public static final int HORIZONTE_DIAS = 30;

    private final ViagemRepository viagemRepository;
    private final RotinaRepository rotinaRepository;
    private final Clock clock;

    public GeradorViagens(ViagemRepository viagemRepository, RotinaRepository rotinaRepository, Clock clock) {
        this.viagemRepository = viagemRepository;
        this.rotinaRepository = rotinaRepository;
        this.clock = clock;
    }

    /** A rotina mudou: refaz os rascunhos futuros dela com os dados novos e completa o horizonte. */
    @Transactional
    public void regenerar(Rotina rotina) {
        descartarRascunhosFuturos(rotina);
        gerar(rotina);
    }

    /** Rotina saindo (rota passou a Única): seus rascunhos futuros deixam de existir. */
    @Transactional
    public void descartarRascunhosFuturos(Rotina rotina) {
        if (rotina.getId() == null) return;
        var excluidos = viagemRepository.excluirRascunhosFuturos(rotina.getId(), LocalDateTime.now(clock), StatusViagem.RASCUNHO);
        if (excluidos > 0) log.info("Rotina {}: {} rascunho(s) futuro(s) excluído(s).", rotina.getId(), excluidos);
    }

    /** Usado pelo agendador: cada rotina na sua transação, para a falha de uma não desfazer as outras. */
    @Transactional
    public void gerarPorId(String rotinaId) {
        rotinaRepository.findById(rotinaId).ifPresent(this::gerar);
    }

    /** Cria as viagens que faltam no horizonte e registra o resultado na rotina. */
    @Transactional
    public void gerar(Rotina rotina) {
        var agora = LocalDateTime.now(clock);
        var hoje = agora.toLocalDate();
        var ultimoDia = hoje.plusDays(HORIZONTE_DIAS);
        if (rotina.getFim() != null && rotina.getFim().isBefore(ultimoDia)) ultimoDia = rotina.getFim();
        var criadas = 0;
        var conflitos = 0;

        // Veículo inativo não recebe viagem nova; a rotina fica parada até o operador trocar o veículo.
        if (Boolean.TRUE.equals(rotina.getVeiculo().getAtivo())) {
            for (LocalDate dia = rotina.getInicio().isAfter(hoje) ? rotina.getInicio() : hoje; !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
                if (!rotina.valeEm(dia)) continue;
                for (LocalTime horario : rotina.getHorarios().stream().sorted(Comparator.naturalOrder()).toList()) {
                    var partida = dia.atTime(horario);
                    if (!partida.isAfter(agora) || viagemRepository.existsByRotinaIdAndPartida(rotina.getId(), partida)) continue;
                    if (viagemRepository.existeConflitoVeiculo(rotina.getVeiculo().getId(), partida, null, StatusViagem.CANCELADA)) {
                        conflitos++;
                        continue;
                    }
                    viagemRepository.save(nova(rotina, partida));
                    criadas++;
                }
            }
        }
        rotina.setGeradaEm(agora);
        rotina.setGeradas(criadas);
        rotina.setConflitos(conflitos);
    }

    private static Viagem nova(Rotina rotina, LocalDateTime partida) {
        var status = Boolean.TRUE.equals(rotina.getPublicar()) ? StatusViagem.PUBLICADA : StatusViagem.RASCUNHO;
        return new Viagem(rotina.getRota(), rotina.getVeiculo(), rotina.getMotorista(), partida, rotina.getPreco(), rotina.getCapacidade(), status, rotina);
    }
}
