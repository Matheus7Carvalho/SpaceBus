package com.spacebus.ops.api.controller.reserva;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProvedorSimuladoController.class)
@Import({WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
@TestPropertySource(properties = "spacebus.pagamento.provedor=simulado")
class ProvedorSimuladoControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private PagamentoRepository pagamentos;
    @MockitoBean private IReservaService reservas;
    @MockitoBean private UserDetailsService userDetailsService;

    private static UsuarioAutenticado usuario(String id) {
        var u = new Usuario("Paula", id + "@spacebus.local", "h", Perfil.PASSAGEIRO, true);
        u.setId(id);
        return new UsuarioAutenticado(u);
    }

    @BeforeEach
    void setUp() {
        var dona = new Usuario("Paula", "p@x", "h", Perfil.PASSAGEIRO, true);
        dona.setId("u1");
        var reserva = new Reserva();
        reserva.setId("rs1");
        reserva.setCodigo("SB-ABC234");
        reserva.setPassageiro(dona);
        lenient().when(pagamentos.cobrancaDoPassageiro("sim_1", "u1")).thenReturn(true);
        when(pagamentos.findByProvedorId("sim_1"))
                .thenReturn(Optional.of(new Pagamento(reserva, "sim_1", "PIX_SIMULADO", new BigDecimal("15.00"), StatusPagamento.PENDENTE, "/x")));
    }

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(post("/api/provedor-simulado/cobrancas/sim_1/aprovar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(reservas);
    }

    /** Sem corpo: o cliente HTTP do frontend só aceita resposta vazia com 204. */
    @Test
    void aprovarERecusarRespondem204SemCorpo() throws Exception {
        mvc.perform(post("/api/provedor-simulado/cobrancas/sim_1/aprovar").with(user(usuario("u1"))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(reservas).processarRetorno("sim_1", RetornoPagamentoRequest.Resultado.APROVADO);

        mvc.perform(post("/api/provedor-simulado/cobrancas/sim_1/recusar").with(user(usuario("u1")))).andExpect(status().isNoContent());
        verify(reservas).processarRetorno("sim_1", RetornoPagamentoRequest.Resultado.RECUSADO);
    }

    @Test
    void cobrancaDeOutroPassageiroNaoEEncontrada() throws Exception {
        mvc.perform(post("/api/provedor-simulado/cobrancas/sim_1/aprovar").with(user(usuario("outro")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/provedor-simulado/cobrancas/sim_1").with(user(usuario("outro")))).andExpect(status().isNotFound());
        verifyNoInteractions(reservas);
    }

    @Test
    void mostraACobrancaAoDono() throws Exception {
        mvc.perform(get("/api/provedor-simulado/cobrancas/sim_1").with(user(usuario("u1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaCodigo").value("SB-ABC234"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }
}
