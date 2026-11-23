package com.spacebus.ops.domain.service.rotina;

import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Rotina;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.FrequenciaRotina;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeradorViagensTest {

    /** Terça, 17/11/2026, 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Araguaina"));
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);

    @Mock private ViagemRepository repository;
    @Mock private com.spacebus.ops.domain.repository.rota.RotinaRepository rotinaRepository;

    private GeradorViagens gerador;
    private Rotina rotina;
    private final Veiculo van = new Veiculo("SBV1A23", "Van", TipoVeiculo.VAN, 15, true);

    @BeforeEach
    void setUp() {
        gerador = new GeradorViagens(repository, rotinaRepository, CLOCK);
        van.setId("v1");
        var rota = new Rota("R", "A", "B", List.of());
        rotina = new Rotina();
        rotina.setId("rt1");
        rota.definirRotina(rotina);
        rotina.setFrequencia(FrequenciaRotina.DIARIA);
        rotina.setHorarios(new java.util.HashSet<>(Set.of(LocalTime.of(7, 0), LocalTime.of(18, 0))));
        rotina.setVeiculo(van);
        rotina.setPreco(new BigDecimal("12.00"));
        rotina.setCapacidade(12);
        rotina.setInicio(HOJE);
        rotina.setPublicar(true);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private List<Viagem> geradas() {
        var captor = ArgumentCaptor.forClass(Viagem.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void diariaGeraTodosOsHorariosFuturosDos30Dias() {
        gerador.gerar(rotina);

        // Hoje só 18:00 (07:00 já passou) + 30 dias × 2 horários.
        var viagens = geradas();
        assertThat(viagens).hasSize(61);
        assertThat(viagens.get(0).getPartida()).isEqualTo(HOJE.atTime(18, 0));
        assertThat(viagens.get(viagens.size() - 1).getPartida()).isEqualTo(HOJE.plusDays(30).atTime(18, 0));
        assertThat(viagens).allSatisfy(v -> {
            assertThat(v.getStatus()).isEqualTo(StatusViagem.PUBLICADA);
            assertThat(v.getRotina()).isSameAs(rotina);
            assertThat(v.getCapacidade()).isEqualTo(12);
            assertThat(v.getVeiculo()).isSameAs(van);
        });
        assertThat(rotina.getGeradas()).isEqualTo(61);
        assertThat(rotina.getConflitos()).isZero();
        assertThat(rotina.getGeradaEm()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    void semanalSoNosDiasMarcadosEEmRascunhoSemPublicar() {
        rotina.setFrequencia(FrequenciaRotina.SEMANAL);
        rotina.setDias(new java.util.HashSet<>(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)));
        rotina.setPublicar(false);

        gerador.gerar(rotina);

        var viagens = geradas();
        assertThat(viagens).allSatisfy(v -> {
            assertThat(v.getPartida().getDayOfWeek()).isIn(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            assertThat(v.getStatus()).isEqualTo(StatusViagem.RASCUNHO);
        });
        // De 18/11 a 17/12: 4 sextas e 4 segundas (20, 27/11, 4, 11/12; 23, 30/11, 7, 14/12) × 2 horários.
        assertThat(viagens).hasSize(16);
    }

    @Test
    void respeitaInicioEFimDaVigencia() {
        rotina.setInicio(HOJE.plusDays(5));
        rotina.setFim(HOJE.plusDays(6));

        gerador.gerar(rotina);

        assertThat(geradas()).extracting(Viagem::getPartida).containsExactly(
                HOJE.plusDays(5).atTime(7, 0), HOJE.plusDays(5).atTime(18, 0),
                HOJE.plusDays(6).atTime(7, 0), HOJE.plusDays(6).atTime(18, 0));
    }

    @Test
    void naoRecriaHorarioJaGeradoNemComConflitoDeVeiculo() {
        rotina.setFim(HOJE.plusDays(1));
        lenient().when(repository.existsByRotinaIdAndPartida("rt1", HOJE.plusDays(1).atTime(7, 0))).thenReturn(true); // já gerado (ou cancelado)
        lenient().when(repository.existeConflitoVeiculo(eq("v1"), eq(HOJE.plusDays(1).atTime(18, 0)), isNull(), eq(StatusViagem.CANCELADA))).thenReturn(true);

        gerador.gerar(rotina);

        assertThat(geradas()).extracting(Viagem::getPartida).containsExactly(HOJE.atTime(18, 0));
        assertThat(rotina.getGeradas()).isEqualTo(1);
        assertThat(rotina.getConflitos()).isEqualTo(1);
    }

    @Test
    void vigenciaEncerradaOuVeiculoInativoNaoGeramNada() {
        rotina.setInicio(HOJE.minusDays(10));
        rotina.setFim(HOJE.minusDays(1));
        gerador.gerar(rotina);

        rotina.setFim(null);
        van.setAtivo(false);
        gerador.gerar(rotina);

        verify(repository, never()).save(any());
        assertThat(rotina.getGeradas()).isZero();
    }

    @Test
    void regenerarDescartaRascunhosFuturosAntesDeGerar() {
        rotina.setFim(HOJE);

        gerador.regenerar(rotina);

        verify(repository).excluirRascunhosFuturos("rt1", LocalDateTime.now(CLOCK), StatusViagem.RASCUNHO);
        assertThat(geradas()).hasSize(1);
    }
}
