package com.spacebus.ops.api.controller.rota;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.model.rota.IRotaMapperImpl;
import com.spacebus.ops.domain.service.rota.IRotaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RotaController.class)
@Import({IRotaMapperImpl.class, WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class RotaControllerTest {

    private static final String VALIDA = """
            {"nome":"Centro – Aeroporto","origem":"Centro","destino":"Aeroporto",
             "pontos":[{"nome":"Terminal","referencia":"Plataforma 3"},{"nome":"Aeroporto"}]}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private IRotaService service;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/rota")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void passageiroNaoAcessa() throws Exception {
        mvc.perform(post("/api/rota").contentType(MediaType.APPLICATION_JSON).content(VALIDA)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void criaComPontosNaOrdemEnviada() throws Exception {
        when(service.create(any())).thenAnswer(inv -> {
            Rota r = inv.getArgument(0);
            var ordem = 1;
            for (PontoParada p : r.getPontos()) p.setOrdem(ordem++);
            return r;
        });

        mvc.perform(post("/api/rota").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos[0].nome").value("Terminal"))
                .andExpect(jsonPath("$.pontos[0].ordem").value(1))
                .andExpect(jsonPath("$.pontos[1].nome").value("Aeroporto"))
                .andExpect(jsonPath("$.pontos[1].rota").doesNotExist());

        var captor = ArgumentCaptor.forClass(Rota.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getPontos()).allSatisfy(p -> assertThat(p.getRota()).isSameAs(captor.getValue()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void exigeDoisPontosEValidaCadaPonto() throws Exception {
        mvc.perform(post("/api/rota").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"","origem":"Centro","destino":"Aeroporto","pontos":[{"nome":"","referencia":"%s"}]}""".formatted("X".repeat(151))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'nome')].message").value("Informe o nome."))
                .andExpect(jsonPath("$.errors[?(@.field == 'pontos')].message").value("A rota deve ter de 2 a 30 pontos de parada."))
                .andExpect(jsonPath("$.errors[?(@.field == 'pontos[0].nome')].message").value("Informe o nome do ponto."))
                .andExpect(jsonPath("$.errors[?(@.field == 'pontos[0].referencia')].message").value("A referência deve ter até 150 caracteres."));
        verifyNoInteractions(service);
    }
}
