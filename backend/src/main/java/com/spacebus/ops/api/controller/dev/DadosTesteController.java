package com.spacebus.ops.api.controller.dev;

import com.spacebus.ops.domain.service.limpeza.LimpezaDadosTesteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Limpeza dos dados de teste E2E. Só no perfil dev e só para Administrador (SecurityConfiguration). */
@RestController
@Profile("dev")
@RequestMapping(value = "/api/dev/dados-teste", produces = "application/json")
@Tag(name = "Desenvolvimento", description = "Apoio aos testes automatizados; ausente fora do perfil dev.")
public class DadosTesteController {

    private final LimpezaDadosTesteService service;

    public DadosTesteController(LimpezaDadosTesteService service) {
        this.service = service;
    }

    @DeleteMapping
    @Operation(description = "Apaga rotas e veículos com a marca, com suas viagens, reservas, pagamentos e histórico.")
    public LimpezaDadosTesteService.Resultado limpar(@RequestParam String marca) {
        return service.limpar(marca);
    }
}
