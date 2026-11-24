package com.spacebus.ops.domain.service.viagem;

import com.spacebus.ops.domain.core.exception.AcessoNegadoException;
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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.spacebus.ops.domain.entity.enums.StatusViagem.EM_ANDAMENTO;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.ENCERRADA;
import static com.spacebus.ops.domain.entity.enums.StatusViagem.PUBLICADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** H13: iniciar e encerrar a viagem (janela, motorista, outra viagem em andamento, cancelar em andamento). */
@ExtendWith(MockitoExtension.class)
class ViagemInicioEncerramentoTest {

    private static final ZoneId FUSO = ZoneId.of("America/Araguaina");
    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), FUSO);
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);

    @Mock private ViagemRepository repository;
    @Mock private RotaRepository rotaRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private IReservaService reservas;

    private ViagemService service;
    private final Rota rota = new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of());
    private final Veiculo van = new Veiculo("SBV1A23", "Van Sprinter", TipoVeiculo.VAN, 15, true);
    private final Usuario joao = new Usuario("João Costa", "motorista@spacebus.local", "hash", Perfil.MOTORISTA, true);

    @BeforeEach
    void setUp() {
        service = new ViagemService(repository, rotaRepository, veiculoRepository, usuarioRepository, reservas, CLOCK,
                LocalizacaoPropriedades.padrao());
        van.setId("v1");
        joao.setId("m1");
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Viagem viagem(StatusViagem status, LocalDateTime partida) {
        var viagem = new Viagem(rota, van, joao, partida, new BigDecimal("18.50"), 15, status);
        viagem.setId("vg1");
        lenient().when(repository.travar("vg1")).thenReturn(Optional.of(viagem));
        return viagem;
    }

    private Viagem publicadaEm30Min() {
        return viagem(PUBLICADA, AGORA.plusMinutes(30));
    }

    @Test
    void motoristaIniciaAViagemDeleExatamenteTrintaMinutosAntesComHoraEResponsavel() {
        publicadaEm30Min();

        var iniciada = service.iniciar("vg1", "m1", "m1");

        assertThat(iniciada.getStatus()).isEqualTo(EM_ANDAMENTO);
        assertThat(iniciada.getIniciadaEm()).isEqualTo(AGORA);
        assertThat(iniciada.getIniciadaPor()).isEqualTo("m1");
        verify(repository).travar("vg1");
        verify(repository).saveAndFlush(iniciada);
    }

    @Test
    void operadorIniciaSemConferirMotoristaEViagemSemMotoristaTambemInicia() {
        publicadaEm30Min().setMotorista(null);

        var iniciada = service.iniciar("vg1", "op1", null);

        assertThat(iniciada.getStatus()).isEqualTo(EM_ANDAMENTO);
        assertThat(iniciada.getIniciadaPor()).isEqualTo("op1");
    }

    @Test
    void trintaEUmMinutosAntesAindaNaoInicia() {
        viagem(PUBLICADA, AGORA.plusMinutes(31));

        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A viagem pode ser iniciada a partir de 30 minutos antes da partida.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void duasHorasDepoisDaPartidaAindaIniciaMasUmMinutoAMaisNao() {
        viagem(PUBLICADA, AGORA.minusMinutes(120));
        assertThat(service.iniciar("vg1", "m1", "m1").getStatus()).isEqualTo(EM_ANDAMENTO);

        viagem(PUBLICADA, AGORA.minusMinutes(121));
        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1")).hasMessage("O prazo para iniciar esta viagem acabou.");
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "SUSPENSA", "CANCELADA", "ENCERRADA"})
    void soPublicadaInicia(StatusViagem status) {
        viagem(status, AGORA.plusMinutes(10));

        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1")).hasMessage("Só viagens publicadas podem ser iniciadas.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void segundoToqueRecebeJaEmAndamento() {
        viagem(EM_ANDAMENTO, AGORA.plusMinutes(10));

        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1")).hasMessage("A viagem já está em andamento.");
    }

    @Test
    void motoristaDeOutraViagemNaoIniciaNemEncerra() {
        publicadaEm30Min();

        assertThatThrownBy(() -> service.iniciar("vg1", "m2", "m2"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Só o motorista da viagem pode iniciar.");
        viagem(EM_ANDAMENTO, AGORA);
        assertThatThrownBy(() -> service.encerrar("vg1", "m2", "m2"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Só o motorista da viagem pode encerrar.");
        verify(repository, never()).saveAndFlush(any());
        verify(repository, never()).save(any());
    }

    @Test
    void motoristaComOutraViagemEmAndamentoNaoIniciaOutra() {
        publicadaEm30Min();
        when(repository.existsByMotoristaIdAndStatusAndIdNot("m1", EM_ANDAMENTO, "vg1")).thenReturn(true);

        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1")).hasMessage("Encerre a viagem em andamento antes de iniciar outra.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void veiculoEmOutraViagemEmAndamentoNaoInicia() {
        publicadaEm30Min();
        when(repository.existsByVeiculoIdAndStatusAndIdNot("v1", EM_ANDAMENTO, "vg1")).thenReturn(true);

        assertThatThrownBy(() -> service.iniciar("vg1", "op1", null)).hasMessage("O veículo já está em outra viagem em andamento.");
        verify(repository, never()).saveAndFlush(any());
    }

    /** Dois inícios simultâneos do mesmo motorista em viagens diferentes: o índice único decide e a mensagem é a da regra. */
    @Test
    void indiceUnicoDoMotoristaViraMensagemDaRegra() {
        publicadaEm30Min();
        when(repository.saveAndFlush(any())).thenThrow(violacao("uk_viagem_motorista_em_andamento"));

        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1")).hasMessage("Encerre a viagem em andamento antes de iniciar outra.");
    }

    @Test
    void indiceUnicoDoVeiculoViraMensagemDaRegra() {
        publicadaEm30Min();
        when(repository.saveAndFlush(any())).thenThrow(violacao("uk_viagem_veiculo_em_andamento"));

        assertThatThrownBy(() -> service.iniciar("vg1", "m1", "m1")).hasMessage("O veículo já está em outra viagem em andamento.");
    }

    private static DataIntegrityViolationException violacao(String indice) {
        var causa = new ConstraintViolationException("duplicada", new SQLException("duplicate key value violates unique constraint \"" + indice + "\""), indice);
        return new DataIntegrityViolationException("could not execute statement", causa);
    }

    @Test
    void encerraEmAndamentoComHoraEResponsavel() {
        viagem(EM_ANDAMENTO, AGORA.minusMinutes(40));

        var encerrada = service.encerrar("vg1", "m1", "m1");

        assertThat(encerrada.getStatus()).isEqualTo(ENCERRADA);
        assertThat(encerrada.getEncerradaEm()).isEqualTo(AGORA);
        assertThat(encerrada.getEncerradaPor()).isEqualTo("m1");
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "PUBLICADA", "SUSPENSA", "CANCELADA", "ENCERRADA"})
    void soEmAndamentoEncerra(StatusViagem status) {
        viagem(status, AGORA);

        assertThatThrownBy(() -> service.encerrar("vg1", "op1", null)).hasMessage("Só viagens em andamento podem ser encerradas.");
        verify(repository, never()).save(any());
    }

    @Test
    void viagemEmAndamentoNaoECanceladaNemCancelaReservas() {
        viagem(EM_ANDAMENTO, AGORA);

        assertThatThrownBy(() -> service.cancelar("vg1", "op1")).hasMessage("Viagem em andamento não pode ser cancelada. Encerre a viagem.");
        verifyNoInteractions(reservas);
        verify(repository, never()).save(any());
    }

    @Test
    void viagemEncerradaNaoECancelada() {
        viagem(ENCERRADA, AGORA);

        assertThatThrownBy(() -> service.cancelar("vg1", "op1")).hasMessage("Viagem encerrada não pode ser cancelada.");
        verifyNoInteractions(reservas);
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"EM_ANDAMENTO", "ENCERRADA"})
    void viagemEmAndamentoOuEncerradaNaoEAlterada(StatusViagem status) {
        viagem(status, AGORA);

        assertThatThrownBy(() -> service.update(new Viagem(), "vg1")).hasMessage("Viagem em andamento ou encerrada não pode ser alterada.");
        verify(repository, never()).save(any());
    }

    @Test
    void impedimentoInformaOMotivoSemMudarNada() {
        var viagem = viagem(PUBLICADA, AGORA.plusHours(3));

        assertThat(service.impedimentoParaIniciar(viagem)).contains("A viagem pode ser iniciada a partir de 30 minutos antes da partida.");
        assertThat(service.impedimentoParaIniciar(viagem(PUBLICADA, AGORA))).isEmpty();
        verifyNoInteractions(reservas);
    }

    @Test
    void encerramentoAutomaticoUsaSeisHorasAposOInicio() {
        when(repository.encerrarIniciadasAte(AGORA.minusHours(6), AGORA)).thenReturn(2);

        assertThat(service.encerrarEsquecidas()).isEqualTo(2);
    }
}
