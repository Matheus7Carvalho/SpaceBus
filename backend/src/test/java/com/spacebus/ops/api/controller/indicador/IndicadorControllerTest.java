package com.spacebus.ops.api.controller.indicador;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.indicador.IndicadorViagemResponse;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse;
import com.spacebus.ops.domain.service.indicador.IIndicadorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndicadorController.class)
@Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class IndicadorControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private IIndicadorService service;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/indicador")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/indicador/viagem")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTORISTA", "PASSAGEIRO"})
    void motoristaEPassageiroNaoVeem(String perfil) throws Exception {
        mvc.perform(get("/api/indicador").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(get("/api/indicador/viagem").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void resumoComHorarios() throws Exception {
        when(service.resumo("2026-11-01", "2026-11-30", "r1")).thenReturn(new ResumoIndicadoresResponse(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), new ResumoIndicadoresResponse.Viagens(3, 2, 0, 1, 2), 12, 30, 0.4, new BigDecimal("180.00"),
                List.of(new ResumoIndicadoresResponse.Horario("07:30", 2, 30, 12, 0.4))));

        mvc.perform(get("/api/indicador?inicio=2026-11-01&fim=2026-11-30&rotaId=r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-11-01"))
                .andExpect(jsonPath("$.viagens.total").value(3))
                .andExpect(jsonPath("$.ocupacao").value(0.4))
                .andExpect(jsonPath("$.receita").value(180.0))
                .andExpect(jsonPath("$.horarios[0].horario").value("07:30"))
                .andExpect(jsonPath("$.horarios[0].vendidos").value(12));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void porViagemPaginado() throws Exception {
        when(service.porViagem(null, null, null, 1)).thenReturn(new PageImpl<>(List.of(new IndicadorViagemResponse("vg1",
                LocalDateTime.of(2026, 11, 20, 7, 30), "Centro – Aeroporto", "SBV1A23", StatusViagem.CANCELADA, 15, 0, null, null)),
                PageRequest.of(1, 20), 21));

        mvc.perform(get("/api/indicador/viagem?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].partida").value("2026-11-20T07:30:00"))
                .andExpect(jsonPath("$.content[0].disponiveis").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(21))
                .andExpect(jsonPath("$.page.size").value(20));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void periodoInvalidoResponde400ComAMensagem() throws Exception {
        when(service.resumo("2026-11-21", "2026-11-20", null)).thenThrow(new DomainException("A data inicial deve ser anterior ou igual à final."));

        mvc.perform(get("/api/indicador?inicio=2026-11-21&fim=2026-11-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A data inicial deve ser anterior ou igual à final."));
    }
}
