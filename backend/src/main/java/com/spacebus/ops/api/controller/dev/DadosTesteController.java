package com.spacebus.ops.api.controller.dev;

import com.spacebus.ops.domain.service.limpeza.LimpezaDadosTesteService;
import com.spacebus.ops.domain.service.limpeza.MotoristaTesteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dados dos testes E2E. Só no perfil dev e só para Administrador (SecurityConfiguration). */
@RestController
@Profile({"dev", "e2e"})
@RequestMapping(value = "/api/dev/dados-teste", produces = "application/json")
@Tag(name = "Desenvolvimento", description = "Apoio aos testes automatizados; ausente fora do perfil dev.")
public class DadosTesteController {

    private final LimpezaDadosTesteService service;
    private final MotoristaTesteService motoristas;

    public DadosTesteController(LimpezaDadosTesteService service, MotoristaTesteService motoristas) {
        this.service = service;
        this.motoristas = motoristas;
    }

    @DeleteMapping
    @Operation(description = "Apaga rotas, veículos e motoristas com a marca, com suas viagens, reservas, pagamentos, histórico e localização.")
    public LimpezaDadosTesteService.Resultado limpar(@RequestParam String marca) {
        return service.limpar(marca);
    }

    @PostMapping("/motorista")
    @Operation(description = "Cria um motorista só para um teste (nome com a marca, senha das contas de demonstração).")
    public MotoristaTesteService.MotoristaTeste criarMotorista(@RequestParam String marca) {
        return motoristas.criar(marca);
    }
}
