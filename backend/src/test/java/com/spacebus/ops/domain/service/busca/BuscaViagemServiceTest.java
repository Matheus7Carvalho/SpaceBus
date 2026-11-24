package com.spacebus.ops.domain.service.busca;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.busca.BuscaViagemRequest;
import com.spacebus.ops.domain.model.busca.ViagemEncontradaResponse;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscaViagemServiceTest {

    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Araguaina"));
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);
    private static final LocalDate AMANHA = LocalDate.of(2026, 11, 18);

    @Mock private ViagemRepository repository;
    @Mock private IReservaService reservas;

    private BuscaViagemService service;
    private Rota rota;

    @BeforeEach
    void setUp() {
        service = new BuscaViagemService(repository, reservas, CLOCK, "Transportadora piloto");
        lenient().when(reservas.lugaresDisponiveis(any(Viagem.class))).thenAnswer(inv -> ((Viagem) inv.getArgument(0)).getCapacidade());
        lenient().when(reservas.lugaresDisponiveis(org.mockito.ArgumentMatchers.<java.util.Collection<Viagem>>any())).thenAnswer(inv -> {
            java.util.Collection<Viagem> viagens = inv.getArgument(0);
            return viagens.stream().collect(java.util.stream.Collectors.toMap(Viagem::getId, Viagem::getCapacidade));
        });
        var pontos = new ArrayList<PontoParada>();
        String[][] dados = {{"Terminal Central", "Plataforma 3"}, {"Praça Principal", null}, {"Shopping", "Portão B"}, {"Aeroporto", "Desembarque"}};
        for (int i = 0; i < dados.length; i++) {
            var p = new PontoParada(dados[i][0], dados[i][1]);
            p.setId("p" + (i + 1));
            p.setOrdem(i + 1);
            pontos.add(p);
        }
        rota = new Rota("Centro – Aeroporto", "Centro", "Aeroporto", pontos);
        rota.setId("r1");
    }

    private Viagem viagem(String id, LocalDateTime partida) {
        var van = new Veiculo("SBV1A23", "Van Sprinter", TipoVeiculo.VAN, 15, true);
        var v = new Viagem(rota, van, null, partida, new BigDecimal("18.50"), 15, StatusViagem.PUBLICADA);
        v.setId(id);
        v.setDataCriacao(AGORA.minusDays(2));
        return v;
    }

    private List<ViagemEncontradaResponse> buscar(String origem, String destino, Viagem... encontradas) {
        var inicio = AMANHA.atStartOfDay();
        lenient().when(repository.publicadasEntre(inicio, inicio.plusDays(1), AGORA, StatusViagem.PUBLICADA)).thenReturn(List.of(encontradas));
        return service.buscar(new BuscaViagemRequest(origem, destino, AMANHA));
    }

    @Test
    void encontraTrechoComEmbarqueEDesembarqueEDadosDoPassageiro() {
        var resultado = buscar("terminal", "aeroporto", viagem("v1", AMANHA.atTime(7, 0)));

        assertThat(resultado).hasSize(1);
        var r = resultado.get(0);
        assertThat(r.getEmbarque().getNome()).isEqualTo("Terminal Central");
        assertThat(r.getEmbarque().getReferencia()).isEqualTo("Plataforma 3");
        assertThat(r.getDesembarque().getNome()).isEqualTo("Aeroporto");
        assertThat(r.getTransportadora()).isEqualTo("Transportadora piloto");
        assertThat(r.getTipoVeiculo()).isEqualTo(TipoVeiculo.VAN);
        assertThat(r.getPreco()).isEqualByComparingTo("18.50");
        assertThat(r.getLugaresDisponiveis()).isEqualTo(15);
    }

    @ParameterizedTest(name = "{0} → {1}: embarque {2}, desembarque {3}")
    @CsvSource({
            "PRACA, shopping, Praça Principal, Shopping",         // sem acento e em maiúsculas
            "centro, aeroporto, Terminal Central, Aeroporto",     // origem/destino da rota identificam o 1º e o último ponto
            "  shop  , AEROPORTO, Shopping, Aeroporto",           // espaços nas pontas
            "terminal, porto, Terminal Central, Aeroporto",       // trecho de nome
    })
    void casaIgnorandoAcentosMaiusculasEUsaOrigemEDestinoDaRota(String origem, String destino, String embarque, String desembarque) {
        var r = buscar(origem, destino, viagem("v1", AMANHA.atTime(7, 0))).get(0);

        assertThat(r.getEmbarque().getNome()).isEqualTo(embarque);
        assertThat(r.getDesembarque().getNome()).isEqualTo(desembarque);
    }

    @ParameterizedTest(name = "{0} → {1} não encontra")
    @CsvSource({
            "aeroporto, centro",      // sentido contrário ao percurso
            "shopping, praca",        // desembarque antes do embarque
            "shopping, shopping",     // mesmo ponto
            "rodoviaria, aeroporto",  // origem fora da rota
    })
    void naoEncontraSentidoContrarioNemLocalForaDaRota(String origem, String destino) {
        assertThat(buscar(origem, destino, viagem("v1", AMANHA.atTime(7, 0)))).isEmpty();
    }

    private Viagem viagemNaRota(String origemRota, String destinoRota, String... nomes) {
        var pontos = new ArrayList<PontoParada>();
        for (int i = 0; i < nomes.length; i++) {
            var p = new PontoParada(nomes[i], null);
            p.setOrdem(i + 1);
            pontos.add(p);
        }
        var v = viagem("v1", AMANHA.atTime(7, 0));
        v.setRota(new Rota("R", origemRota, destinoRota, pontos));
        return v;
    }

    @Test
    void preferePontoComNomeIgualAoQueSoContemOTexto() {
        var v = viagemNaRota("Centro", "Aeroporto", "Terminal", "Rua do Aeroporto Velho", "Shopping", "Aeroporto");

        var r = buscar("terminal", "aeroporto", v).get(0);

        assertThat(r.getDesembarque().getNome()).isEqualTo("Aeroporto");
    }

    @Test
    void pontoComONomeVenceAOrigemDaRota() {
        // A origem da rota é "Shopping", mas o primeiro ponto tem outro nome; há um ponto "Shopping" no meio.
        var v = viagemNaRota("Shopping", "Aeroporto", "Rodoviária", "Shopping", "Aeroporto");

        var r = buscar("shopping", "aeroporto", v).get(0);

        assertThat(r.getEmbarque().getNome()).isEqualTo("Shopping");
        assertThat(r.getEmbarque().getOrdem()).isEqualTo(2);
    }

    @Test
    void comecaComVenceContem() {
        var v = viagemNaRota("A", "B", "Praça Principal", "Terminal da Praça", "Aeroporto");

        assertThat(buscar("praca", "aeroporto", v).get(0).getEmbarque().getNome()).isEqualTo("Praça Principal");
        assertThat(buscar("da praca", "aeroporto", v).get(0).getEmbarque().getNome()).isEqualTo("Terminal da Praça");
    }

    @Test
    void mantemAOrdemDoRepositorioEFiltraSoAsQueCasam() {
        var outraRota = new Rota("Rodoviária – Universidade", "Rodoviária", "Universidade",
                List.of(new PontoParada("Rodoviária", null), new PontoParada("Universidade", null)));
        var outra = viagem("v2", AMANHA.atTime(8, 0));
        outra.setRota(outraRota);

        var resultado = buscar("centro", "aeroporto", viagem("v1", AMANHA.atTime(7, 0)), outra, viagem("v3", AMANHA.atTime(12, 30)));

        assertThat(resultado).extracting(ViagemEncontradaResponse::getId).containsExactly("v1", "v3");
    }

    @Test
    void consultaSoPublicadasDoDiaComPartidaDepoisDeAgora() {
        buscar("centro", "aeroporto");

        var inicio = AMANHA.atStartOfDay();
        verify(repository).publicadasEntre(inicio, inicio.plusDays(1), AGORA, StatusViagem.PUBLICADA);
    }

    @Test
    void recusaDataPassada() {
        assertThatThrownBy(() -> service.buscar(new BuscaViagemRequest("centro", "aeroporto", AGORA.toLocalDate().minusDays(1))))
                .isInstanceOf(DomainException.class)
                .hasMessage("Escolha hoje ou uma data futura.");
        verifyNoInteractions(repository);
    }

    @Test
    void aceitaHoje() {
        var inicio = AGORA.toLocalDate().atStartOfDay();
        when(repository.publicadasEntre(inicio, inicio.plusDays(1), AGORA, StatusViagem.PUBLICADA)).thenReturn(List.of());

        assertThat(service.buscar(new BuscaViagemRequest("centro", "aeroporto", AGORA.toLocalDate()))).isEmpty();
    }

    @Test
    void detalheTrazPontosNaOrdemEReferenciaAusenteComoNula() {
        var v = viagem("v1", AMANHA.atTime(7, 0));
        v.setDataAtualizacao(AGORA.minusHours(1));
        when(repository.findById("v1")).thenReturn(Optional.of(v));

        var d = service.detalhar("v1");

        assertThat(d.getPontos()).extracting(p -> p.getOrdem() + " " + p.getNome())
                .containsExactly("1 Terminal Central", "2 Praça Principal", "3 Shopping", "4 Aeroporto");
        assertThat(d.getPontos().get(1).getReferencia()).isNull();
        assertThat(d.getVeiculoDescricao()).isEqualTo("Van Sprinter");
        assertThat(d.getAtualizadaEm()).isEqualTo(AGORA.minusHours(1));
    }

    @Test
    void detalheSemAlteracaoUsaADataDoCadastro() {
        when(repository.findById("v1")).thenReturn(Optional.of(viagem("v1", AMANHA.atTime(7, 0))));

        assertThat(service.detalhar("v1").getAtualizadaEm()).isEqualTo(AGORA.minusDays(2));
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "SUSPENSA", "CANCELADA"})
    void detalheDeViagemNaoPublicadaFicaIndisponivel(StatusViagem status) {
        var v = viagem("v1", AMANHA.atTime(7, 0));
        v.setStatus(status);
        when(repository.findById("v1")).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.detalhar("v1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Esta viagem não está mais disponível.");
    }

    @Test
    void detalheDeViagemQueJaPartiuOuInexistenteFicaIndisponivel() {
        when(repository.findById("v1")).thenReturn(Optional.of(viagem("v1", AGORA)));
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalhar("v1")).hasMessage("Esta viagem não está mais disponível.");
        assertThatThrownBy(() -> service.detalhar("x")).hasMessage("Esta viagem não está mais disponível.");
    }

    @Test
    void locaisSemRepeticaoOrdenadosEIgnorandoAcento() {
        var outraRota = new Rota("Outra", "Centro", "Praca Principal", List.of(new PontoParada("Centro", null)));
        var outra = viagem("v2", AMANHA.atTime(8, 0));
        outra.setRota(outraRota);
        when(repository.publicadasFuturas(any(), any())).thenReturn(List.of(viagem("v1", AMANHA.atTime(7, 0)), viagem("v3", AMANHA.atTime(9, 0)), outra));

        assertThat(service.locais()).containsExactly("Aeroporto", "Centro", "Praça Principal", "Shopping", "Terminal Central");
        verify(repository).publicadasFuturas(AGORA, StatusViagem.PUBLICADA);
    }
}
