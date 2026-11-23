package com.spacebus.ops.domain.service.veiculo;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.repository.rota.RotinaRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Araguaina");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), FUSO);

    @Mock private VeiculoRepository repository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private RotinaRepository rotinaRepository;

    private VeiculoService service;

    @BeforeEach
    void setUp() {
        service = new VeiculoService(repository, viagemRepository, rotinaRepository, CLOCK);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Veiculo veiculo(String placa, int capacidade) {
        return new Veiculo(placa, " Van Sprinter ", TipoVeiculo.VAN, capacidade, null);
    }

    @Test
    void criaNormalizandoPlacaDescricaoEAtivo() {
        var salvo = service.create(veiculo(" sbv-1a23 ", 15));

        assertThat(salvo.getPlaca()).isEqualTo("SBV1A23");
        assertThat(salvo.getDescricao()).isEqualTo("Van Sprinter");
        assertThat(salvo.getAtivo()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD123", "1BC1D23", "ABC1D2X", "ABC12345"})
    void rejeitaPlacaInvalida(String placa) {
        assertThatThrownBy(() -> service.create(veiculo(placa, 15)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Informe uma placa válida (ex.: ABC1D23).");
        verify(repository, never()).save(any());
    }

    @Test
    void aceitaPlacaNoPadraoAntigo() {
        assertThat(service.create(veiculo("ABC-1234", 15)).getPlaca()).isEqualTo("ABC1234");
    }

    @Test
    void rejeitaPlacaDuplicada() {
        when(repository.existePlaca("SBV1A23", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(veiculo("SBV1A23", 15)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Já existe um veículo com a placa SBV1A23.");
        verify(repository, never()).save(any());
    }

    @Test
    void edicaoIgnoraAPropriaPlacaNaUnicidade() {
        var existente = veiculo("SBV1A23", 15);
        existente.setId("v1");
        when(repository.findById("v1")).thenReturn(Optional.of(existente));

        var salvo = service.update(veiculo("SBV1A23", 15), "v1");

        verify(repository, atLeastOnce()).existePlaca("SBV1A23", "v1");
        assertThat(salvo.getId()).isEqualTo("v1");
    }

    /** A regra roda antes de alterar a entidade gerenciada; senão o flush grava a placa repetida e o banco recusa. */
    @Test
    void edicaoComPlacaRepetidaNaoAlteraORegistro() {
        var existente = veiculo("SBV2B34", 12);
        existente.setId("v2");
        lenient().when(repository.findById("v2")).thenReturn(Optional.of(existente));
        when(repository.existePlaca("SBV1A23", "v2")).thenReturn(true);

        assertThatThrownBy(() -> service.update(veiculo("sbv-1a23", 12), "v2"))
                .hasMessage("Já existe um veículo com a placa SBV1A23.");
        assertThat(existente.getPlaca()).isEqualTo("SBV2B34");
        verify(repository, never()).save(any());
    }

    @Test
    void naoInativaVeiculoComViagensFuturas() {
        var existente = veiculo("SBV1A23", 15);
        existente.setId("v1");
        lenient().when(repository.findById("v1")).thenReturn(Optional.of(existente));
        when(viagemRepository.existeViagemFutura("v1", LocalDateTime.now(CLOCK), StatusViagem.CANCELADA)).thenReturn(true);
        var inativo = veiculo("SBV1A23", 15);
        inativo.setAtivo(false);

        assertThatThrownBy(() -> service.update(inativo, "v1"))
                .hasMessage("Veículo com viagens futuras não pode ser inativado. Cancele as viagens ou troque o veículo delas.");
        assertThat(existente.getAtivo()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void naoInativaNemReduzVeiculoDeRotinaVigente() {
        var existente = veiculo("SBV1A23", 15);
        existente.setId("v1");
        lenient().when(repository.findById("v1")).thenReturn(Optional.of(existente));
        var hoje = LocalDateTime.now(CLOCK).toLocalDate();
        when(rotinaRepository.existeVigenteComVeiculo("v1", hoje)).thenReturn(true);
        var inativo = veiculo("SBV1A23", 15);
        inativo.setAtivo(false);

        assertThatThrownBy(() -> service.update(inativo, "v1"))
                .hasMessage("Veículo usado na rotina de uma rota fixa não pode ser inativado. Troque o veículo da rotina.");

        when(rotinaRepository.maiorCapacidadeVigente("v1", hoje)).thenReturn(14);
        assertThatThrownBy(() -> service.update(veiculo("SBV1A23", 12), "v1"))
                .hasMessage("Há viagens futuras ou rotinas com 14 lugares neste veículo. Ajuste-as antes de reduzir a capacidade.");
        verify(repository, never()).save(any());
    }

    @Test
    void inativaVeiculoSemViagensFuturas() {
        var existente = veiculo("SBV1A23", 15);
        existente.setId("v1");
        when(repository.findById("v1")).thenReturn(Optional.of(existente));
        var inativo = veiculo("SBV1A23", 15);
        inativo.setAtivo(false);

        assertThat(service.update(inativo, "v1").getAtivo()).isFalse();
    }

    @Test
    void naoReduzCapacidadeAbaixoDeViagemFutura() {
        var existente = veiculo("SBV1A23", 15);
        existente.setId("v1");
        lenient().when(repository.findById("v1")).thenReturn(Optional.of(existente));
        when(viagemRepository.maiorCapacidadeFutura(eq("v1"), eq(LocalDateTime.now(CLOCK)), eq(StatusViagem.CANCELADA))).thenReturn(15);

        assertThatThrownBy(() -> service.update(veiculo("SBV1A23", 12), "v1"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Há viagens futuras ou rotinas com 15 lugares neste veículo. Ajuste-as antes de reduzir a capacidade.");
        verify(repository, never()).save(any());
    }

    @Test
    void naoExcluiVeiculoComViagens() {
        when(viagemRepository.existsByVeiculoId("v1")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("v1"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Veículo com viagens não pode ser excluído. Inative o veículo.");
        verify(repository, never()).deleteById(any());
    }

    @Test
    void excluiVeiculoSemViagens() {
        service.delete("v1");

        verify(repository).deleteById("v1");
    }
}
