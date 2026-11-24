package com.spacebus.ops.domain.service.limpeza;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Limpeza dos dados de E2E (D8 do módulo 2) contra PostgreSQL real: apaga só o que é de teste, com as dependências. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.liquibase.contexts=teste",
        "spacebus.agendamentos.ativos=false",
        "spacebus.pagamento.provedor=simulado",
        "spacebus.pagamento.webhook-segredo=teste",
})
@ActiveProfiles("teste")
class LimpezaDadosTesteIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private NamedParameterJdbcTemplate namedJdbc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private IReservaService reservas;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagamentoRepository pagamentoRepository;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private RotaRepository rotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private LimpezaDadosTesteService limpeza;
    private String passageiroId;
    private int placa = 10;

    @BeforeEach
    void setUp() {
        limpeza = new LimpezaDadosTesteService(namedJdbc);
        jdbc.update("delete from reserva_historico");
        jdbc.update("delete from pagamento");
        jdbc.update("delete from reserva");
        jdbc.update("delete from viagem");
        jdbc.update("delete from rota");
        jdbc.update("delete from veiculo");
        passageiroId = usuarioRepository.save(new Usuario("Paula", "p-" + UUID.randomUUID() + "@teste", "h", Perfil.PASSAGEIRO, true)).getId();
    }

    private Rota rota(String nome) {
        var a = new PontoParada("A", null);
        a.setOrdem(1);
        var b = new PontoParada("B", null);
        b.setOrdem(2);
        return rotaRepository.save(new Rota(nome, "A", "B", List.of(a, b)));
    }

    private Veiculo veiculo(String descricao) {
        return veiculoRepository.save(new Veiculo("TST1A" + placa++, descricao, TipoVeiculo.VAN, 15, true));
    }

    private Viagem viagem(Rota rota, Veiculo veiculo, StatusViagem status) {
        return viagemRepository.save(new Viagem(rota, veiculo, null, LocalDateTime.now().plusDays(5).plusMinutes(placa++),
                new BigDecimal("15.00"), 10, status));
    }

    /** Reserva com pagamento e histórico, para provar que as dependências também saem. */
    private void reservaPaga(Viagem viagem) {
        var reserva = reservas.reservar(passageiroId, new ReservaRequest(viagem.getId(), 1, 2, 1));
        pagamentoRepository.save(new Pagamento(reserva, "sim_" + UUID.randomUUID().toString().replace("-", ""), "PIX_SIMULADO",
                reserva.getValorTotal(), StatusPagamento.APROVADO, "/x"));
    }

    @Test
    void apagaSoOsDadosDeTesteComAsDependencias() {
        var rotaReal = rota("Centro – Aeroporto");
        var vanReal = veiculo("Van Master");
        var rotaTeste = rota("E2E-ABC Rota");
        var vanTeste = veiculo("E2E-ABC Van");
        var publicadaReal = viagem(rotaReal, vanReal, StatusViagem.PUBLICADA);
        reservaPaga(publicadaReal);
        reservaPaga(viagem(rotaTeste, vanReal, StatusViagem.PUBLICADA));
        viagem(rotaReal, vanTeste, StatusViagem.RASCUNHO);
        var canceladaDeTeste = viagem(rotaReal, vanTeste, StatusViagem.PUBLICADA);
        reservaPaga(canceladaDeTeste);
        jdbc.update("update viagem set status = 'CANCELADA' where id = ?", canceladaDeTeste.getId());

        var resultado = limpeza.limpar("E2E");

        assertThat(resultado).isEqualTo(new LimpezaDadosTesteService.Resultado(3, 1, 1));
        assertThat(viagemRepository.findAll()).extracting(Viagem::getId).containsExactly(publicadaReal.getId());
        assertThat(rotaRepository.findAll()).extracting(Rota::getNome).containsExactly("Centro – Aeroporto");
        assertThat(veiculoRepository.findAll()).extracting(Veiculo::getDescricao).containsExactly("Van Master");
        assertThat(reservaRepository.count()).isEqualTo(1);
        assertThat(pagamentoRepository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reserva_historico", Integer.class)).isEqualTo(1);
    }

    /** QA R1: viagem cancelada pelo operador, sem a marca, nunca é apagada (nem a gerada por rotina, que voltaria). */
    @Test
    void viagemCanceladaSemAMarcaFica() {
        var rota = rota("Linha Bairro");
        var van = veiculo("Van Master");
        var cancelada = viagem(rota, van, StatusViagem.PUBLICADA);
        reservaPaga(cancelada);
        jdbc.update("update viagem set status = 'CANCELADA' where id = ?", cancelada.getId());

        assertThat(limpeza.limpar("E2E")).isEqualTo(new LimpezaDadosTesteService.Resultado(0, 0, 0));
        assertThat(viagemRepository.count()).isEqualTo(1);
        assertThat(reservaRepository.count()).isEqualTo(1);
        assertThat(pagamentoRepository.count()).isEqualTo(1);
    }

    @Test
    void recusaMarcaQueNaoEDeTeste() {
        rota("Centro – Aeroporto");

        for (var marca : new String[]{null, "", "Centro", "%", "E2E%", "e2e"}) {
            assertThatThrownBy(() -> limpeza.limpar(marca)).isInstanceOf(DomainException.class)
                    .hasMessage("Informe uma marca de teste (ex.: E2E).");
        }
        assertThat(rotaRepository.count()).isEqualTo(1);
    }
}
