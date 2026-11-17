package com.spacebus.ops.domain.service.rota;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    @Mock private RotaRepository repository;
    @Mock private ViagemRepository viagemRepository;

    private RotaService service;

    @BeforeEach
    void setUp() {
        service = new RotaService(repository, viagemRepository);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
