package com.spacebus.ops.api.controller.dev;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** H17: posição simulada só no perfil dev e só para Administrador. */
class LocalizacaoSimuladaControllerTest {

    private static final String URL = "/api/dev/localizacao/vg1";
    private static final String POSICAO = """
            {"latitude":-10.18,"longitude":-48.33,"precisaoM":10,"registradaEm":"2026-11-17T08:59:30-03:00"}""";

    @Nested
    @WebMvcTest(LocalizacaoSimuladaController.class)
    @ActiveProfiles("dev")
    @Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
    class NoPerfilDev {

        @Autowired private MockMvc mvc;
        @MockitoBean private ILocalizacaoService service;
        @MockitoBean private UserDetailsService userDetailsService;

        @Test
        void semSessaoResponde401() throws Exception {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(POSICAO)).andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"OPERADOR", "MOTORISTA", "PASSAGEIRO"})
        void soAdministradorSimula(String perfil) throws Exception {
            mvc.perform(post(URL).with(user("u").roles(perfil)).contentType(MediaType.APPLICATION_JSON).content(POSICAO))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void administradorGravaPosicaoSimuladaPeloMesmoCaminho() throws Exception {
            when(service.registrarSimulada(eq("vg1"), any())).thenReturn(new ResultadoEnvioResponse(1, 0, 0));

            mvc.perform(post(URL).with(user("u").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(POSICAO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aceitas").value(1));
        }

        @Test
        void validaAPosicao() throws Exception {
            mvc.perform(post(URL).with(user("u").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON)
                            .content(POSICAO.replace("-10.18", "91")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("Latitude deve estar entre -90 e 90."));
            verifyNoInteractions(service);
        }
    }

    /** O application.properties ativa dev por padrão; aqui, o perfil de produção. */
    @Nested
    @WebMvcTest(LocalizacaoSimuladaController.class)
    @ActiveProfiles("producao")
    @Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
    class ForaDoPerfilDev {

        @Autowired private MockMvc mvc;
        @MockitoBean private ILocalizacaoService service;
        @MockitoBean private UserDetailsService userDetailsService;

        @Test
        void endpointNaoExiste() throws Exception {
            mvc.perform(post(URL).with(user("u").roles("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(POSICAO))
                    .andExpect(status().isNotFound());
            verifyNoInteractions(service);
        }
    }
}
