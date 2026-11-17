package com.spacebus.ops.api.controller.viagem;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.viagem.IViagemMapperImpl;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViagemController.class)
@Import({IViagemMapperImpl.class, WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class ViagemControllerTest {

    private static final String VALIDA = """
            {"rotaId":"r1","veiculoId":"v1","motoristaId":null,"partida":"2026-11-18T07:00:00","preco":18.50,"capacidade":15}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private IViagemService service;
    @MockitoBean private UserDetailsService userDetailsService;

    private static Viagem viagem(StatusViagem status) {
        var rota = new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of());
        rota.setId("r1");
        var van = new Veiculo("SBV1A23", "Van Sprinter", TipoVeiculo.VAN, 15, true);
        van.setId("v1");
        var viagem = new Viagem(rota, van, null, LocalDateTime.of(2026, 11, 18, 7, 0), new BigDecimal("18.50"), 15, status);
        viagem.setId("vg1");
        return viagem;
    }

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/viagem")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/viagem/vg1/publicar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTORISTA", "PASSAGEIRO"})
    void perfisForaDaOperacaoNaoAcessamNemTransicoes(String perfil) throws Exception {
        mvc.perform(get("/api/viagem").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        for (var acao : List.of("publicar", "suspender", "cancelar")) {
            mvc.perform(post("/api/viagem/vg1/" + acao).with(user("u").roles(perfil))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void criaEnviandoReferenciasEDevolveResumoAchatado() throws Exception {
        when(service.create(any())).thenReturn(viagem(StatusViagem.RASCUNHO));

        mvc.perform(post("/api/viagem").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.rotaNome").value("Centro – Aeroporto"))
                .andExpect(jsonPath("$.origem").value("Centro"))
                .andExpect(jsonPath("$.veiculoPlaca").value("SBV1A23"))
                .andExpect(jsonPath("$.tipoVeiculo").value("VAN"))
                .andExpect(jsonPath("$.partida").value("2026-11-18T07:00:00"))
                .andExpect(jsonPath("$.preco").value(18.5))
                .andExpect(jsonPath("$.motoristaId").doesNotExist());

        var captor = ArgumentCaptor.forClass(Viagem.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getRota().getId()).isEqualTo("r1");
        assertThat(captor.getValue().getVeiculo().getId()).isEqualTo("v1");
        assertThat(captor.getValue().getMotorista()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void validaCamposELimites() throws Exception {
        mvc.perform(post("/api/viagem").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rotaId":"","veiculoId":"","partida":null,"preco":10000.00,"capacidade":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'rotaId')].message").value("Selecione a rota."))
                .andExpect(jsonPath("$.errors[?(@.field == 'veiculoId')].message").value("Selecione o veículo."))
                .andExpect(jsonPath("$.errors[?(@.field == 'partida')].message").value("Informe a partida."))
                .andExpect(jsonPath("$.errors[?(@.field == 'capacidade')].message").value("A capacidade deve ser entre 1 e 60 lugares."));
        mvc.perform(post("/api/viagem").contentType(MediaType.APPLICATION_JSON).content(VALIDA.replace("18.50", "18.555")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'preco')].message").value("Use no máximo 2 casas decimais no preço."));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void transicoesDelegamAoServico() throws Exception {
        when(service.publicar("vg1")).thenReturn(viagem(StatusViagem.PUBLICADA));
        when(service.suspender("vg1")).thenReturn(viagem(StatusViagem.SUSPENSA));
        when(service.cancelar("vg1")).thenReturn(viagem(StatusViagem.CANCELADA));

        mvc.perform(post("/api/viagem/vg1/publicar")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLICADA"));
        mvc.perform(post("/api/viagem/vg1/suspender")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENSA"));
        mvc.perform(post("/api/viagem/vg1/cancelar")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void regraDeNegocioVira400ComMensagem() throws Exception {
        when(service.publicar("vg1")).thenThrow(new DomainException("A partida deve ser depois do horário atual."));

        mvc.perform(post("/api/viagem/vg1/publicar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A partida deve ser depois do horário atual."));
    }
}
