package com.spacebus.ops.api.controller.veiculo;

import com.spacebus.ops.domain.core.controller.BaseController;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.model.veiculo.IVeiculoMapper;
import com.spacebus.ops.domain.model.veiculo.VeiculoRequest;
import com.spacebus.ops.domain.model.veiculo.VeiculoResponse;
import com.spacebus.ops.domain.service.veiculo.IVeiculoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Restrito a Administrador e Operador em SecurityConfiguration. */
@RestController
@RequestMapping(value = "/api/veiculo", produces = "application/json")
@Tag(name = "Veículo", description = "Frota da transportadora.")
public class VeiculoController extends BaseController<Veiculo, VeiculoRequest, VeiculoResponse> {

    protected VeiculoController(final IVeiculoMapper mapper, final IVeiculoService service) {
        super(mapper, service);
    }
}
