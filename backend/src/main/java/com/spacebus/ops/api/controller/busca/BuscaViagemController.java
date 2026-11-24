package com.spacebus.ops.api.controller.busca;

import com.spacebus.ops.domain.model.busca.BuscaViagemRequest;
import com.spacebus.ops.domain.model.busca.DetalheViagemResponse;
import com.spacebus.ops.domain.model.busca.ViagemEncontradaResponse;
import com.spacebus.ops.domain.service.busca.IBuscaViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta do passageiro. Somente leitura, aberta a todo usuário autenticado; não expõe dados internos
 * da operação (motorista, situação, viagens não publicadas).
 */
@RestController
@RequestMapping(value = "/api/busca", produces = "application/json")
@Tag(name = "Busca de viagens", description = "Consulta de viagens publicadas pelo passageiro.")
public class BuscaViagemController {

    private final IBuscaViagemService service;

    public BuscaViagemController(IBuscaViagemService service) {
        this.service = service;
    }

    @GetMapping("/viagens")
    @Operation(description = "Viagens publicadas com partida futura que passam pela origem e, depois, pelo destino, na data.")
    public List<ViagemEncontradaResponse> buscar(@Valid @ModelAttribute BuscaViagemRequest busca) {
        return service.buscar(busca);
    }

    @GetMapping("/viagens/{id}")
    @Operation(description = "Detalhe de uma viagem publicada com partida futura; 404 se não estiver mais disponível.")
    public DetalheViagemResponse detalhar(@PathVariable String id) {
        return service.detalhar(id);
    }

    @GetMapping("/locais")
    @Operation(description = "Locais atendidos (pontos de rotas com viagem publicada futura), para sugestão.")
    public List<String> locais() {
        return service.locais();
    }
}
