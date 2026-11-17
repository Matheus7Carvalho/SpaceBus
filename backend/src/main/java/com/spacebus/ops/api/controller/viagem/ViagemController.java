package com.spacebus.ops.api.controller.viagem;

import com.spacebus.ops.domain.core.controller.BaseController;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.model.viagem.IViagemMapper;
import com.spacebus.ops.domain.model.viagem.ViagemRequest;
import com.spacebus.ops.domain.model.viagem.ViagemResponse;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Restrito a Administrador e Operador em SecurityConfiguration. A busca do passageiro terá endpoint próprio. */
@RestController
@RequestMapping(value = "/api/viagem", produces = "application/json")
@Tag(name = "Viagem", description = "Partidas de uma rota: horário, veículo, preço, capacidade e situação.")
public class ViagemController extends BaseController<Viagem, ViagemRequest, ViagemResponse> {

    private final IViagemService service;

    protected ViagemController(final IViagemMapper mapper, final IViagemService service) {
        super(mapper, service);
        this.service = service;
    }

    @PostMapping("/{id}/publicar")
    @Operation(description = "Publica a viagem (rascunho ou suspensa).")
    public ViagemResponse publicar(@PathVariable String id) {
        return getMapper().toResponse(service.publicar(id));
    }

    @PostMapping("/{id}/suspender")
    @Operation(description = "Suspende uma viagem publicada.")
    public ViagemResponse suspender(@PathVariable String id) {
        return getMapper().toResponse(service.suspender(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(description = "Cancela a viagem. Definitivo.")
    public ViagemResponse cancelar(@PathVariable String id) {
        return getMapper().toResponse(service.cancelar(id));
    }
}
