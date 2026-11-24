package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.localizacao.PosicaoRequest;
import com.spacebus.ops.domain.repository.localizacao.LocalizacaoViagemRepository;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Concorrência da localização com PostgreSQL real (H13, H14): posições simultâneas fora de ordem, posição × encerrar,
 * inícios simultâneos (mesma viagem; mesmo motorista em viagens diferentes) e encerramento automático × manual.
 * Sem Docker, a classe é pulada.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.liquibase.contexts=teste",
        "spacebus.agendamentos.ativos=false",
        "spacebus.pagamento.provedor=simulado",
        "spacebus.pagamento.webhook-segredo=teste",
})
@ActiveProfiles("teste")
class LocalizacaoConcorrenciaIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private IViagemService viagens;
    @Autowired private ILocalizacaoService localizacao;
    @Autowired private LocalizacaoViagemRepository localizacaoRepository;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private RotaRepository rotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TransactionTemplate transacao;
    @Autowired private JdbcTemplate jdbc;

    private String motoristaId;
    private String operadorId;
    private Rota rota;
    private Usuario motorista;

    @BeforeEach
    void setUp() {
        var sufixo = UUID.randomUUID().toString().substring(0, 8);
        motorista = usuarioRepository.save(new Usuario("João", "m-" + sufixo + "@teste", "h", Perfil.MOTORISTA, true));
        motoristaId = motorista.getId();
        operadorId = usuarioRepository.save(new Usuario("Ana", "o-" + sufixo + "@teste", "h", Perfil.OPERADOR, true)).getId();
        rota = rotaRepository.save(new Rota("Rota " + sufixo, "A", "B", List.of(ponto("A", 1), ponto("B", 2))));
    }

    private static PontoParada ponto(String nome, int ordem) {
        var p = new PontoParada(nome, null);
        p.setOrdem(ordem);
        return p;
    }

    /** Viagem publicada daqui a 10 min (dentro da janela de início), com van própria. */
    private String viagemPublicada() {
        var placa = "L" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        var van = veiculoRepository.save(new Veiculo(placa, "Van", TipoVeiculo.VAN, 15, true));
        var partida = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MINUTES);
        return viagemRepository.save(new Viagem(rota, van, motorista, partida, new BigDecimal("15.00"), 10, StatusViagem.PUBLICADA)).getId();
    }

    private String viagemEmAndamento() {
        var id = viagemPublicada();
        viagens.iniciar(id, motoristaId, motoristaId);
        return id;
    }

    private static PosicaoRequest posicao(OffsetDateTime hora) {
        return new PosicaoRequest(-10.18, -48.33, 10.0, hora.toString());
    }

    private static <T> List<T> simultaneos(int quantidade, java.util.function.IntFunction<Supplier<T>> tarefa) {
        var largada = new CountDownLatch(1);
        var futuros = IntStream.range(0, quantidade).mapToObj(i -> {
            var acao = tarefa.apply(i);
            return CompletableFuture.supplyAsync(() -> {
                aguardar(largada);
                return acao.get();
            });
        }).toList();
        largada.countDown();
        return futuros.stream().map(CompletableFuture::join).toList();
    }

    private static String mensagem(Runnable acao) {
        try {
            acao.run();
            return "ok";
        } catch (DomainException e) {
            return e.getMessage();
        }
    }

    @Test
    void posicoesSimultaneasForaDeOrdemFicaADeMaiorHora() {
        var viagemId = viagemEmAndamento();
        var base = OffsetDateTime.now().minusMinutes(5).truncatedTo(ChronoUnit.SECONDS);
        var horas = new ArrayList<>(IntStream.range(0, 12).mapToObj(i -> base.plusSeconds(15L * i)).toList());
        Collections.shuffle(horas);

        simultaneos(horas.size(), i -> () -> localizacao.registrar(viagemId, motoristaId, List.of(posicao(horas.get(i)))));

        var gravada = localizacaoRepository.findByViagemId(viagemId).orElseThrow();
        var maior = base.plusSeconds(15L * 11).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
        assertThat(gravada.getRegistradaEm()).isEqualTo(maior);
        assertThat(gravada.getAceitas() + gravada.getIgnoradas()).isEqualTo(12);
    }

    @Test
    void posicaoConcorrenteComOEncerramentoNaoEGravadaDepoisDele() throws Exception {
        var viagemId = viagemEmAndamento();
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var encerramento = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(s -> {
            viagemRepository.travar(viagemId);
            travou.countDown();
            aguardar(liberar);
            viagens.encerrar(viagemId, operadorId, null);
        }));
        assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();
        var envio = CompletableFuture.supplyAsync(() ->
                mensagem(() -> localizacao.registrar(viagemId, motoristaId, List.of(posicao(OffsetDateTime.now())))));
        esperarAlguemBloqueado();
        liberar.countDown();
        encerramento.get(10, TimeUnit.SECONDS);

        assertThat(envio.get(10, TimeUnit.SECONDS)).isEqualTo("A viagem não está em andamento.");
        assertThat(localizacaoRepository.findByViagemId(viagemId)).isEmpty();
    }

    @Test
    void doisToquesSimultaneosEmIniciarFazemUmaTransicao() {
        var viagemId = viagemPublicada();

        var resultados = simultaneos(2, i -> () -> mensagem(() -> viagens.iniciar(viagemId, i == 0 ? motoristaId : operadorId,
                i == 0 ? motoristaId : null)));

        assertThat(resultados).containsExactlyInAnyOrder("ok", "A viagem já está em andamento.");
        assertThat(viagemRepository.findById(viagemId).orElseThrow().getStatus()).isEqualTo(StatusViagem.EM_ANDAMENTO);
    }

    @Test
    void doisIniciosSimultaneosDoMesmoMotoristaEmViagensDiferentesSoUmPassa() {
        var viagemIds = List.of(viagemPublicada(), viagemPublicada());

        var resultados = simultaneos(2, i -> () -> mensagem(() -> viagens.iniciar(viagemIds.get(i), motoristaId, motoristaId)));

        assertThat(resultados).containsExactlyInAnyOrder("ok", "Encerre a viagem em andamento antes de iniciar outra.");
        var emAndamento = jdbc.queryForObject("select count(*) from viagem where motorista_id = ? and status = 'EM_ANDAMENTO'",
                Integer.class, motoristaId);
        assertThat(emAndamento).isEqualTo(1);
    }

    @Test
    void indiceUnicoImpedeDuasViagensEmAndamentoDoMesmoMotoristaMesmoSemAConferencia() {
        viagemEmAndamento();
        var outra = viagemPublicada();

        assertThatThrownBy(() -> jdbc.update("update viagem set status = 'EM_ANDAMENTO' where id = ?", outra))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_viagem_motorista_em_andamento");
    }

    /** QA D1: consultas simultâneas do Administrador gravam um registro por hora; na hora seguinte, outro. */
    @Test
    void consultasSimultaneasDoAdministradorGravamUmaAuditoriaPorHora() {
        var viagemId = viagemEmAndamento();
        var admin = usuarioRepository.save(new Usuario("Rafael", "a-" + UUID.randomUUID() + "@teste", "h", Perfil.ADMINISTRADOR, true)).getId();

        simultaneos(3, i -> () -> localizacao.daViagem(viagemId, admin, Perfil.ADMINISTRADOR));
        var contar = (java.util.function.Supplier<Integer>) () -> jdbc.queryForObject(
                "select count(*) from auditoria_acesso where usuario_id = ? and recurso_id = ? and acao = 'CONSULTA_LOCALIZACAO_VIAGEM'",
                Integer.class, admin, viagemId);
        assertThat(contar.get()).isEqualTo(1);

        jdbc.update("update auditoria_acesso set data_criacao = data_criacao - interval '1 hour' where usuario_id = ?", admin);
        localizacao.daViagem(viagemId, admin, Perfil.ADMINISTRADOR);
        assertThat(contar.get()).isEqualTo(2);
    }

    @Test
    void encerramentoAutomaticoNaoBrigaComOManual() throws Exception {
        var viagemId = viagemEmAndamento();
        jdbc.update("update viagem set iniciada_em = ? where id = ?", LocalDateTime.now().minusHours(7), viagemId);
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var manual = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(s -> {
            viagens.encerrar(viagemId, operadorId, null);
            travou.countDown();
            aguardar(liberar);
        }));
        assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();

        var automaticas = viagens.encerrarEsquecidas(); // SKIP LOCKED: não espera a manual
        liberar.countDown();
        manual.get(10, TimeUnit.SECONDS);

        assertThat(automaticas).isZero();
        assertThat(viagemRepository.findById(viagemId).orElseThrow().getEncerradaPor()).isEqualTo(operadorId);
        assertThat(viagens.encerrarEsquecidas()).isZero();
    }

    @Test
    void encerramentoAutomaticoEncerraSoAsEsquecidasSemResponsavel() {
        var esquecida = viagemEmAndamento();
        jdbc.update("update viagem set iniciada_em = ? where id = ?", LocalDateTime.now().minusHours(7), esquecida);
        motorista = usuarioRepository.save(new Usuario("Caio", "c-" + UUID.randomUUID() + "@teste", "h", Perfil.MOTORISTA, true));
        motoristaId = motorista.getId();
        var recente = viagemEmAndamento();

        assertThat(viagens.encerrarEsquecidas()).isEqualTo(1);

        var encerrada = viagemRepository.findById(esquecida).orElseThrow();
        assertThat(encerrada.getStatus()).isEqualTo(StatusViagem.ENCERRADA);
        assertThat(encerrada.getEncerradaEm()).isNotNull();
        assertThat(encerrada.getEncerradaPor()).isNull();
        assertThat(viagemRepository.findById(recente).orElseThrow().getStatus()).isEqualTo(StatusViagem.EM_ANDAMENTO);
    }

    @Test
    void contadoresLacunasEExpurgoNoBancoReal() {
        var viagemId = viagemEmAndamento();
        var base = OffsetDateTime.now().minusMinutes(6).truncatedTo(ChronoUnit.SECONDS);

        var r = localizacao.registrar(viagemId, motoristaId, List.of(
                posicao(base), posicao(base.plusSeconds(5)), posicao(base.plusSeconds(90)), posicao(base.plusSeconds(100)),
                new PosicaoRequest(-10.0, -48.0, 500.0, base.plusSeconds(120).toString())));

        assertThat(r.aceitas()).isEqualTo(3);
        assertThat(r.ignoradas()).isEqualTo(1);
        assertThat(r.rejeitadas()).isEqualTo(1);
        var l = localizacaoRepository.findByViagemId(viagemId).orElseThrow();
        assertThat(l.getLacunas()).isEqualTo(1);
        assertThat(l.getMaiorIntervaloSegundos()).isEqualTo(90);
        assertThat(l.getRejeitadasImprecisa()).isEqualTo(1);
        assertThat(l.getUltimoMotivoRejeicao()).isNotNull();

        viagens.encerrar(viagemId, motoristaId, motoristaId);
        jdbc.update("update viagem set encerrada_em = ? where id = ?", LocalDateTime.now().minusHours(25), viagemId);
        assertThat(localizacao.expurgar()).isEqualTo(1);

        var expurgada = localizacaoRepository.findByViagemId(viagemId).orElseThrow();
        assertThat(expurgada.getLatitude()).isNull();
        assertThat(expurgada.getLongitude()).isNull();
        assertThat(expurgada.getAceitas()).isEqualTo(3);
        assertThat(expurgada.getRegistradaEm()).isNotNull();
    }

    private void esperarAlguemBloqueado() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            var bloqueados = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and datname = current_database()", Integer.class);
            if (bloqueados != null && bloqueados > 0) return;
            Thread.sleep(50);
        }
        throw new AssertionError("O concorrente não chegou a esperar a trava da viagem.");
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Tempo esgotado.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
