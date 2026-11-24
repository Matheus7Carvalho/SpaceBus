package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagensMotoristaServiceTest {

    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Araguaina"));
    private static final LocalDateTime HOJE = LocalDateTime.of(2026, 11, 17, 0, 0);

    @Mock private IViagemService viagens;
    @Mock private ViagemRepository viagemRepository;
    @Mock private ILocalizacaoService localizacao;

    private ViagensMotoristaService service;

    @BeforeEach
    void setUp() {
        service = new ViagensMotoristaService(viagens, viagemRepository, localizacao, CLOCK);
    }

    private static Viagem viagem(String id, StatusViagem status) {
        var motorista = new Usuario("João", "m@x", "h", Perfil.MOTORISTA, true);
        motorista.setId("m1");
        var viagem = new Viagem(new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of()),
                new Veiculo("SBV1A23", "Van Sprinter", TipoVeiculo.VAN, 15, true), motorista,
                HOJE.plusHours(10), new BigDecimal("18.50"), 15, status);
        viagem.setId(id);
        return viagem;
    }

    @Test
    void listaAsViagensDeHojeDoFusoDaOperacaoComMotivoSoParaPublicadaForaDaJanela() {
        var publicada = viagem("vg1", StatusViagem.PUBLICADA);
        var emAndamento = viagem("vg2", StatusViagem.EM_ANDAMENTO);
        when(viagemRepository.doMotorista("m1", HOJE, HOJE.plusDays(1), StatusViagem.RASCUNHO, StatusViagem.EM_ANDAMENTO))
                .thenReturn(List.of(publicada, emAndamento));
        when(viagens.impedimentoParaIniciar(publicada)).thenReturn(Optional.of("A viagem pode ser iniciada a partir de 30 minutos antes da partida."));
        when(viagens.impedimentoParaIniciar(emAndamento)).thenReturn(Optional.of("A viagem já está em andamento."));
        when(localizacao.ultimasPosicoes(List.of("vg1", "vg2"))).thenReturn(Map.of("vg2", HOJE.plusHours(9)));

        var lista = service.deHoje("m1");

        assertThat(lista).extracting("id").containsExactly("vg1", "vg2");
        assertThat(lista.get(0).isPodeIniciar()).isFalse();
        assertThat(lista.get(0).getMotivo()).isEqualTo("A viagem pode ser iniciada a partir de 30 minutos antes da partida.");
        assertThat(lista.get(0).getVeiculoPlaca()).isEqualTo("SBV1A23");
        assertThat(lista.get(1).getMotivo()).isNull();
        assertThat(lista.get(1).getUltimaPosicaoEm()).isEqualTo(HOJE.plusHours(9));
    }

    @Test
    void iniciarEEncerrarPassamPeloServicoDaViagemConferindoOMotorista() {
        when(viagens.iniciar("vg1", "m1", "m1")).thenReturn(viagem("vg1", StatusViagem.EM_ANDAMENTO));
        when(viagens.encerrar("vg1", "m1", "m1")).thenReturn(viagem("vg1", StatusViagem.ENCERRADA));
        when(viagens.impedimentoParaIniciar(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of("x"));

        assertThat(service.iniciar("vg1", "m1").getStatus()).isEqualTo(StatusViagem.EM_ANDAMENTO);
        assertThat(service.encerrar("vg1", "m1").getStatus()).isEqualTo(StatusViagem.ENCERRADA);
        verify(viagens).iniciar("vg1", "m1", "m1");
        verify(viagens).encerrar("vg1", "m1", "m1");
    }
}
