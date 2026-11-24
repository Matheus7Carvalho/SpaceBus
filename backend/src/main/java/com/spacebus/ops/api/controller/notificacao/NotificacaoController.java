package com.spacebus.ops.api.controller.notificacao;

import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.domain.model.notificacao.NotificacaoResponse;
import com.spacebus.ops.domain.model.notificacao.PreferenciasRequest;
import com.spacebus.ops.domain.model.notificacao.PreferenciasResponse;
import com.spacebus.ops.domain.service.notificacao.INotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Notificações do passageiro logado. Restrito ao perfil Passageiro em SecurityConfiguration; cada um só vê as suas. */
@RestController
@RequestMapping(value = "/api/notificacao", produces = "application/json")
@Tag(name = "Notificação", description = "Avisos da reserva ao passageiro e preferência de WhatsApp (simulado).")
public class NotificacaoController {

    public static final int POR_PAGINA = 20;

    private final INotificacaoService service;

    public NotificacaoController(INotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(description = "Minhas notificações, da mais recente para a mais antiga, com os envios externos.")
    public Page<NotificacaoResponse> minhas(@AuthenticationPrincipal UsuarioAutenticado usuario, @RequestParam(defaultValue = "0") int page) {
        return service.minhas(usuario.getId(), PageRequest.of(Math.max(page, 0), POR_PAGINA));
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> naoLidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("quantidade", service.naoLidas(usuario.getId()));
    }

    @PostMapping("/{id}/ler")
    @Operation(description = "Marca como lida (idempotente).")
    public NotificacaoResponse ler(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return service.ler(id, usuario.getId());
    }

    @PostMapping("/ler-todas")
    public Map<String, Integer> lerTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("lidas", service.lerTodas(usuario.getId()));
    }

    @GetMapping("/preferencias")
    public PreferenciasResponse preferencias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.preferencias(usuario.getId());
    }

    @PutMapping("/preferencias")
    @Operation(description = "Liga ou desliga o WhatsApp (simulado). Ligar grava a data do consentimento.")
    public PreferenciasResponse definir(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody PreferenciasRequest pedido) {
        return service.definirWhatsapp(usuario.getId(), pedido.getWhatsapp());
    }
}
