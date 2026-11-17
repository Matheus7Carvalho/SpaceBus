package com.spacebus.ops.api.controller.usuario;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.model.usuario.IUsuarioMapperImpl;
import com.spacebus.ops.domain.service.usuario.IUsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import({IUsuarioMapperImpl.class, WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class UsuarioControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private IUsuarioService service;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void passageiroNaoAcessa() throws Exception {
        mvc.perform(get("/api/usuario/motoristas")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void listaMotoristasSemSenha() throws Exception {
        when(service.motoristasAtivos()).thenReturn(List.of(new Usuario("João Costa", "motorista@spacebus.local", "hash", Perfil.MOTORISTA, true)));

        mvc.perform(get("/api/usuario/motoristas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("João Costa"))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
    }
}
