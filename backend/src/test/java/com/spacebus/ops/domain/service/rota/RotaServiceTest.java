package com.spacebus.ops.domain.service.rota;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Rotina;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.enums.FrequenciaRotina;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.TipoRota;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.rotina.GeradorViagens;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    @Mock private RotaRepository repository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GeradorViagens gerador;

    private RotaService service;
    private final Veiculo van = new Veiculo("SBV1A23", "Van", TipoVeiculo.VAN, 15, true);

    @BeforeEach
    void setUp() {
        service = new RotaService(repository, viagemRepository, veiculoRepository, usuarioRepository, gerador);
        van.setId("v1");
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(veiculoRepository.findById("v1")).thenReturn(Optional.of(van));
    }

    /** Rotina como chega do mapper: veículo só com id. */
    private static Rotina rotina(FrequenciaRotina frequencia, int capacidade, DayOfWeek... dias) {
        var r = new Rotina();
        r.setFrequencia(frequencia);
        r.setDias(new HashSet<>(Set.of(dias)));
        r.setHorarios(new HashSet<>(Set.of(LocalTime.of(7, 0))));
        var v = new Veiculo();
        v.setId("v1");
        r.setVeiculo(v);
        r.setPreco(new BigDecimal("10.00"));
        r.setCapacidade(capacidade);
        r.setInicio(LocalDate.of(2026, 11, 17));
        return r;
    }

    private static Rota fixa(String nome, Rotina rotina) {
        var r = rota(nome, 2);
        r.setTipo(TipoRota.FIXA);
        r.definirRotina(rotina);
        return r;
    }

    @Test
    void rotaFixaResolveVeiculoEGeraAsViagens() {
        var salva = service.create(fixa("Fixa", rotina(FrequenciaRotina.SEMANAL, 15, DayOfWeek.MONDAY)));

        assertThat(salva.getRotina().getVeiculo()).isSameAs(van);
        assertThat(salva.getRotina().getPublicar()).isFalse();
        verify(gerador).gerar(salva.getRotina());
    }

    @Test
    void rotaUnicaDescartaRotinaEnviadaENaoGera() {
        var r = fixa("Única", rotina(FrequenciaRotina.DIARIA, 15));
        r.setTipo(TipoRota.UNICA);

        assertThat(service.create(r).getRotina()).isNull();
        verifyNoInteractions(gerador);
    }

    @Test
    void diariaIgnoraDiasDaSemana() {
        var salva = service.create(fixa("Diária", rotina(FrequenciaRotina.DIARIA, 15, DayOfWeek.MONDAY)));

        assertThat(salva.getRotina().getDias()).isEmpty();
    }

    @Test
    void regrasDaRotina() {
        var semRotina = rota("Sem rotina", 2);
        semRotina.setTipo(TipoRota.FIXA);
        assertThatThrownBy(() -> service.create(semRotina)).hasMessage("Defina a rotina da rota fixa.");

        assertThatThrownBy(() -> service.create(fixa("Semanal", rotina(FrequenciaRotina.SEMANAL, 15))))
                .hasMessage("Escolha ao menos um dia da semana.");

        assertThatThrownBy(() -> service.create(fixa("Lotada", rotina(FrequenciaRotina.DIARIA, 16))))
                .hasMessage("A capacidade não pode passar dos 15 lugares do veículo SBV1A23.");

        var invertida = rotina(FrequenciaRotina.DIARIA, 15);
        invertida.setFim(invertida.getInicio().minusDays(1));
        assertThatThrownBy(() -> service.create(fixa("Invertida", invertida))).hasMessage("O fim da vigência não pode ser antes do início.");

        van.setAtivo(false);
        assertThatThrownBy(() -> service.create(fixa("Inativo", rotina(FrequenciaRotina.DIARIA, 15)))).hasMessage("O veículo SBV1A23 está inativo.");

        verify(repository, never()).save(any());
        verifyNoInteractions(gerador);
    }

    @Test
    void motoristaDaRotinaPrecisaSerMotoristaAtivo() {
        var r = rotina(FrequenciaRotina.DIARIA, 15);
        var m = new Usuario();
        m.setId("p1");
        r.setMotorista(m);
        when(usuarioRepository.findById("p1")).thenReturn(Optional.of(new Usuario("Paula", "p@x", "h", Perfil.PASSAGEIRO, true)));

        assertThatThrownBy(() -> service.create(fixa("Motorista", r))).hasMessage("Selecione um motorista ativo.");
    }

    @Test
    void edicaoDaRotinaMantemOIdERegera() {
        var atual = rotina(FrequenciaRotina.DIARIA, 10);
        atual.setId("rt1");
        atual.setVeiculo(van);
        var existente = fixa("Fixa", atual);
        existente.setId("r1");
        when(repository.findById("r1")).thenReturn(Optional.of(existente));

        var salva = service.update(fixa("Fixa", rotina(FrequenciaRotina.SEMANAL, 12, DayOfWeek.FRIDAY)), "r1");

        assertThat(salva.getRotina()).isSameAs(atual);
        assertThat(atual.getId()).isEqualTo("rt1");
        assertThat(atual.getFrequencia()).isEqualTo(FrequenciaRotina.SEMANAL);
        assertThat(atual.getDias()).containsExactly(DayOfWeek.FRIDAY);
        assertThat(atual.getCapacidade()).isEqualTo(12);
        verify(gerador).regenerar(atual);
    }

    /** Editar só nome ou pontos não refaz os rascunhos (preserva ajustes manuais); só completa o horizonte. */
    @Test
    void edicaoSemMudarAProgramacaoNaoRefazRascunhos() {
        var atual = rotina(FrequenciaRotina.SEMANAL, 12, DayOfWeek.FRIDAY);
        atual.setId("rt1");
        atual.setVeiculo(van);
        atual.setPublicar(false);
        var existente = fixa("Fixa", atual);
        existente.setId("r1");
        when(repository.findById("r1")).thenReturn(Optional.of(existente));

        service.update(fixa("Fixa renomeada", rotina(FrequenciaRotina.SEMANAL, 12, DayOfWeek.FRIDAY)), "r1");

        verify(gerador).gerar(atual);
        verify(gerador, never()).regenerar(any());
    }

    @Test
    void fixaQueViraUnicaPerdeRotinaERascunhos() {
        var atual = rotina(FrequenciaRotina.DIARIA, 10);
        atual.setId("rt1");
        var existente = fixa("Fixa", atual);
        existente.setId("r1");
        when(repository.findById("r1")).thenReturn(Optional.of(existente));

        var salva = service.update(rota("Fixa", 2), "r1");

        assertThat(salva.getTipo()).isEqualTo(TipoRota.UNICA);
        assertThat(salva.getRotina()).isNull();
        verify(gerador).descartarRascunhosFuturos(atual);
        verify(gerador, never()).regenerar(any());
    }

    private static Rota rota(String nome, int pontos) {
        var lista = new ArrayList<PontoParada>();
        IntStream.rangeClosed(1, pontos).forEach(i -> lista.add(new PontoParada(" Ponto " + i + " ", i == 1 ? "  " : " Ref " + i)));
        return new Rota(nome, " Centro ", " Aeroporto ", lista);
    }

    @Test
    void criaOrdenandoPontosEAparandoTextos() {
        var salva = service.create(rota(" Centro – Aeroporto ", 3));

        assertThat(salva.getNome()).isEqualTo("Centro – Aeroporto");
        assertThat(salva.getOrigem()).isEqualTo("Centro");
        assertThat(salva.getDestino()).isEqualTo("Aeroporto");
        assertThat(salva.getPontos()).extracting(PontoParada::getOrdem).containsExactly(1, 2, 3);
        assertThat(salva.getPontos()).extracting(PontoParada::getNome).containsExactly("Ponto 1", "Ponto 2", "Ponto 3");
        assertThat(salva.getPontos().get(0).getReferencia()).isNull();
        assertThat(salva.getPontos()).allSatisfy(p -> assertThat(p.getRota()).isSameAs(salva));
    }

    @Test
    void exigeAoMenosDoisPontos() {
        assertThatThrownBy(() -> service.create(rota("Curta", 1)))
                .isInstanceOf(DomainException.class)
                .hasMessage("A rota deve ter de 2 a 30 pontos de parada.");
        verify(repository, never()).save(any());
    }

    @Test
    void aceitaNoMaximoTrintaPontos() {
        assertThatThrownBy(() -> service.create(rota("Longa", 31))).hasMessage("A rota deve ter de 2 a 30 pontos de parada.");
        assertThat(service.create(rota("Limite", 30)).getPontos()).hasSize(30);
    }

    @Test
    void rejeitaNomeDuplicado() {
        when(repository.existeNome("Centro – Aeroporto", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(rota("Centro – Aeroporto", 2)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Já existe uma rota com o nome Centro – Aeroporto.");
    }

    @Test
    void edicaoSubstituiPontosEReordena() {
        var existente = rota("Centro – Aeroporto", 4);
        existente.setId("r1");
        when(repository.findById("r1")).thenReturn(Optional.of(existente));
        var alterada = new Rota("Centro – Aeroporto", "Centro", "Aeroporto",
                List.of(new PontoParada("Aeroporto", null), new PontoParada("Terminal", null)));

        var salva = service.update(alterada, "r1");

        assertThat(salva).isSameAs(existente);
        assertThat(salva.getPontos()).extracting(PontoParada::getNome).containsExactly("Aeroporto", "Terminal");
        assertThat(salva.getPontos()).extracting(PontoParada::getOrdem).containsExactly(1, 2);
        verify(repository, atLeastOnce()).existeNome("Centro – Aeroporto", "r1");
    }

    /** A regra roda antes de alterar a entidade gerenciada; senão o flush grava o nome repetido e o banco recusa. */
    @Test
    void edicaoComNomeRepetidoNaoAlteraORegistro() {
        var existente = rota("Rota A", 3);
        existente.setId("r2");
        when(repository.findById("r2")).thenReturn(Optional.of(existente));
        when(repository.existeNome("Centro – Aeroporto", "r2")).thenReturn(true);

        assertThatThrownBy(() -> service.update(rota(" Centro – Aeroporto ", 2), "r2"))
                .hasMessage("Já existe uma rota com o nome Centro – Aeroporto.");
        assertThat(existente.getNome()).isEqualTo("Rota A");
        assertThat(existente.getPontos()).hasSize(3);
        verify(repository, never()).save(any());
    }

    @Test
    void naoExcluiRotaComViagens() {
        when(viagemRepository.existsByRotaId("r1")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("r1"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Rota com viagens não pode ser excluída.");
        verify(repository, never()).deleteById(any());
    }
}
