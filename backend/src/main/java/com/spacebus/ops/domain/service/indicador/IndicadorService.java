package com.spacebus.ops.domain.service.indicador;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.model.indicador.IndicadorViagemResponse;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse;
import com.spacebus.ops.domain.repository.indicador.IndicadorRepository;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class IndicadorService implements IIndicadorService {

    public static final int PERIODO_MAX_DIAS = 92;
    public static final int DIAS_ANTES = 30;
    public static final int DIAS_DEPOIS = 7;
    public static final int POR_PAGINA = 20;
    /** Tamanho da janela padrão, em dias (inclusivo). */
    static final int JANELA_DIAS = DIAS_ANTES + DIAS_DEPOIS + 1;
    /** Datas fora desta faixa de anos são tratadas como inválidas (evita estouro em datas extremas). */
    static final int ANO_MIN = 2000;
    static final int ANO_MAX = 2100;

    private final IndicadorRepository repository;
    private final RotaRepository rotaRepository;
    private final Clock clock;

    public IndicadorService(IndicadorRepository repository, RotaRepository rotaRepository, Clock clock) {
        this.repository = repository;
        this.rotaRepository = rotaRepository;
        this.clock = clock;
    }

    /** Dias [inicio, fim] por data de partida, no fuso da operação; consulta usa [inicio 00:00, fim + 1 dia 00:00). */
    record Periodo(LocalDate inicio, LocalDate fim, String rotaId) {
        LocalDateTime de() {
            return inicio.atStartOfDay();
        }

        LocalDateTime ate() {
            return fim.plusDays(1).atStartOfDay();
        }
    }

    @Override
    public ResumoIndicadoresResponse resumo(String inicio, String fim, String rotaId) {
        var p = periodo(inicio, fim, rotaId);
        var agora = LocalDateTime.now(clock);
        var t = repository.totais(p.de(), p.ate(), p.rotaId(), agora);
        var horarios = repository.porHorario(p.de(), p.ate(), p.rotaId(), agora).stream()
                .map(h -> new ResumoIndicadoresResponse.Horario(h.horario(), h.viagens(), h.oferecidos(), h.vendidos(), ocupacao(h.vendidos(), h.oferecidos())))
                .toList();
        return new ResumoIndicadoresResponse(p.inicio(), p.fim(), t.viagens(), t.vendidos(), t.oferecidos(), ocupacao(t.vendidos(), t.oferecidos()),
                t.receita(), horarios);
    }

    @Override
    public Page<IndicadorViagemResponse> porViagem(String inicio, String fim, String rotaId, int pagina) {
        var p = periodo(inicio, fim, rotaId);
        var agora = LocalDateTime.now(clock);
        var pageable = PageRequest.of(Math.max(pagina, 0), POR_PAGINA);
        var linhas = repository.porViagem(p.de(), p.ate(), p.rotaId(), agora, POR_PAGINA, pageable.getOffset());
        return new PageImpl<>(linhas, pageable, repository.contarViagens(p.de(), p.ate(), p.rotaId(), agora));
    }

    /** Média ponderada; sem lugares oferecidos não há ocupação ("—"). */
    static Double ocupacao(long vendidos, long oferecidos) {
        return oferecidos == 0 ? null : (double) vendidos / oferecidos;
    }

    Periodo periodo(String inicio, String fim, String rotaId) {
        var hoje = LocalDate.now(clock);
        var informadoDe = data(inicio);
        var informadoAte = data(fim);
        // Só uma data: a outra fecha a janela padrão (38 dias) a partir dela; nenhuma: hoje − 30 até hoje + 7.
        var de = informadoDe.orElseGet(() -> informadoAte.map(a -> a.minusDays(JANELA_DIAS - 1)).orElse(hoje.minusDays(DIAS_ANTES)));
        var ate = informadoAte.orElseGet(() -> informadoDe.map(d -> d.plusDays(JANELA_DIAS - 1)).orElse(hoje.plusDays(DIAS_DEPOIS)));
        if (de.isAfter(ate)) throw new DomainException("A data inicial deve ser anterior ou igual à final.");
        if (ChronoUnit.DAYS.between(de, ate) + 1 > PERIODO_MAX_DIAS) {
            throw new DomainException("O período pode ter no máximo %d dias.", PERIODO_MAX_DIAS);
        }
        var rota = rotaId == null || rotaId.isBlank() ? null : rotaId;
        if (rota != null && !rotaRepository.existsById(rota)) throw new DomainException("Rota não encontrada.");
        return new Periodo(de, ate, rota);
    }

    private static java.util.Optional<LocalDate> data(String texto) {
        if (texto == null || texto.isBlank()) return java.util.Optional.empty();
        try {
            var data = LocalDate.parse(texto);
            return data.getYear() < ANO_MIN || data.getYear() > ANO_MAX ? java.util.Optional.empty() : java.util.Optional.of(data);
        } catch (DateTimeParseException e) {
            return java.util.Optional.empty();
        }
    }
}
