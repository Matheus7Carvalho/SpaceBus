package com.spacebus.ops.domain.service.viagem;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.localizacao.LocalizacaoPropriedades;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.spacebus.ops.domain.entity.enums.StatusViagem.CANCELADA;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.PUBLICADA;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.RASCUNHO;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.SUSPENSA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Araguaina");
    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), FUSO);
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);
    private static final LocalDateTime AMANHA_7H = LocalDateTime.of(2026, 11, 18, 7, 0);

    @Mock private ViagemRepository repository;
    @Mock private RotaRepository rotaRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private IReservaService reservas;

    private ViagemService service;
    private final Rota rota = new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of());
    private final Veiculo van = new Veiculo("SBV1A23", "Van Sprinter", TipoVeiculo.VAN, 15, true);
    private final Usuario motorista = new Usuario("João Costa", "motorista@spacebus.local", "hash", Perfil.MOTORISTA, true);

    @BeforeEach
    void setUp() {
        service = new ViagemService(repository, rotaRepository, veiculoRepository, usuarioRepository, reservas, CLOCK, LocalizacaoPropriedades.padrao());
        rota.setId("r1");
        van.setId("v1");
        motorista.setId("m1");
        lenient().when(rotaRepository.findById("r1")).thenReturn(Optional.of(rota));
        lenient().when(veiculoRepository.findById("v1")).thenReturn(Optional.of(van));
        lenient().when(usuarioRepository.findById("m1")).thenReturn(Optional.of(motorista));
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Viagem como chega do mapper: relações só com id. */
    private static Viagem pedido(String motoristaId, LocalDateTime partida, int capacidade) {
        var r = new Rota();
        r.setId("r1");
        var v = new Veiculo();
        v.setId("v1");
        Usuario m = null;
        if (motoristaId != null) {
            m = new Usuario();
            m.setId(motoristaId);
        }
        return new Viagem(r, v, m, partida, new BigDecimal("18.50"), capacidade, null);
    }

    private Viagem existente(StatusViagem status) {
        var viagem = new Viagem(rota, van, null, AMANHA_7H, new BigDecimal("18.50"), 15, status);
        viagem.setId("vg1");
        lenient().when(repository.findById("vg1")).thenReturn(Optional.of(viagem));
        lenient().when(repository.travar("vg1")).thenReturn(Optional.of(viagem));
        return viagem;
    }

    @Test
    void criaComoRascunhoResolvendoReferenciasESemSegundos() {
        var salva = service.create(pedido("m1", AMANHA_7H.plusSeconds(42), 15));

        assertThat(salva.getStatus()).isEqualTo(RASCUNHO);
        assertThat(salva.getRota()).isSameAs(rota);
        assertThat(salva.getVeiculo()).isSameAs(van);
        assertThat(salva.getMotorista()).isSameAs(motorista);
        assertThat(salva.getPartida()).isEqualTo(AMANHA_7H);
    }

    @Test
    void motoristaEOpcional() {
        assertThat(service.create(pedido(null, AMANHA_7H, 15)).getMotorista()).isNull();
    }

    @Test
    void rejeitaRotaInexistente() {
        when(rotaRepository.findById("r1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(pedido(null, AMANHA_7H, 15))).hasMessage("Rota não encontrada.");
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaUsuarioQueNaoEMotorista() {
        var passageiro = new Usuario("Paula", "passageiro@spacebus.local", "hash", Perfil.PASSAGEIRO, true);
        when(usuarioRepository.findById("p1")).thenReturn(Optional.of(passageiro));

        assertThatThrownBy(() -> service.create(pedido("p1", AMANHA_7H, 15))).hasMessage("Selecione um motorista ativo.");
    }

    @Test
    void rejeitaMotoristaInativo() {
        motorista.setAtivo(false);

        assertThatThrownBy(() -> service.create(pedido("m1", AMANHA_7H, 15))).hasMessage("Selecione um motorista ativo.");
    }

    @Test
    void rejeitaPartidaNoPassadoOuAgora() {
        assertThatThrownBy(() -> service.create(pedido(null, AGORA, 15)))
                .isInstanceOf(DomainException.class)
                .hasMessage("A partida deve ser depois do horário atual.");
        assertThatThrownBy(() -> service.create(pedido(null, AGORA.minusDays(1), 15)))
                .hasMessage("A partida deve ser depois do horário atual.");
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaCapacidadeAcimaDoVeiculo() {
        assertThatThrownBy(() -> service.create(pedido(null, AMANHA_7H, 16)))
                .hasMessage("A capacidade não pode passar dos 15 lugares do veículo SBV1A23.");
    }

    @Test
    void rejeitaVeiculoInativo() {
        van.setAtivo(false);

        assertThatThrownBy(() -> service.create(pedido(null, AMANHA_7H, 15))).hasMessage("O veículo SBV1A23 está inativo.");
    }

    @Test
    void rejeitaVeiculoComOutraViagemNoMesmoHorario() {
        when(repository.existeConflitoVeiculo("v1", AMANHA_7H, null, CANCELADA)).thenReturn(true);

        assertThatThrownBy(() -> service.create(pedido(null, AMANHA_7H, 15))).hasMessage("O veículo SBV1A23 já tem viagem nesse horário.");
    }

    @Test
    void edicaoMantemStatusEIgnoraAPropriaViagemNoConflito() {
        var viagem = existente(PUBLICADA);
        var alterada = pedido("m1", AMANHA_7H.plusHours(1), 10);
        alterada.setStatus(CANCELADA); // status do pedido é ignorado

        var salva = service.update(alterada, "vg1");

        assertThat(salva).isSameAs(viagem);
        assertThat(salva.getStatus()).isEqualTo(PUBLICADA);
        assertThat(salva.getCapacidade()).isEqualTo(10);
        assertThat(salva.getPartida()).isEqualTo(AMANHA_7H.plusHours(1));
        assertThat(salva.getMotorista()).isSameAs(motorista);
        verify(repository).existeConflitoVeiculo(eq("v1"), eq(AMANHA_7H.plusHours(1)), eq("vg1"), eq(CANCELADA));
    }

    @Test
    void naoEditaViagemCancelada() {
        existente(CANCELADA);

        assertThatThrownBy(() -> service.update(pedido(null, AMANHA_7H, 15), "vg1")).hasMessage("Viagem cancelada não pode ser alterada.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "SUSPENSA"})
    void publicaRascunhoOuSuspensa(StatusViagem inicial) {
        existente(inicial);

        assertThat(service.publicar("vg1").getStatus()).isEqualTo(PUBLICADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"PUBLICADA", "CANCELADA"})
    void naoPublicaPublicadaOuCancelada(StatusViagem inicial) {
        existente(inicial);

        assertThatThrownBy(() -> service.publicar("vg1")).hasMessage("Só viagens em rascunho ou suspensas podem ser publicadas.");
        verify(repository, never()).save(any());
    }

    @Test
    void naoPublicaViagemQueJaPartiu() {
        existente(RASCUNHO).setPartida(AGORA.minusMinutes(1));

        assertThatThrownBy(() -> service.publicar("vg1")).hasMessage("A partida deve ser depois do horário atual.");
        verify(repository, never()).save(any());
    }

    @Test
    void naoPublicaComVeiculoInativado() {
        existente(SUSPENSA);
        van.setAtivo(false);

        assertThatThrownBy(() -> service.publicar("vg1")).hasMessage("O veículo SBV1A23 está inativo.");
    }

    @Test
    void publicarRevalidaCapacidadeEConflito() {
        var viagem = existente(RASCUNHO);
        van.setCapacidade(12); // veículo editado depois da viagem criada com 15 lugares

        assertThatThrownBy(() -> service.publicar("vg1")).hasMessage("A capacidade não pode passar dos 12 lugares do veículo SBV1A23.");

        van.setCapacidade(15);
        when(repository.existeConflitoVeiculo("v1", AMANHA_7H, "vg1", CANCELADA)).thenReturn(true);
        assertThatThrownBy(() -> service.publicar("vg1")).hasMessage("O veículo SBV1A23 já tem viagem nesse horário.");
        assertThat(viagem.getStatus()).isEqualTo(RASCUNHO);
        verify(repository, never()).save(any());
    }

    @Test
    void agendaComDiaUsaSoAqueleDia() {
        var pagina = PageRequest.of(0, 100);
        service.agenda(LocalDate.of(2026, 11, 10), "r1", PUBLICADA, pagina);

        verify(repository).agenda(LocalDateTime.of(2026, 11, 10, 0, 0), LocalDateTime.of(2026, 11, 11, 0, 0), "r1", "PUBLICADA", pagina);
    }

    @Test
    void agendaSemDiaComecaHojeEFiltrosVaziosViramNulos() {
        var pagina = PageRequest.of(0, 100);
        service.agenda(null, " ", null, pagina);

        var hoje = AGORA.toLocalDate().atStartOfDay();
        verify(repository).agenda(hoje, hoje.plusYears(10), null, null, pagina);
    }

    @Test
    void naoReduzCapacidadeAbaixoDosLugaresReservados() {
        existente(PUBLICADA);
        when(reservas.lugaresOcupados("vg1")).thenReturn(11);

        assertThatThrownBy(() -> service.update(pedido(null, AMANHA_7H, 10), "vg1"))
                .hasMessage("Já há 11 lugares reservados nesta viagem. A capacidade não pode ser menor.");
        verify(repository, never()).save(any());
    }

    @Test
    void viagemComReservasNaoTrocaDeRota() {
        existente(PUBLICADA);
        when(reservas.lugaresOcupados("vg1")).thenReturn(1);
        var outra = new Rota("Outra", "X", "Y", List.of());
        outra.setId("r2");
        when(rotaRepository.findById("r2")).thenReturn(Optional.of(outra));
        var pedido = pedido(null, AMANHA_7H, 15);
        pedido.getRota().setId("r2");

        assertThatThrownBy(() -> service.update(pedido, "vg1")).hasMessage("Viagem com reservas não pode trocar de rota.");
    }

    @Test
    void cancelarViagemCancelaAsReservas() {
        existente(PUBLICADA);

        service.cancelar("vg1", "op1");

        verify(reservas).cancelarPorViagem("vg1", "op1");
    }

    @Test
    void transicoesTravamAViagem() {
        existente(RASCUNHO);

        service.publicar("vg1");

        verify(repository).travar("vg1");
    }

    @Test
    void suspendeSoPublicada() {
        existente(PUBLICADA);
        assertThat(service.suspender("vg1").getStatus()).isEqualTo(SUSPENSA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "SUSPENSA", "CANCELADA"})
    void naoSuspendeOutrasSituacoes(StatusViagem inicial) {
        existente(inicial);

        assertThatThrownBy(() -> service.suspender("vg1")).hasMessage("Só viagens publicadas podem ser suspensas.");
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "PUBLICADA", "SUSPENSA"})
    void cancelaQualquerSituacaoAtiva(StatusViagem inicial) {
        existente(inicial);

        assertThat(service.cancelar("vg1", "op1").getStatus()).isEqualTo(CANCELADA);
    }

    @Test
    void naoCancelaDuasVezes() {
        existente(CANCELADA);

        assertThatThrownBy(() -> service.cancelar("vg1", "op1")).hasMessage("A viagem já está cancelada.");
    }

    @Test
    void excluiSoRascunho() {
        existente(RASCUNHO);
        service.delete("vg1");
        verify(repository).deleteById("vg1");
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"PUBLICADA", "SUSPENSA", "CANCELADA"})
    void naoExcluiViagemJaOferecida(StatusViagem inicial) {
        existente(inicial);

        assertThatThrownBy(() -> service.delete("vg1")).hasMessage("Só viagens em rascunho podem ser excluídas. Cancele a viagem.");
        verify(repository, never()).deleteById(any());
    }
}
