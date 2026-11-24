package com.spacebus.ops.api.controller.auth;

import com.spacebus.ops.api.config.security.SessaoService;
import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.domain.model.auth.LoginRequest;
import com.spacebus.ops.domain.model.auth.SessaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

/** O logout (POST /api/auth/logout) é tratado pelo filtro do Spring Security. */
@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
@Tag(name = "Autenticação", description = "Login por sessão.")
public class AuthController {

    private final SessaoService sessaoService;
    private final Clock clock;

    public AuthController(SessaoService sessaoService, Clock clock) {
        this.sessaoService = sessaoService;
        this.clock = clock;
    }

    @PostMapping("/login")
    @Operation(description = "Autentica e abre a sessão.")
    public SessaoResponse login(@Valid @RequestBody LoginRequest login, HttpServletRequest request, HttpServletResponse response) {
        return sessao(sessaoService.entrar(login, request, response));
    }

    @GetMapping("/sessao")
    @Operation(description = "Usuário da sessão atual e o fuso da operação. 401 se não autenticado.")
    public SessaoResponse sessao(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new SessaoResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), clock.getZone().getId());
    }
}
