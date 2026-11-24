package com.spacebus.ops.api.controller.motorista;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.core.exception.AcessoNegadoException;
import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.localizacao.PosicaoRequest;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;
import com.spacebus.ops.domain.model.localizacao.ViagemMotoristaResponse;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import com.spacebus.ops.domain.service.localizacao.IViagensMotoristaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MotoristaViagemController.class)
@Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class MotoristaViagemControllerTest {

    private static final String POSICAO = """
            {"latitude":-10.18,"longitude":-48.33,"precisaoM":12.5,"registradaEm":"2026-11-17T08:59:30-03:00"}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private IViagensMotoristaService viagens;
    @MockitoBean private ILocalizacaoService localizacao;
    @MockitoBean private UserDetailsService userDetailsService;

    private static UsuarioAutenticado joao() {
        var u = new Usuario("João Costa", "motorista@spacebus.local", "h", Perfil.MOTORISTA, true);
        u.setId("m1");
        return new UsuarioAutenticado(u);
    }

    private static String lote(String... posicoes) {
        return "{\"posicoes\":[" + String.join(",", posicoes) + "]}";
    }

    @Test
    void semSessaoResponde401EmJson() throws Exception {
        mvc.perform(get("/api/motorista/viagem")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sessão expirada. Entre novamente."));
        mvc.perform(post("/api/motorista/viagem/vg1/iniciar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").contentType(MediaType.APPLICATION_JSON).content(lote(POSICAO)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(viagens, localizacao);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSAGEIRO", "OPERADOR", "ADMINISTRADOR"})
    void soMotoristaUsaAsRotasDoMotorista(String perfil) throws Exception {
        mvc.perform(get("/api/motorista/viagem").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/motorista/viagem/vg1/iniciar").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/motorista/viagem/vg1/encerrar").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user("u").roles(perfil))
                .contentType(MediaType.APPLICATION_JSON).content(lote(POSICAO))).andExpect(status().isForbidden());
        verifyNoInteractions(viagens, localizacao);
    }

    @Test
    void listaAsViagensDoMotoristaLogado() throws Exception {
        when(viagens.deHoje("m1")).thenReturn(List.of(ViagemMotoristaResponse.builder().id("vg1").rotaNome("Centro – Aeroporto")
                .partida(LocalDateTime.of(2026, 11, 17, 10, 0)).veiculoPlaca("SBV1A23").status(StatusViagem.PUBLICADA)
                .podeIniciar(false).motivo("A viagem pode ser iniciada a partir de 30 minutos antes da partida.").build()));

        mvc.perform(get("/api/motorista/viagem").with(user(joao())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("vg1"))
                .andExpect(jsonPath("$[0].partida").value("2026-11-17T10:00:00"))
                .andExpect(jsonPath("$[0].podeIniciar").value(false))
                .andExpect(jsonPath("$[0].motivo").value("A viagem pode ser iniciada a partir de 30 minutos antes da partida."));
    }

    @Test
    void iniciarEEncerrarLevamOMotoristaLogado() throws Exception {
        when(viagens.iniciar("vg1", "m1")).thenReturn(ViagemMotoristaResponse.builder().id("vg1").status(StatusViagem.EM_ANDAMENTO).build());
        when(viagens.encerrar("vg1", "m1")).thenReturn(ViagemMotoristaResponse.builder().id("vg1").status(StatusViagem.ENCERRADA).build());

        mvc.perform(post("/api/motorista/viagem/vg1/iniciar").with(user(joao())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        mvc.perform(post("/api/motorista/viagem/vg1/encerrar").with(user(joao())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADA"));
    }

    @Test
    void motoristaDeOutraViagemResponde403ComOMotivo() throws Exception {
        when(viagens.iniciar("vg1", "m1")).thenThrow(new AcessoNegadoException("Só o motorista da viagem pode iniciar."));

        mvc.perform(post("/api/motorista/viagem/vg1/iniciar").with(user(joao())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Só o motorista da viagem pode iniciar."));
    }

    @Test
    void loteValidoDevolveAContagem() throws Exception {
        when(localizacao.registrar(eq("vg1"), eq("m1"), anyList())).thenReturn(new ResultadoEnvioResponse(1, 1, 0));

        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON)
                        .content(lote(POSICAO, POSICAO.replace("12.5", "300"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceitas").value(1))
                .andExpect(jsonPath("$.rejeitadas").value(1))
                .andExpect(jsonPath("$.ignoradas").value(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PosicaoRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(localizacao).registrar(eq("vg1"), eq("m1"), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getRegistradaEm()).isEqualTo("2026-11-17T08:59:30-03:00");
    }

    @Test
    void loteVazioSemPosicoesOuComMaisDe20ERecusado() throws Exception {
        var vinteEUma = IntStream.range(0, 21).mapToObj(i -> POSICAO).collect(Collectors.joining(","));
        for (var corpo : List.of(lote(), lote(vinteEUma), "{}")) {
            mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field == 'posicoes')].message").value("Envie de 1 a 20 posições."));
        }
        verifyNoInteractions(localizacao);
    }

    @Test
    void validaLatitudeLongitudePrecisaoEHoraComFuso() throws Exception {
        var invalida = """
                {"latitude":-90.5,"longitude":180.1,"precisaoM":5001,"registradaEm":"2026-11-17T08:59:30"}""";

        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON).content(lote(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'posicoes[0].latitude')].message").value("Latitude deve estar entre -90 e 90."))
                .andExpect(jsonPath("$.errors[?(@.field == 'posicoes[0].longitude')].message").value("Longitude deve estar entre -180 e 180."))
                .andExpect(jsonPath("$.errors[?(@.field == 'posicoes[0].precisaoM')].message").value("Precisão deve estar entre 0 e 5000 m."))
                .andExpect(jsonPath("$.errors[?(@.field == 'posicoes[0].registradaEm')].message").value("Informe a hora da posição com fuso (ISO-8601)."));

        var semHora = """
                {"latitude":-10,"longitude":-48,"precisaoM":10}""";
        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON).content(lote(semHora)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'posicoes[0].registradaEm')].message").value("Informe a hora da posição."));
        verifyNoInteractions(localizacao);
    }

    /** QA D4: JSON malformado responde 400 com mensagem curta, sem detalhe técnico do Jackson. */
    @Test
    void jsonMalformadoResponde400SemDetalheTecnico() throws Exception {
        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posicoes\":[{\"latitude\":\"abc\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido."))
                .andExpect(jsonPath("$.debugMessage").isEmpty())
                .andExpect(jsonPath("$.errors").isEmpty());
        verifyNoInteractions(localizacao);
    }

    @Test
    void horaEmBrancoTemUmaMensagemSo() throws Exception {
        var emBranco = """
                {"latitude":-10,"longitude":-48,"precisaoM":10,"registradaEm":""}""";

        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON).content(lote(emBranco)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].message").value("Informe a hora da posição."));
    }

    @Test
    void viagemForaDeAndamentoResponde400ComAMensagem() throws Exception {
        when(localizacao.registrar(eq("vg1"), eq("m1"), anyList())).thenThrow(new DomainException("A viagem não está em andamento."));

        mvc.perform(post("/api/motorista/viagem/vg1/localizacao").with(user(joao())).contentType(MediaType.APPLICATION_JSON).content(lote(POSICAO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A viagem não está em andamento."));
    }
}
