package com.spacebus.ops.api.controller.busca;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.busca.BuscaViagemRequest;
import com.spacebus.ops.domain.model.busca.ViagemEncontradaResponse;
import com.spacebus.ops.domain.model.rota.PontoParadaResponse;
import com.spacebus.ops.domain.service.busca.IBuscaViagemService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuscaViagemController.class)
@Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class BuscaViagemControllerTest {

    private static final String BUSCA = "/api/busca/viagens?origem=centro&destino=aeroporto&data=2026-11-18";

    @Autowired private MockMvc mvc;
    @MockitoBean private IBuscaViagemService service;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get(BUSCA)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/busca/locais")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSAGEIRO", "MOTORISTA", "OPERADOR", "ADMINISTRADOR"})
    void todoPerfilBusca(String perfil) throws Exception {
        when(service.buscar(any())).thenReturn(List.of(ViagemEncontradaResponse.builder()
                .id("v1").partida(LocalDateTime.of(2026, 11, 18, 7, 0)).preco(new BigDecimal("18.50")).tipoVeiculo(TipoVeiculo.VAN)
                .embarque(new PontoParadaResponse("p1", 1, "Terminal Central", "Plataforma 3"))
                .desembarque(new PontoParadaResponse("p4", 4, "Aeroporto", null)).lugaresDisponiveis(15).build()));

        mvc.perform(get(BUSCA).with(user("u").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partida").value("2026-11-18T07:00:00"))
                .andExpect(jsonPath("$[0].embarque.nome").value("Terminal Central"))
                .andExpect(jsonPath("$[0].lugaresDisponiveis").value(15))
                .andExpect(jsonPath("$[0].motoristaNome").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());

        var captor = ArgumentCaptor.forClass(BuscaViagemRequest.class);
        verify(service).buscar(captor.capture());
        assertThat(captor.getValue().getData()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void validaParametros() throws Exception {
        mvc.perform(get("/api/busca/viagens?origem=&destino=" + "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'origem')].message").value("Informe a origem."))
                .andExpect(jsonPath("$.errors[?(@.field == 'destino')].message").value("O destino deve ter até 100 caracteres."))
                .andExpect(jsonPath("$.errors[?(@.field == 'data')].message").value("Informe a data."));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void dataMalformadaNaoMostraMensagemTecnica() throws Exception {
        mvc.perform(get("/api/busca/viagens?origem=a&destino=b&data=2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'data')].message").value("Informe um valor válido."));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void soEspacosContaComoVazio() throws Exception {
        mvc.perform(get("/api/busca/viagens").param("origem", "   ").param("destino", "b").param("data", "2026-11-18"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'origem')].message").value("Informe a origem."));
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void dataPassadaVira400ComMensagem() throws Exception {
        when(service.buscar(any())).thenThrow(new DomainException("Escolha hoje ou uma data futura."));

        mvc.perform(get(BUSCA)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Escolha hoje ou uma data futura."));
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void detalheIndisponivelVira404() throws Exception {
        when(service.detalhar("v9")).thenThrow(new EntityNotFoundException("Esta viagem não está mais disponível."));

        mvc.perform(get("/api/busca/viagens/v9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Esta viagem não está mais disponível."));
    }

    @Test
    @WithMockUser(roles = "MOTORISTA")
    void listaLocais() throws Exception {
        when(service.locais()).thenReturn(List.of("Aeroporto", "Centro"));

        mvc.perform(get("/api/busca/locais")).andExpect(status().isOk()).andExpect(jsonPath("$[1]").value("Centro"));
    }
}
