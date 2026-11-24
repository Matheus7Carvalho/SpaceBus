package com.spacebus.ops.api.controller.reserva;

import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoResponse;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReservaController.class, PagamentoWebhookController.class})
@Import({ReservaRespostas.class, WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
@TestPropertySource(properties = "spacebus.pagamento.webhook-segredo=segredo-teste")
class ReservaControllerTest {

    private static final String PEDIDO = """
            {"viagemId":"vg1","embarqueOrdem":1,"desembarqueOrdem":4,"quantidade":2}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private IReservaService service;
    @MockitoBean private ILocalizacaoService localizacao;
    @MockitoBean private UserDetailsService userDetailsService;

    private static UsuarioAutenticado paula() {
        var u = new Usuario("Paula", "passageiro@spacebus.local", "h", Perfil.PASSAGEIRO, true);
        u.setId("u1");
        return new UsuarioAutenticado(u);
    }

    private static Reserva reserva() {
        var viagem = new Viagem(new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of()),
                new Veiculo("SBV1A23", "Van", TipoVeiculo.VAN, 15, true), null,
                LocalDateTime.of(2026, 11, 18, 7, 0), new BigDecimal("18.50"), 15, StatusViagem.PUBLICADA);
        viagem.setId("vg1");
        var r = new Reserva();
        r.setId("rs1");
        r.setCodigo("SB-ABC234");
        r.setViagem(viagem);
        r.setEmbarqueNome("Terminal");
        r.setDesembarqueNome("Aeroporto");
        r.setQuantidade(2);
        r.setValorTotal(new BigDecimal("37.00"));
        r.setStatus(StatusReserva.AGUARDANDO_PAGAMENTO);
        r.setExpiraEm(LocalDateTime.of(2026, 11, 17, 9, 10));
        return r;
    }

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/reserva")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reserva").contentType(MediaType.APPLICATION_JSON).content(PEDIDO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "ADMINISTRADOR", "MOTORISTA"})
    void soPassageiroReserva(String perfil) throws Exception {
        mvc.perform(post("/api/reserva").with(user("u").roles(perfil)).contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reserva").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void reservaEmNomeDoPassageiroLogado() throws Exception {
        when(service.reservar(eq("u1"), any())).thenReturn(reserva());
        when(service.pagavel(any())).thenReturn(true);
        when(service.ultimoPagamento("rs1")).thenReturn(Optional.empty());

        mvc.perform(post("/api/reserva").with(user(paula())).contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("SB-ABC234"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.valorTotal").value(37.0))
                .andExpect(jsonPath("$.pagavel").value(true))
                .andExpect(jsonPath("$.rotaNome").value("Centro – Aeroporto"))
                .andExpect(jsonPath("$.situacaoViagem").value("PUBLICADA"))
                .andExpect(jsonPath("$.passageiro").doesNotExist());

        var pedido = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(service).reservar(eq("u1"), pedido.capture());
        assertThat(pedido.getValue().getQuantidade()).isEqualTo(2);
    }

    @Test
    void validaPedido() throws Exception {
        mvc.perform(post("/api/reserva").with(user(paula())).contentType(MediaType.APPLICATION_JSON).content("""
                        {"viagemId":"","embarqueOrdem":null,"desembarqueOrdem":31,"quantidade":7}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'viagemId')].message").value("Selecione a viagem."))
                .andExpect(jsonPath("$.errors[?(@.field == 'embarqueOrdem')].message").value("Informe o ponto de embarque."))
                .andExpect(jsonPath("$.errors[?(@.field == 'quantidade')].message").value("Reserve de 1 a 6 lugares."));
        verifyNoInteractions(service);
    }

    @Test
    void pagamentoSoMostraAUrlEnquantoPendente() throws Exception {
        var pendente = new Pagamento(reserva(), "sim_1", "PIX_SIMULADO", new BigDecimal("37.00"), StatusPagamento.PENDENTE, "/reservas/pagamento/sim_1");
        when(service.iniciarPagamento("rs1", "u1")).thenReturn(pendente);

        mvc.perform(post("/api/reserva/rs1/pagamento").with(user(paula())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlPagamento").value("/reservas/pagamento/sim_1"));

        pendente.setStatus(StatusPagamento.APROVADO);
        mvc.perform(post("/api/reserva/rs1/pagamento").with(user(paula())))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.urlPagamento").doesNotExist());
    }

    /** QA N3: segredo em branco liberaria um cabeçalho vazio; o backend não sobe assim. */
    @Test
    void segredoEmBrancoImpedeASubida() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new PagamentoWebhookController(service, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPACEBUS_WEBHOOK_SEGREDO");
    }

    @Test
    void webhookExigeOSegredoDoProvedor() throws Exception {
        var corpo = """
                {"provedorId":"sim_1","resultado":"APROVADO"}""";
        mvc.perform(post("/api/pagamento/webhook").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pagamento/webhook").header(PagamentoWebhookController.CABECALHO_SEGREDO, "errado")
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);

        mvc.perform(post("/api/pagamento/webhook").header(PagamentoWebhookController.CABECALHO_SEGREDO, "segredo-teste")
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isNoContent());
        verify(service).processarRetorno("sim_1", RetornoPagamentoRequest.Resultado.APROVADO);
    }

    // --- Módulo 4: localização da van na reserva ---

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "ADMINISTRADOR", "MOTORISTA"})
    void localizacaoDaReservaSoParaPassageiro(String perfil) throws Exception {
        mvc.perform(get("/api/reserva/rs1/localizacao").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(localizacao);
    }

    @Test
    void localizacaoSemSessaoResponde401() throws Exception {
        mvc.perform(get("/api/reserva/rs1/localizacao")).andExpect(status().isUnauthorized());
        verifyNoInteractions(localizacao);
    }

    @Test
    void passageiroVeALocalizacaoDaSuaReservaSemDadosDoMotorista() throws Exception {
        when(localizacao.daReserva("rs1", "u1")).thenReturn(LocalizacaoResponse.builder().situacaoViagem(StatusViagem.EM_ANDAMENTO)
                .disponivel(true).latitude(-10.18).longitude(-48.33).precisaoM(30.0).registradaEm(LocalDateTime.of(2026, 11, 18, 7, 5))
                .segundosDesdeAtualizacao(150L).desatualizada(true).simulada(true).build());

        mvc.perform(get("/api/reserva/rs1/localizacao").with(user(paula())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoViagem").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.precisaoM").value(30.0))
                .andExpect(jsonPath("$.registradaEm").value("2026-11-18T07:05:00"))
                .andExpect(jsonPath("$.segundosDesdeAtualizacao").value(150))
                .andExpect(jsonPath("$.desatualizada").value(true))
                .andExpect(jsonPath("$.simulada").value(true))
                .andExpect(jsonPath("$.motoristaNome").doesNotExist())
                .andExpect(jsonPath("$.diagnostico").doesNotExist());
    }

    @Test
    void reservaDeOutroPassageiroResponde404ENaoConfirmadaRespondeAMensagem() throws Exception {
        when(localizacao.daReserva("rs9", "u1")).thenThrow(new jakarta.persistence.EntityNotFoundException("Reserva não encontrada."));
        when(localizacao.daReserva("rs1", "u1")).thenThrow(new com.spacebus.ops.domain.core.exception.DomainException("Localização disponível só para reservas confirmadas."));

        mvc.perform(get("/api/reserva/rs9/localizacao").with(user(paula()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/reserva/rs1/localizacao").with(user(paula())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Localização disponível só para reservas confirmadas."));
    }
}
