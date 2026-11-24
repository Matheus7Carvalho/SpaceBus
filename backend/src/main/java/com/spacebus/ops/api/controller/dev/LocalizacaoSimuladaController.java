package com.spacebus.ops.api.controller.dev;

import com.spacebus.ops.domain.model.localizacao.PosicaoRequest;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Posição simulada para demonstração e testes. Só no perfil dev e só para Administrador (SecurityConfiguration). */
@RestController
@Profile({"dev", "e2e"})
@RequestMapping(value = "/api/dev/localizacao", produces = "application/json")
@Tag(name = "Desenvolvimento")
public class LocalizacaoSimuladaController {

    private final ILocalizacaoService service;

    public LocalizacaoSimuladaController(ILocalizacaoService service) {
        this.service = service;
    }

    @PostMapping("/{viagemId}")
    @Operation(description = "Grava uma posição simulada na viagem em andamento, pelo mesmo caminho do motorista.")
    public ResultadoEnvioResponse simular(@PathVariable String viagemId, @Valid @RequestBody PosicaoRequest posicao) {
        return service.registrarSimulada(viagemId, posicao);
    }
}
