package com.spacebus.ops.api.controller.notificacao;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusEnvio;
import com.spacebus.ops.domain.entity.enums.TipoNotificacao;
import com.spacebus.ops.domain.model.notificacao.NotificacaoResponse;
import com.spacebus.ops.domain.model.notificacao.PreferenciasResponse;
import com.spacebus.ops.domain.service.notificacao.INotificacaoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacaoController.class)
@Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class NotificacaoControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private INotificacaoService service;
    @MockitoBean private UserDetailsService userDetailsService;

    private static UsuarioAutenticado paula() {
        var u = new Usuario("Paula", "passageiro@spacebus.local", "h", Perfil.PASSAGEIRO, true);
        u.setId("p1");
        return new UsuarioAutenticado(u);
    }

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/notificacao")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificacao/nao-lidas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "MOTORISTA", "ADMINISTRADOR"})
    void soPassageiroVeNotificacoes(String perfil) throws Exception {
        mvc.perform(get("/api/notificacao").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(get("/api/notificacao/nao-lidas").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/notificacao/n1/ler").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/notificacao/ler-todas").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(get("/api/notificacao/preferencias").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void listaAsDoPassageiroComEnviosSemEmail() throws Exception {
        var n = new NotificacaoResponse("n1", TipoNotificacao.RESERVA_CONFIRMADA, "Reserva SB-AB12CD confirmada", "Praça → Aeroporto",
                "/reservas/rs1", "rs1", LocalDateTime.of(2026, 11, 24, 7, 12), null,
                List.of(new NotificacaoResponse.Envio(CanalEnvio.EMAIL, StatusEnvio.ENVIADA, null, LocalDateTime.of(2026, 11, 24, 7, 12))));
        when(service.minhas(eq("p1"), any())).thenReturn(new PageImpl<>(List.of(n)));

        mvc.perform(get("/api/notificacao").with(user(paula())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Reserva SB-AB12CD confirmada"))
                .andExpect(jsonPath("$.content[0].link").value("/reservas/rs1"))
                .andExpect(jsonPath("$.content[0].lidaEm").isEmpty())
                .andExpect(jsonPath("$.content[0].envios[0].canal").value("EMAIL"))
                .andExpect(jsonPath("$.content[0].envios[0].status").value("ENVIADA"))
                .andExpect(jsonPath("$.content[0].envios[0].destino").doesNotExist())
                .andExpect(jsonPath("$.content[0].envios[0].email").doesNotExist());
    }

    @Test
    void contagemDeNaoLidas() throws Exception {
        when(service.naoLidas("p1")).thenReturn(3L);

        mvc.perform(get("/api/notificacao/nao-lidas").with(user(paula()))).andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(3));
    }

    @Test
    void notificacaoDeOutroPassageiroResponde404() throws Exception {
        when(service.ler("n9", "p1")).thenThrow(new EntityNotFoundException("Notificação não encontrada."));

        mvc.perform(post("/api/notificacao/n9/ler").with(user(paula())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notificação não encontrada."));
    }

    @Test
    void lerTodasEPreferencias() throws Exception {
        when(service.lerTodas("p1")).thenReturn(2);
        when(service.definirWhatsapp("p1", true)).thenReturn(new PreferenciasResponse(true, LocalDateTime.of(2026, 11, 24, 9, 0)));

        mvc.perform(post("/api/notificacao/ler-todas").with(user(paula()))).andExpect(status().isOk()).andExpect(jsonPath("$.lidas").value(2));
        mvc.perform(put("/api/notificacao/preferencias").with(user(paula())).contentType(MediaType.APPLICATION_JSON).content("{\"whatsapp\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp").value(true))
                .andExpect(jsonPath("$.whatsappConsentidoEm").value("2026-11-24T09:00:00"));
        verify(service).definirWhatsapp("p1", true);
    }

    @Test
    void preferenciaSemValorERecusada() throws Exception {
        mvc.perform(put("/api/notificacao/preferencias").with(user(paula())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("Informe se quer receber por WhatsApp."));
        verifyNoInteractions(service);
    }
}
