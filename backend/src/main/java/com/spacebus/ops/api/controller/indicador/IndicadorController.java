package com.spacebus.ops.api.controller.indicador;

import com.spacebus.ops.domain.model.indicador.IndicadorViagemResponse;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse;
import com.spacebus.ops.domain.service.indicador.IIndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura. Restrito a Administrador e Operador em SecurityConfiguration. */
@RestController
@RequestMapping(value = "/api/indicador", produces = "application/json")
@Tag(name = "Indicador", description = "Viagens, lugares vendidos e ocupação por horário e por viagem.")
public class IndicadorController {

    private final IIndicadorService service;

    public IndicadorController(IIndicadorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(description = "Resumo do período e ocupação por horário. Sem datas: hoje − 30 dias até hoje + 7 dias.")
    public ResumoIndicadoresResponse resumo(@RequestParam(required = false) String inicio,
                                            @RequestParam(required = false) String fim,
                                            @RequestParam(required = false) String rotaId) {
        return service.resumo(inicio, fim, rotaId);
    }

    @GetMapping("/viagem")
    @Operation(description = "Ocupação por viagem, partida mais recente primeiro (20 por página).")
    public Page<IndicadorViagemResponse> porViagem(@RequestParam(required = false) String inicio,
                                                   @RequestParam(required = false) String fim,
                                                   @RequestParam(required = false) String rotaId,
                                                   @RequestParam(defaultValue = "0") int page) {
        return service.porViagem(inicio, fim, rotaId, page);
    }
}
