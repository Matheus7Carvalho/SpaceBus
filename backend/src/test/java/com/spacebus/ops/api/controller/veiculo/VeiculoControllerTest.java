package com.spacebus.ops.api.controller.veiculo;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.model.veiculo.IVeiculoMapperImpl;
import com.spacebus.ops.domain.service.veiculo.IVeiculoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeiculoController.class)
@Import({IVeiculoMapperImpl.class, WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class VeiculoControllerTest {

    private static final String VALIDO = """
            {"placa":"SBV-1A23","descricao":"Van Sprinter","tipo":"VAN","capacidade":15,"ativo":true}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private IVeiculoService service;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/veiculo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sessão expirada. Entre novamente."));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTORISTA", "PASSAGEIRO"})
    void perfisForaDaOperacaoNaoAcessam(String perfil) throws Exception {
        mvc.perform(get("/api/veiculo").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/veiculo").with(user("u").roles(perfil)).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void operadorCadastra() throws Exception {
        when(service.create(any())).thenAnswer(inv -> {
            Veiculo v = inv.getArgument(0);
            v.setId("v1");
            return v;
        });

        mvc.perform(post("/api/veiculo").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("v1"))
                .andExpect(jsonPath("$.tipo").value("VAN"))
                .andExpect(jsonPath("$.capacidade").value(15));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void validaCamposELimites() throws Exception {
        mvc.perform(post("/api/veiculo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"placa":"ABC-12345","descricao":"%s","tipo":null,"capacidade":61}""".formatted("X".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'placa')].message").value("A placa deve ter até 8 caracteres."))
                .andExpect(jsonPath("$.errors[?(@.field == 'descricao')].message").value("A descrição deve ter até 100 caracteres."))
                .andExpect(jsonPath("$.errors[?(@.field == 'tipo')].message").value("Selecione o tipo."))
                .andExpect(jsonPath("$.errors[?(@.field == 'capacidade')].message").value("A capacidade deve ser entre 1 e 60 lugares."));
        verifyNoInteractions(service);
    }
}
