package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.LocalizacaoViagem;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.MotivoRejeicaoPosicao;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.localizacao.PosicaoRequest;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;
import com.spacebus.ops.domain.repository.localizacao.LocalizacaoViagemRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.auditoria.IAuditoriaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** H14 a H17: classificação e gravação das posições, visão do passageiro e da operação, expurgo. */
@ExtendWith(MockitoExtension.class)
class LocalizacaoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Araguaina");
    /** 17/11/2026 09:00 no fuso da operação (12:00 UTC). */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), FUSO);
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);

    @Mock private LocalizacaoViagemRepository repository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private IAuditoriaService auditoria;

    private LocalizacaoService service;
    private final Usuario joao = new Usuario("João Costa", "motorista@spacebus.local", "h", Perfil.MOTORISTA, true);

    @BeforeEach
    void setUp() {
        service = new LocalizacaoService(repository, viagemRepository, reservaRepository, auditoria, CLOCK, LocalizacaoPropriedades.padrao());
        joao.setId("m1");
        lenient().when(repository.gravarSeMaisNova(anyString(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyBoolean(), anyInt()))
                .thenReturn(1);
    }

    private void emAndamento(String motoristaId) {
        situacao("EM_ANDAMENTO", motoristaId);
    }

    private void situacao(String status, String motoristaId) {
        when(viagemRepository.travarCompartilhado("vg1")).thenReturn(Optional.of(new ViagemRepository.SituacaoViagem() {
            public String getStatus() {
                return status;
            }

            public String getMotoristaId() {
                return motoristaId;
            }
        }));
    }

    /** Hora UTC ("Z"): 12:00Z = 09:00 no fuso da aplicação. */
    private static PosicaoRequest posicao(String hora, double precisao) {
        return new PosicaoRequest(-10.18, -48.33, precisao, hora);
    }

    @Test
    void aceitaPosicaoConvertendoAHoraParaOFusoDaAplicacao() {
        emAndamento("m1");

        var resultado = service.registrar("vg1", "m1", List.of(posicao("2026-11-17T11:59:30Z", 15)));

        assertThat(resultado).isEqualTo(new ResultadoEnvioResponse(1, 0, 0));
        var registrada = LocalDateTime.of(2026, 11, 17, 8, 59, 30);
        verify(repository).garantirLinha(anyString(), eq("vg1"), eq(AGORA));
        verify(repository).gravarSeMaisNova("vg1", -10.18, -48.33, 15, registrada, registrada.minusSeconds(10), AGORA, false, 60);
        verify(repository, never()).contar(any(), anyInt(), anyInt(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void horaComOffsetDoBrasilVaiParaOMesmoInstante() {
        emAndamento("m1");

        service.registrar("vg1", "m1", List.of(posicao("2026-11-17T08:59:30-03:00", 15)));

        var registrada = LocalDateTime.of(2026, 11, 17, 8, 59, 30);
        verify(repository).gravarSeMaisNova("vg1", -10.18, -48.33, 15, registrada, registrada.minusSeconds(10), AGORA, false, 60);
    }

    @Test
    void rejeitaAdiantadaAntigaEImprecisaContandoPorMotivoSemFalharOLote() {
        emAndamento("m1");

        var resultado = service.registrar("vg1", "m1", List.of(
                posicao("2026-11-17T12:02:01Z", 10),   // 2 min e 1 s à frente do servidor
                posicao("2026-11-17T11:49:59Z", 10),   // 10 min e 1 s atrás
                posicao("2026-11-17T11:59:00Z", 201),  // imprecisa
                posicao("2026-11-17T11:59:40Z", 10)));  // boa

        assertThat(resultado).isEqualTo(new ResultadoEnvioResponse(1, 3, 0));
        verify(repository).contar("vg1", 1, 1, 1, 0, MotivoRejeicaoPosicao.ADIANTADA.name(), AGORA);
        verify(repository).gravarSeMaisNova(eq("vg1"), anyDouble(), anyDouble(), eq(10.0), eq(LocalDateTime.of(2026, 11, 17, 8, 59, 40)),
                any(), any(), anyBoolean(), anyInt());
    }

    @Test
    void limitesDeAdiantadaAntigaEPrecisaoSaoInclusivos() {
        emAndamento("m1");

        var resultado = service.registrar("vg1", "m1", List.of(
                posicao("2026-11-17T12:02:00Z", 10), posicao("2026-11-17T11:50:00Z", 10), posicao("2026-11-17T11:55:00Z", 200)));

        assertThat(resultado.rejeitadas()).isZero();
        assertThat(resultado.aceitas()).isEqualTo(3);
    }

    /** Fora de ordem ou a menos de 10 s da última: o UPDATE condicional não grava e a posição conta como ignorada. */
    @Test
    void posicaoQueOBancoNaoGravaEIgnoradaENaoFalha() {
        emAndamento("m1");
        when(repository.gravarSeMaisNova(anyString(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyBoolean(), anyInt()))
                .thenReturn(0);

        var resultado = service.registrar("vg1", "m1", List.of(posicao("2026-11-17T11:59:30Z", 10)));

        assertThat(resultado).isEqualTo(new ResultadoEnvioResponse(0, 0, 1));
        verify(repository).contar("vg1", 0, 0, 0, 1, null, AGORA);
    }

    @Test
    void gravaOLoteDaMaisAntigaParaAMaisNova() {
        emAndamento("m1");

        service.registrar("vg1", "m1", List.of(posicao("2026-11-17T11:59:40Z", 10), posicao("2026-11-17T11:58:00Z", 10)));

        var ordem = inOrder(repository);
        ordem.verify(repository).gravarSeMaisNova(any(), anyDouble(), anyDouble(), anyDouble(), eq(LocalDateTime.of(2026, 11, 17, 8, 58)),
                any(), any(), anyBoolean(), anyInt());
        ordem.verify(repository).gravarSeMaisNova(any(), anyDouble(), anyDouble(), anyDouble(), eq(LocalDateTime.of(2026, 11, 17, 8, 59, 40)),
                any(), any(), anyBoolean(), anyInt());
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"PUBLICADA", "ENCERRADA", "CANCELADA"})
    void viagemForaDeAndamentoRecusaOLoteSemGravar(StatusViagem status) {
        situacao(status.name(), "m1");

        assertThatThrownBy(() -> service.registrar("vg1", "m1", List.of(posicao("2026-11-17T11:59:30Z", 10))))
                .isInstanceOf(DomainException.class)
                .hasMessage("A viagem não está em andamento.");
        verify(repository, never()).garantirLinha(any(), any(), any());
        verify(repository, never()).gravarSeMaisNova(any(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyBoolean(), anyInt());
    }

    @Test
    void motoristaQueNaoEODaViagemOuViagemInexistenteRecusa() {
        emAndamento("m1");
        assertThatThrownBy(() -> service.registrar("vg1", "m2", List.of(posicao("2026-11-17T11:59:30Z", 10))))
                .hasMessage("A viagem não está em andamento.");

        when(viagemRepository.travarCompartilhado("vg1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrar("vg1", "m1", List.of(posicao("2026-11-17T11:59:30Z", 10))))
                .hasMessage("A viagem não está em andamento.");
        verify(repository, never()).garantirLinha(any(), any(), any());
    }

    @Test
    void horaInvalidaRecusaAntesDeTravar() {
        assertThatThrownBy(() -> service.registrar("vg1", "m1", List.of(posicao("2026-13-40T99:00:00Z", 10))))
                .hasMessage("Informe a hora da posição com fuso (ISO-8601).");
        verifyNoInteractions(viagemRepository);
    }

    @Test
    void simuladaPassaPeloMesmoCaminhoSemConferirMotoristaEMarcada() {
        emAndamento(null);

        var resultado = service.registrarSimulada("vg1", posicao("2026-11-17T11:59:30Z", 10));

        assertThat(resultado.aceitas()).isEqualTo(1);
        verify(repository).gravarSeMaisNova(eq("vg1"), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), eq(true), anyInt());
    }

    // --- Passageiro (H15) ---

    private Viagem viagem(StatusViagem status, LocalDateTime partida, Usuario motorista) {
        var viagem = new Viagem(new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of()),
                new Veiculo("SBV1A23", "Van", TipoVeiculo.VAN, 15, true), motorista, partida, new BigDecimal("18.50"), 15, status);
        viagem.setId("vg1");
        return viagem;
    }

    private void reserva(StatusReserva status, Viagem viagem) {
        var reserva = new Reserva();
        reserva.setId("rs1");
        reserva.setStatus(status);
        reserva.setViagem(viagem);
        when(reservaRepository.findByIdAndPassageiroId("rs1", "p1")).thenReturn(Optional.of(reserva));
    }

    private LocalizacaoViagem posicaoGravada(LocalDateTime registradaEm, boolean simulada) {
        var l = new LocalizacaoViagem();
        l.setViagemId("vg1");
        l.setLatitude(-10.18);
        l.setLongitude(-48.33);
        l.setPrecisaoM(30.0);
        l.setRegistradaEm(registradaEm);
        l.setRecebidaEm(registradaEm.plusSeconds(1));
        l.setSimulada(simulada);
        l.setAceitas(5);
        l.setRejeitadasImprecisa(2);
        l.setLacunas(1);
        l.setMaiorIntervaloSegundos(75);
        lenient().when(repository.findByViagemId("vg1")).thenReturn(Optional.of(l));
        lenient().when(repository.findByViagemIdIn(List.of("vg1"))).thenReturn(List.of(l));
        return l;
    }

    @Test
    void passageiroConfirmadoVeAPosicaoComHoraPrecisaoEIdadeDadaPeloServidor() {
        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.EM_ANDAMENTO, AGORA.minusMinutes(5), joao));
        posicaoGravada(AGORA.minusSeconds(120), true);

        var l = service.daReserva("rs1", "p1");

        assertThat(l.isDisponivel()).isTrue();
        assertThat(l.getLatitude()).isEqualTo(-10.18);
        assertThat(l.getPrecisaoM()).isEqualTo(30.0);
        assertThat(l.getRegistradaEm()).isEqualTo(AGORA.minusSeconds(120));
        assertThat(l.getSegundosDesdeAtualizacao()).isEqualTo(120);
        assertThat(l.isDesatualizada()).as("exatamente 2 min ainda não é desatualizada").isFalse();
        assertThat(l.isSimulada()).isTrue();
        assertThat(l.getSituacaoViagem()).isEqualTo(StatusViagem.EM_ANDAMENTO);
    }

    @Test
    void maisDeDoisMinutosSemPosicaoEDesatualizadaMasContinuaComAPosicao() {
        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.EM_ANDAMENTO, AGORA.minusMinutes(5), joao));
        posicaoGravada(AGORA.minusSeconds(121), false);

        var l = service.daReserva("rs1", "p1");

        assertThat(l.isDesatualizada()).isTrue();
        assertThat(l.isDisponivel()).isTrue();
        assertThat(l.getLatitude()).isNotNull();
    }

    @Test
    void aparelhoUmPoucoAdiantadoNaoGeraIdadeNegativa() {
        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.EM_ANDAMENTO, AGORA, joao));
        posicaoGravada(AGORA.plusSeconds(30), false);

        assertThat(service.daReserva("rs1", "p1").getSegundosDesdeAtualizacao()).isZero();
    }

    @Test
    void emAndamentoSemNenhumaPosicaoEIndisponivelSemPosicaoInventada() {
        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.EM_ANDAMENTO, AGORA, joao));
        when(repository.findByViagemId("vg1")).thenReturn(Optional.empty());

        var l = service.daReserva("rs1", "p1");

        assertThat(l.isDisponivel()).isFalse();
        assertThat(l.getLatitude()).isNull();
        assertThat(l.getRegistradaEm()).isNull();
    }

    @Test
    void encerradaNaoMostraPosicaoMesmoComCoordenadasGuardadas() {
        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.ENCERRADA, AGORA.minusHours(1), joao));
        posicaoGravada(AGORA.minusMinutes(10), false);

        var l = service.daReserva("rs1", "p1");

        assertThat(l.getSituacaoViagem()).isEqualTo(StatusViagem.ENCERRADA);
        assertThat(l.isDisponivel()).isFalse();
        assertThat(l.getLatitude()).isNull();
    }

    @Test
    void publicadaAguardaInicioSoDentroDaJanela() {
        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.PUBLICADA, AGORA.plusMinutes(30), joao));
        assertThat(service.daReserva("rs1", "p1").isAguardandoInicio()).isTrue();

        reserva(StatusReserva.CONFIRMADA, viagem(StatusViagem.PUBLICADA, AGORA.plusMinutes(31), joao));
        assertThat(service.daReserva("rs1", "p1").isAguardandoInicio()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"AGUARDANDO_PAGAMENTO", "EXPIRADA", "CANCELADA"})
    void soReservaConfirmadaVeALocalizacao(StatusReserva status) {
        reserva(status, viagem(StatusViagem.EM_ANDAMENTO, AGORA, joao));

        assertThatThrownBy(() -> service.daReserva("rs1", "p1")).hasMessage("Localização disponível só para reservas confirmadas.");
        verifyNoInteractions(repository);
    }

    @Test
    void reservaDeOutroPassageiroNaoEEncontrada() {
        when(reservaRepository.findByIdAndPassageiroId("rs1", "p2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.daReserva("rs1", "p2")).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository);
    }

    // --- Operação (H16) ---

    @Test
    void operacaoVeOMesmoQueOPassageiroMaisODiagnosticoSemAuditoriaDoOperador() {
        var viagem = viagem(StatusViagem.EM_ANDAMENTO, AGORA.minusMinutes(5), joao);
        viagem.setIniciadaEm(AGORA.minusMinutes(20));
        when(viagemRepository.findById("vg1")).thenReturn(Optional.of(viagem));
        posicaoGravada(AGORA.minusSeconds(30), false);

        var r = service.daViagem("vg1", "op1", Perfil.OPERADOR);

        assertThat(r.getLocalizacao().isDisponivel()).isTrue();
        assertThat(r.isSemMotorista()).isFalse();
        assertThat(r.getDiagnostico().getIniciadaEm()).isEqualTo(AGORA.minusMinutes(20));
        assertThat(r.getDiagnostico().getAceitas()).isEqualTo(5);
        assertThat(r.getDiagnostico().getRejeitadasImprecisa()).isEqualTo(2);
        assertThat(r.getDiagnostico().getLacunas()).isEqualTo(1);
        assertThat(r.getDiagnostico().getMaiorIntervaloSegundos()).isEqualTo(75);
        assertThat(r.getDiagnostico().getUltimaRecebidaEm()).isEqualTo(AGORA.minusSeconds(29));
        verifyNoInteractions(auditoria);
    }

    @Test
    void consultaDoAdministradorEAuditadaNoMaximoUmaVezPorHora() {
        when(viagemRepository.findById("vg1")).thenReturn(Optional.of(viagem(StatusViagem.EM_ANDAMENTO, AGORA, null)));

        var r = service.daViagem("vg1", "adm1", Perfil.ADMINISTRADOR);

        assertThat(r.isSemMotorista()).isTrue();
        verify(auditoria).registrarUmaVezPorHora("adm1", Perfil.ADMINISTRADOR, AcaoAuditada.CONSULTA_LOCALIZACAO_VIAGEM, "vg1", AGORA);
    }

    @Test
    void viagemInexistenteNaOperacaoNaoAudita() {
        when(viagemRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.daViagem("x", "adm1", Perfil.ADMINISTRADOR)).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(auditoria);
    }

    @Test
    void semSinalSoParaEmAndamentoDesatualizadaOuSemPosicao() {
        var comSinal = viagem(StatusViagem.EM_ANDAMENTO, AGORA, joao);
        var desatualizada = viagem(StatusViagem.EM_ANDAMENTO, AGORA, joao);
        desatualizada.setId("vg2");
        var semPosicao = viagem(StatusViagem.EM_ANDAMENTO, AGORA, joao);
        semPosicao.setId("vg3");
        var publicada = viagem(StatusViagem.PUBLICADA, AGORA, joao);
        publicada.setId("vg4");
        var atual = new LocalizacaoViagem();
        atual.setViagemId("vg1");
        atual.setLatitude(1.0);
        atual.setLongitude(1.0);
        atual.setRegistradaEm(AGORA.minusSeconds(20));
        var velha = new LocalizacaoViagem();
        velha.setViagemId("vg2");
        velha.setLatitude(1.0);
        velha.setLongitude(1.0);
        velha.setRegistradaEm(AGORA.minusMinutes(3));
        when(repository.findByViagemIdIn(List.of("vg1", "vg2", "vg3"))).thenReturn(List.of(atual, velha));

        assertThat(service.semSinal(List.of(comSinal, desatualizada, semPosicao, publicada))).containsExactlyInAnyOrder("vg2", "vg3");
    }

    @Test
    void semViagemEmAndamentoNaoConsultaLocalizacao() {
        assertThat(service.semSinal(List.of(viagem(StatusViagem.PUBLICADA, AGORA, joao)))).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void expurgoApagaCoordenadas24HorasAposOEncerramento() {
        when(repository.expurgarEncerradasAte(AGORA.minusHours(24), AGORA)).thenReturn(3);

        assertThat(service.expurgar()).isEqualTo(3);
    }
}
