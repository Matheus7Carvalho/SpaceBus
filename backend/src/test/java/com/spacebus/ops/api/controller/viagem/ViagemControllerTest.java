package com.spacebus.ops.api.controller.viagem;

import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.api.config.security.PasswordConfiguration;
import com.spacebus.ops.api.config.security.SecurityConfiguration;
import com.spacebus.ops.api.config.web.WebConfiguration;
import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.viagem.IViagemMapperImpl;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import com.spacebus.ops.domain.service.auditoria.IAuditoriaService;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import com.spacebus.ops.api.controller.reserva.ReservaRespostas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViagemController.class)
@Import({ReservaRespostas.class, IViagemMapperImpl.class, WebConfiguration.class, SecurityConfiguration.class, PasswordConfiguration.class})
class ViagemControllerTest {

    private static final String VALIDA = """
            {"rotaId":"r1","veiculoId":"v1","motoristaId":null,"partida":"2026-11-18T07:00:00","preco":18.50,"capacidade":15}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private IViagemService service;
    @MockitoBean private IReservaService reservas;
    @MockitoBean private IAuditoriaService auditoria;
    @MockitoBean private ILocalizacaoService localizacao;
    @MockitoBean private UserDetailsService userDetailsService;

    private static Viagem viagem(StatusViagem status) {
        var rota = new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of());
        rota.setId("r1");
        var van = new Veiculo("SBV1A23", "Van Sprinter", TipoVeiculo.VAN, 15, true);
        van.setId("v1");
        var viagem = new Viagem(rota, van, null, LocalDateTime.of(2026, 11, 18, 7, 0), new BigDecimal("18.50"), 15, status);
        viagem.setId("vg1");
        return viagem;
    }

    @Test
    void semSessaoResponde401() throws Exception {
        mvc.perform(get("/api/viagem")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/viagem/vg1/publicar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTORISTA", "PASSAGEIRO"})
    void perfisForaDaOperacaoNaoAcessamNemTransicoes(String perfil) throws Exception {
        mvc.perform(get("/api/viagem").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        for (var acao : List.of("publicar", "suspender", "cancelar")) {
            mvc.perform(post("/api/viagem/vg1/" + acao).with(user("u").roles(perfil))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void criaEnviandoReferenciasEDevolveResumoAchatado() throws Exception {
        when(service.create(any())).thenReturn(viagem(StatusViagem.RASCUNHO));

        mvc.perform(post("/api/viagem").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.rotaNome").value("Centro – Aeroporto"))
                .andExpect(jsonPath("$.origem").value("Centro"))
                .andExpect(jsonPath("$.veiculoPlaca").value("SBV1A23"))
                .andExpect(jsonPath("$.tipoVeiculo").value("VAN"))
                .andExpect(jsonPath("$.partida").value("2026-11-18T07:00:00"))
                .andExpect(jsonPath("$.preco").value(18.5))
                .andExpect(jsonPath("$.motoristaId").doesNotExist());

        var captor = ArgumentCaptor.forClass(Viagem.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getRota().getId()).isEqualTo("r1");
        assertThat(captor.getValue().getVeiculo().getId()).isEqualTo("v1");
        assertThat(captor.getValue().getMotorista()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void validaCamposELimites() throws Exception {
        mvc.perform(post("/api/viagem").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rotaId":"","veiculoId":"","partida":null,"preco":10000.00,"capacidade":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'rotaId')].message").value("Selecione a rota."))
                .andExpect(jsonPath("$.errors[?(@.field == 'veiculoId')].message").value("Selecione o veículo."))
                .andExpect(jsonPath("$.errors[?(@.field == 'partida')].message").value("Informe a partida."))
                .andExpect(jsonPath("$.errors[?(@.field == 'capacidade')].message").value("A capacidade deve ser entre 1 e 60 lugares."));
        mvc.perform(post("/api/viagem").contentType(MediaType.APPLICATION_JSON).content(VALIDA.replace("18.50", "18.555")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'preco')].message").value("Use no máximo 2 casas decimais no preço."));
        verifyNoInteractions(service);
    }

    private static UsuarioAutenticado operador() {
        var u = new Usuario("Ana Operadora", "operador@spacebus.local", "h", Perfil.OPERADOR, true);
        u.setId("op1");
        return new UsuarioAutenticado(u);
    }

    @Test
    void transicoesDelegamAoServicoECancelamentoLevaOResponsavel() throws Exception {
        when(service.publicar("vg1")).thenReturn(viagem(StatusViagem.PUBLICADA));
        when(service.suspender("vg1")).thenReturn(viagem(StatusViagem.SUSPENSA));
        when(service.cancelar("vg1", "op1")).thenReturn(viagem(StatusViagem.CANCELADA));

        mvc.perform(post("/api/viagem/vg1/publicar").with(user(operador()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLICADA"));
        mvc.perform(post("/api/viagem/vg1/suspender").with(user(operador()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENSA"));
        mvc.perform(post("/api/viagem/vg1/cancelar").with(user(operador()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
        verify(service).cancelar("vg1", "op1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTORISTA", "PASSAGEIRO"})
    void listaDePassageirosSoParaAOperacao(String perfil) throws Exception {
        mvc.perform(get("/api/viagem/vg1/reservas").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(reservas, auditoria);
    }

    @Test
    void operadorVeAListaDePassageiros() throws Exception {
        when(reservas.daViagem("vg1")).thenReturn(List.of());

        mvc.perform(get("/api/viagem/vg1/reservas").with(user(operador()))).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        verify(reservas).daViagem("vg1");
    }

    /** QA R2: id inexistente ou longo demais responde 404 sem gravar auditoria nem expor erro de banco. */
    @Test
    void listaDePassageirosDeViagemInexistenteResponde404SemAuditoria() throws Exception {
        var idLongo = "x".repeat(40);
        when(service.findById(idLongo)).thenThrow(new jakarta.persistence.EntityNotFoundException("Item não encontrado."));

        mvc.perform(get("/api/viagem/" + idLongo + "/reservas").with(user(operador()))).andExpect(status().isNotFound());

        verifyNoInteractions(auditoria, reservas);
    }

    /** QA N7: o acesso à lista de passageiros é gravado (quem, perfil, viagem) antes de a lista sair. */
    @Test
    void acessoAListaDePassageirosEGravadoAntesDaLista() throws Exception {
        when(reservas.daViagem("vg1")).thenReturn(List.of());

        mvc.perform(get("/api/viagem/vg1/reservas").with(user(operador()))).andExpect(status().isOk());

        var ordem = org.mockito.Mockito.inOrder(auditoria, reservas);
        ordem.verify(auditoria).registrar("op1", com.spacebus.ops.domain.entity.enums.Perfil.OPERADOR,
                com.spacebus.ops.domain.entity.enums.AcaoAuditada.CONSULTA_PASSAGEIROS_VIAGEM, "vg1");
        ordem.verify(reservas).daViagem("vg1");
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void agendaRepassaFiltrosEPaginaPorPartida() throws Exception {
        when(service.agenda(any(), any(), any(), any())).thenAnswer(inv -> new PageImpl<>(List.of(viagem(StatusViagem.PUBLICADA)), inv.getArgument(3), 1));

        mvc.perform(get("/api/viagem/agenda?dia=2026-11-18&rotaId=r1&status=PUBLICADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PUBLICADA"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(100));

        var pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(service).agenda(eq(LocalDate.of(2026, 11, 18)), eq("r1"), eq(StatusViagem.PUBLICADA), pagina.capture());
        assertThat(pagina.getValue().getSort().getOrderFor("partida")).isNotNull();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void agendaIgnoraOrdenacaoETamanhoDoClienteEAceitaPagina() throws Exception {
        when(service.agenda(any(), any(), any(), any())).thenReturn(Page.empty());

        mvc.perform(get("/api/viagem/agenda?page=2&size=5000&sort=motorista.senha")).andExpect(status().isOk());

        var pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(service).agenda(isNull(), isNull(), isNull(), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(100);
        assertThat(pagina.getValue().getSort()).containsExactly(Sort.Order.asc("partida"), Sort.Order.asc("id"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void agendaSemFiltros() throws Exception {
        when(service.agenda(any(), any(), any(), any())).thenReturn(Page.empty());

        mvc.perform(get("/api/viagem/agenda")).andExpect(status().isOk());

        verify(service).agenda(isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "PASSAGEIRO")
    void passageiroNaoVeAgenda() throws Exception {
        mvc.perform(get("/api/viagem/agenda")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void regraDeNegocioVira400ComMensagem() throws Exception {
        when(service.publicar("vg1")).thenThrow(new DomainException("A partida deve ser depois do horário atual."));

        mvc.perform(post("/api/viagem/vg1/publicar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A partida deve ser depois do horário atual."));
    }

    // --- Módulo 4: iniciar, encerrar e localização na operação ---

    @ParameterizedTest
    @ValueSource(strings = {"MOTORISTA", "PASSAGEIRO"})
    void iniciarEncerrarELocalizacaoSoParaAOperacao(String perfil) throws Exception {
        mvc.perform(post("/api/viagem/vg1/iniciar").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/viagem/vg1/encerrar").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        mvc.perform(get("/api/viagem/vg1/localizacao").with(user("u").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(service, localizacao);
    }

    @Test
    void iniciarEncerrarELocalizacaoSemSessaoResponde401() throws Exception {
        mvc.perform(post("/api/viagem/vg1/iniciar")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/viagem/vg1/localizacao")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, localizacao);
    }

    @Test
    void operadorIniciaEEncerraSemConferirMotoristaComoResponsavel() throws Exception {
        var iniciada = viagem(StatusViagem.EM_ANDAMENTO);
        iniciada.setIniciadaEm(LocalDateTime.of(2026, 11, 18, 6, 40));
        when(service.iniciar("vg1", "op1", null)).thenReturn(iniciada);
        when(service.encerrar("vg1", "op1", null)).thenReturn(viagem(StatusViagem.ENCERRADA));

        mvc.perform(post("/api/viagem/vg1/iniciar").with(user(operador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.iniciadaEm").value("2026-11-18T06:40:00"));
        mvc.perform(post("/api/viagem/vg1/encerrar").with(user(operador())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADA"));
    }

    @Test
    void localizacaoDaOperacaoTemDiagnosticoESemDadosDoMotorista() throws Exception {
        var resposta = com.spacebus.ops.domain.model.localizacao.LocalizacaoOperacaoResponse.builder()
                .localizacao(com.spacebus.ops.domain.model.localizacao.LocalizacaoResponse.builder()
                        .situacaoViagem(StatusViagem.EM_ANDAMENTO).disponivel(true).latitude(-10.18).longitude(-48.33).precisaoM(30.0)
                        .registradaEm(LocalDateTime.of(2026, 11, 18, 7, 5)).segundosDesdeAtualizacao(20L).build())
                .diagnostico(com.spacebus.ops.domain.model.localizacao.DiagnosticoLocalizacaoResponse.builder().aceitas(4).lacunas(1).build())
                .build();
        when(localizacao.daViagem("vg1", "op1", Perfil.OPERADOR)).thenReturn(resposta);

        mvc.perform(get("/api/viagem/vg1/localizacao").with(user(operador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoViagem").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.latitude").value(-10.18))
                .andExpect(jsonPath("$.registradaEm").value("2026-11-18T07:05:00"))
                .andExpect(jsonPath("$.desatualizada").value(false))
                .andExpect(jsonPath("$.diagnostico.aceitas").value(4))
                .andExpect(jsonPath("$.diagnostico.lacunas").value(1))
                .andExpect(jsonPath("$.localizacao").doesNotExist())
                .andExpect(jsonPath("$.motoristaNome").doesNotExist())
                .andExpect(jsonPath("$.motoristaId").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void agendaMarcaSemSinalSoNasViagensQueOServicoApontar() throws Exception {
        var emAndamento = viagem(StatusViagem.EM_ANDAMENTO);
        when(service.agenda(any(), any(), any(), any())).thenAnswer(inv -> new PageImpl<>(List.of(emAndamento), inv.getArgument(3), 1));
        when(localizacao.semSinal(List.of(emAndamento))).thenReturn(java.util.Set.of("vg1"));

        mvc.perform(get("/api/viagem/agenda")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].semSinal").value(true));
    }
}
