package com.spacebus.ops.api.controller.dev;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.service.limpeza.LimpezaDadosTesteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DadosTesteController.class)
@ActiveProfiles("dev")
@Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class DadosTesteControllerTest {

    private static final String URL = "/api/dev/dados-teste?marca=E2E";

    @Autowired private MockMvc mvc;
    @MockitoBean private LimpezaDadosTesteService service;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(delete(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "MOTORISTA", "PASSAGEIRO"})
    void soAdministradorLimpa(String perfil) throws Exception {
        mvc.perform(delete(URL).with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void administradorLimpaEVeOQueSaiu() throws Exception {
        when(service.limpar("E2E")).thenReturn(new LimpezaDadosTesteService.Resultado(5, 2, 1));

        mvc.perform(delete(URL).with(user("u").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viagens").value(5))
                .andExpect(jsonPath("$.rotas").value(2))
                .andExpect(jsonPath("$.veiculos").value(1));
    }

    @Test
    void marcaInvalidaResponde400ComMotivo() throws Exception {
        when(service.limpar(anyString())).thenThrow(new DomainException("Informe uma marca de teste (ex.: E2E)."));

        mvc.perform(delete("/api/dev/dados-teste?marca=Centro").with(user("u").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe uma marca de teste (ex.: E2E)."));
    }
}
