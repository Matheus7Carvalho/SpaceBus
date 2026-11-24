package com.spacebus.ops.domain.service.indicador;

import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.indicador.IndicadorViagemResponse;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse.Horario;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Indicadores com PostgreSQL real e dados conhecidos (H21, H22): vendidos só confirmadas, ocupação ponderada,
 * rascunho e cancelada fora, bloqueio vigente nos disponíveis, limites do período e consultas constantes (sem N+1).
 * Cada teste usa a própria rota e filtra por ela. Sem Docker, a classe é pulada.
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
class IndicadorIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private IIndicadorService indicadores;
    @Autowired private IReservaService reservas;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private RotaRepository rotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private NamedParameterJdbcTemplate namedJdbc;

    private static final LocalDate DIA = LocalDate.now().plusDays(3);
    private String passageiroId;
    private Rota rota;

    @BeforeEach
    void setUp() {
        passageiroId = usuarioRepository.save(new Usuario("Paula", "p-" + UUID.randomUUID() + "@teste", "h", Perfil.PASSAGEIRO, true)).getId();
        var a = new PontoParada("A", null);
        a.setOrdem(1);
        var b = new PontoParada("B", null);
        b.setOrdem(2);
        rota = rotaRepository.save(new Rota("Rota " + UUID.randomUUID().toString().substring(0, 8), "A", "B", List.of(a, b)));
    }

    private String viagem(LocalDateTime partida, int capacidade) {
        var van = veiculoRepository.save(new Veiculo("I" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(),
                "Van", TipoVeiculo.VAN, 15, true));
        return viagemRepository.save(new Viagem(rota, van, null, partida, new BigDecimal("15.00"), capacidade, StatusViagem.PUBLICADA)).getId();
    }

    /** Reserva pela regra real (publicada, com vaga) e depois a situação desejada. */
    private void reserva(String viagemId, int lugares, String status) {
        var r = reservas.reservar(passageiroId, new ReservaRequest(viagemId, 1, 2, lugares));
        if (!"AGUARDANDO_PAGAMENTO".equals(status)) jdbc.update("update reserva set status = ? where id = ?", status, r.getId());
    }

    private void situacao(String viagemId, String status) {
        jdbc.update("update viagem set status = ? where id = ?", status, viagemId);
    }

    private String dia(LocalDate d) {
        return d.toString();
    }

    @Test
    void exemploDoDocNumerosDoResumoPorHorarioEPorViagem() {
        var as0730 = DIA.atTime(7, 30);
        var a = viagem(as0730, 15);
        reserva(a, 6, "CONFIRMADA");
        reserva(a, 1, "CONFIRMADA");
        reserva(a, 1, "AGUARDANDO_PAGAMENTO"); // bloqueio vigente: não vende, mas ocupa a vaga
        var b = viagem(as0730, 15);
        reserva(b, 5, "CONFIRMADA");
        reserva(b, 2, "CANCELADA");
        situacao(b, "ENCERRADA");
        var lotada = viagem(DIA.atTime(18, 0), 10);
        reserva(lotada, 6, "CONFIRMADA");
        reserva(lotada, 4, "CONFIRMADA");
        var cancelada = viagem(as0730, 15);
        reserva(cancelada, 3, "CANCELADA");
        situacao(cancelada, "CANCELADA");
        var rascunho = viagem(as0730, 15);
        situacao(rascunho, "RASCUNHO");

        var r = indicadores.resumo(dia(DIA), dia(DIA), rota.getId());

        assertThat(r.viagens()).extracting("total", "publicadas", "realizadas", "canceladas", "comReserva").containsExactly(4L, 2L, 1L, 1L, 3L);
        assertThat(r.vendidos()).isEqualTo(22);
        assertThat(r.oferecidos()).isEqualTo(40);
        assertThat(r.ocupacao()).isEqualTo(0.55);
        assertThat(r.receita()).isEqualByComparingTo("330.00");
        assertThat(r.horarios()).containsExactly(new Horario("07:30", 2, 30, 12, 0.4), new Horario("18:00", 1, 10, 10, 1.0));

        var pagina = indicadores.porViagem(dia(DIA), dia(DIA), rota.getId(), 0);
        assertThat(pagina.getTotalElements()).isEqualTo(r.viagens().total());
        var porId = pagina.getContent().stream().collect(java.util.stream.Collectors.toMap(IndicadorViagemResponse::id, v -> v));
        assertThat(porId.get(lotada)).extracting("vendidos", "disponiveis", "ocupacao").containsExactly(10L, 0L, 1.0);
        assertThat(porId.get(a)).extracting("vendidos", "disponiveis").containsExactly(7L, 7L);
        assertThat(porId.get(cancelada)).extracting("disponiveis", "ocupacao").containsExactly(null, null);
        assertThat(porId).doesNotContainKey(rascunho);
        assertThat(pagina.getContent().get(0).id()).isEqualTo(lotada); // mais recente primeiro
    }

    @Test
    void limitesDoPeriodoEBloqueioVencido() {
        var meiaNoite = viagem(DIA.atStartOfDay(), 10);
        var fimDoDia = viagem(DIA.plusDays(1).atTime(23, 59), 10);
        viagem(DIA.minusDays(1).atTime(23, 59), 10);
        viagem(DIA.plusDays(2).atStartOfDay(), 10);
        reserva(meiaNoite, 2, "AGUARDANDO_PAGAMENTO");
        jdbc.update("update reserva set expira_em = ? where viagem_id = ?", LocalDateTime.now().minusMinutes(1), meiaNoite);

        var pagina = indicadores.porViagem(dia(DIA), dia(DIA.plusDays(1)), rota.getId(), 0);

        assertThat(pagina.getContent()).extracting(IndicadorViagemResponse::id).containsExactly(fimDoDia, meiaNoite);
        assertThat(pagina.getContent().get(1).disponiveis()).as("bloqueio vencido não segura vaga").isEqualTo(10L);
    }

    /** Sem N+1: o número de consultas por chamada não cresce com o número de viagens. */
    @Test
    void numeroDeConsultasNaoDependeDoNumeroDeViagens() {
        viagem(DIA.atTime(8, 0), 10);
        var consultasCom1 = consultasDe(() -> {
            indicadores.resumo(dia(DIA), dia(DIA), rota.getId());
            indicadores.porViagem(dia(DIA), dia(DIA), rota.getId(), 0);
        });
        for (var h = 9; h < 15; h++) reserva(viagem(DIA.atTime(h, 0), 10), 1, "CONFIRMADA");
        var consultasCom7 = consultasDe(() -> {
            indicadores.resumo(dia(DIA), dia(DIA), rota.getId());
            indicadores.porViagem(dia(DIA), dia(DIA), rota.getId(), 0);
        });

        // Constante: resumo (totais + horários) e página (linhas + contagem); o queryForObject passa pelo query do próprio template.
        assertThat(consultasCom7).isEqualTo(consultasCom1).isLessThanOrEqualTo(6);
    }

    private long consultasDe(Runnable acao) {
        Mockito.clearInvocations(namedJdbc);
        acao.run();
        return Mockito.mockingDetails(namedJdbc).getInvocations().stream()
                .filter(i -> i.getMethod().getName().startsWith("query"))
                .count();
    }
}
