package com.spacebus.ops.domain.service.busca;

import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.busca.BuscaViagemRequest;
import com.spacebus.ops.domain.model.busca.ViagemEncontradaResponse;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
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
 * Ressalva D9 do módulo 2: a busca, a lista de locais e a agenda do operador fazem o mesmo número de consultas com 2 ou com 6 viagens
 * (rota, pontos e veículo vêm junto; os lugares de todas as viagens numa consulta só).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.liquibase.contexts=teste",
        "spacebus.agendamentos.ativos=false",
        "spacebus.pagamento.provedor=simulado",
        "spacebus.pagamento.webhook-segredo=teste",
        "spring.jpa.properties.hibernate.generate_statistics=true",
})
@ActiveProfiles("teste")
class BuscaViagemConsultasIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private IBuscaViagemService busca;
    @Autowired private IReservaService reservas;
    @Autowired private IViagemService viagens;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private RotaRepository rotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory emf;

    private final LocalDate amanha = LocalDate.now().plusDays(1);
    private Rota rota;
    private Veiculo van;
    private String passageiroId;
    private int minuto;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from reserva_historico");
        jdbc.update("delete from pagamento");
        jdbc.update("delete from reserva");
        jdbc.update("delete from viagem");
        jdbc.update("delete from rota");
        jdbc.update("delete from veiculo");
        var pontos = List.of(ponto("Terminal Central", 1), ponto("Praça", 2), ponto("Hospital", 3), ponto("Aeroporto", 4));
        rota = rotaRepository.save(new Rota("Centro – Aeroporto", "Centro", "Aeroporto", pontos));
        van = veiculoRepository.save(new Veiculo("TST1A23", "Van", TipoVeiculo.VAN, 15, true));
        passageiroId = usuarioRepository.save(new Usuario("Paula", "p-" + UUID.randomUUID() + "@teste", "h", Perfil.PASSAGEIRO, true)).getId();
    }

    private static PontoParada ponto(String nome, int ordem) {
        var p = new PontoParada(nome, null);
        p.setOrdem(ordem);
        return p;
    }

    /** Viagens publicadas amanhã, cada uma com uma reserva de 2 lugares. */
    private void viagensComReserva(int quantas) {
        for (int i = 0; i < quantas; i++) {
            var viagem = viagemRepository.save(new Viagem(rota, van, null, amanha.atTime(6, 0).plusMinutes(minuto++ * 10L),
                    new BigDecimal("18.50"), 10, StatusViagem.PUBLICADA));
            reservas.reservar(passageiroId, new ReservaRequest(viagem.getId(), 1, 4, 2));
        }
    }

    private Statistics estatisticas() {
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }

    private long consultasDaBusca() {
        var stats = estatisticas();
        busca.buscar(new BuscaViagemRequest("Terminal", "Aeroporto", amanha));
        return stats.getPrepareStatementCount();
    }

    private long consultasDosLocais() {
        var stats = estatisticas();
        busca.locais();
        return stats.getPrepareStatementCount();
    }

    @Test
    void buscaNaoFazUmaConsultaPorViagem() {
        viagensComReserva(2);
        var comDuas = consultasDaBusca();
        viagensComReserva(4);
        var comSeis = consultasDaBusca();

        assertThat(comSeis).isEqualTo(comDuas);
        assertThat(comDuas).isLessThanOrEqualTo(2);
    }

    @Test
    void locaisNaoFazUmaConsultaPorViagem() {
        viagensComReserva(2);
        var comDuas = consultasDosLocais();
        viagensComReserva(4);

        assertThat(consultasDosLocais()).isEqualTo(comDuas).isEqualTo(1);
    }

    @Test
    void agendaDoOperadorNaoFazUmaConsultaPorViagem() {
        viagensComReserva(2);
        var stats = estatisticas();
        viagens.agenda(amanha, null, null, org.springframework.data.domain.PageRequest.of(0, 100));
        var comDuas = stats.getPrepareStatementCount();
        viagensComReserva(4);
        stats = estatisticas();
        var pagina = viagens.agenda(amanha, null, null, org.springframework.data.domain.PageRequest.of(0, 100));

        assertThat(stats.getPrepareStatementCount()).isEqualTo(comDuas);
        assertThat(pagina.getTotalElements()).isEqualTo(6);
    }

    @Test
    void lugaresDisponiveisContinuamCertos() {
        viagensComReserva(3);

        var encontradas = busca.buscar(new BuscaViagemRequest("Terminal", "Aeroporto", amanha));

        assertThat(encontradas).hasSize(3).extracting(ViagemEncontradaResponse::getLugaresDisponiveis).containsOnly(8);
        assertThat(encontradas).extracting(ViagemEncontradaResponse::getPartida).isSorted();
        assertThat(busca.locais()).containsExactly("Aeroporto", "Centro", "Hospital", "Praça", "Terminal Central");
    }

    @Test
    void viagemSemReservaTemTodosOsLugares() {
        viagemRepository.save(new Viagem(rota, van, null, LocalDateTime.of(amanha, java.time.LocalTime.of(5, 0)),
                new BigDecimal("18.50"), 12, StatusViagem.PUBLICADA));

        assertThat(busca.buscar(new BuscaViagemRequest("Terminal", "Aeroporto", amanha)))
                .singleElement().extracting(ViagemEncontradaResponse::getLugaresDisponiveis).isEqualTo(12);
    }
}
