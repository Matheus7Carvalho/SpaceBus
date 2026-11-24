package com.spacebus.ops.domain.service.indicador;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse;
import com.spacebus.ops.domain.repository.indicador.IndicadorRepository;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicadorServiceTest {

    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Araguaina"));
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);

    @Mock private IndicadorRepository repository;
    @Mock private RotaRepository rotaRepository;
    private IndicadorService service;

    @BeforeEach
    void setUp() {
        service = new IndicadorService(repository, rotaRepository, CLOCK);
        lenient().when(repository.totais(any(), any(), any(), any())).thenReturn(
                new IndicadorRepository.Totais(new ResumoIndicadoresResponse.Viagens(0, 0, 0, 0, 0), 0, 0, BigDecimal.ZERO));
    }

    @Test
    void semDatasUsaHojeMenos30AteHojeMais7NoFusoDaOperacao() {
        var r = service.resumo(null, "", null);

        assertThat(r.inicio()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(r.fim()).isEqualTo(LocalDate.of(2026, 11, 24));
        verify(repository).totais(LocalDateTime.of(2026, 10, 18, 0, 0), LocalDateTime.of(2026, 11, 25, 0, 0), null, AGORA);
    }

    @Test
    void dataInvalidaVaiParaOPadrao() {
        var r = service.resumo("2026-13-40", "abc", " ");

        assertThat(r.inicio()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(r.fim()).isEqualTo(LocalDate.of(2026, 11, 24));
    }

    /** QA: data extrema (ano fora de 2000–2100) não estoura; vira o período padrão. */
    @Test
    void dataComAnoForaDaFaixaVaiParaOPadrao() {
        var r = service.resumo("+999999999-12-31", "-999999999-01-01", null);

        assertThat(r.inicio()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(r.fim()).isEqualTo(LocalDate.of(2026, 11, 24));
        assertThat(service.resumo("1999-12-31", "2101-01-01", null).inicio()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    /** QA: só uma data informada fecha a janela padrão de 38 dias a partir dela, sem passar de 92 dias. */
    @Test
    void soUmaDataCalculaAOutraComAJanelaPadrao() {
        var soInicio = service.resumo("2026-01-01", null, null);
        assertThat(soInicio.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(soInicio.fim()).isEqualTo(LocalDate.of(2026, 2, 7));

        var soFim = service.resumo(null, "2026-03-31", null);
        assertThat(soFim.inicio()).isEqualTo(LocalDate.of(2026, 2, 22));
        assertThat(soFim.fim()).isEqualTo(LocalDate.of(2026, 3, 31));

        var deDepoisDoPadrao = service.resumo("2026-12-20", "", null);
        assertThat(deDepoisDoPadrao.fim()).isEqualTo(LocalDate.of(2027, 1, 26));
    }

    @Test
    void periodoDeUmDiaVaiDaMeiaNoiteAteAMeiaNoiteSeguinte() {
        service.resumo("2026-11-20", "2026-11-20", null);

        verify(repository).totais(LocalDateTime.of(2026, 11, 20, 0, 0), LocalDateTime.of(2026, 11, 21, 0, 0), null, AGORA);
    }

    @Test
    void inicioDepoisDoFimERecusado() {
        assertThatThrownBy(() -> service.resumo("2026-11-21", "2026-11-20", null)).isInstanceOf(DomainException.class)
                .hasMessage("A data inicial deve ser anterior ou igual à final.");
        verifyNoInteractions(repository);
    }

    @Test
    void periodoDe92DiasPassaE93NaoPassa() {
        service.resumo("2026-01-01", "2026-04-02", null); // 92 dias

        assertThatThrownBy(() -> service.resumo("2026-01-01", "2026-04-03", null)).hasMessage("O período pode ter no máximo 92 dias.");
    }

    @Test
    void rotaInexistenteERecusada() {
        when(rotaRepository.existsById("x")).thenReturn(false);

        assertThatThrownBy(() -> service.resumo(null, null, "x")).hasMessage("Rota não encontrada.");
        verifyNoInteractions(repository);
    }

    @Test
    void ocupacaoPonderadaENulaSemLugaresOferecidos() {
        when(rotaRepository.existsById("r1")).thenReturn(true);
        when(repository.totais(any(), any(), eq("r1"), any())).thenReturn(
                new IndicadorRepository.Totais(new ResumoIndicadoresResponse.Viagens(3, 2, 0, 1, 2), 22, 40, new BigDecimal("330.00")));
        when(repository.porHorario(any(), any(), eq("r1"), any())).thenReturn(List.of(
                new IndicadorRepository.LinhaHorario("07:30", 2, 30, 12), new IndicadorRepository.LinhaHorario("18:00", 1, 0, 0)));

        var r = service.resumo(null, null, "r1");

        assertThat(r.ocupacao()).isEqualTo(0.55);
        assertThat(r.horarios()).extracting(ResumoIndicadoresResponse.Horario::ocupacao).containsExactly(0.4, null);
        assertThat(IndicadorService.ocupacao(0, 0)).isNull();
    }

    @Test
    void porViagemPagina20() {
        service.porViagem(null, null, null, 2);

        verify(repository).porViagem(any(), any(), isNull(), eq(AGORA), eq(20), eq(40L));
        verify(repository).contarViagens(any(), any(), isNull(), eq(AGORA));
    }
}
