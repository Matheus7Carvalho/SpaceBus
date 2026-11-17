package com.spacebus.ops.api.controller.rota;

import com.spacebus.ops.domain.core.controller.BaseController;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.model.rota.IRotaMapper;
import com.spacebus.ops.domain.model.rota.RotaRequest;
import com.spacebus.ops.domain.model.rota.RotaResponse;
import com.spacebus.ops.domain.service.rota.IRotaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Restrito a Administrador e Operador em SecurityConfiguration. */
@RestController
@RequestMapping(value = "/api/rota", produces = "application/json")
@Tag(name = "Rota", description = "Percursos com pontos de parada ordenados.")
public class RotaController extends BaseController<Rota, RotaRequest, RotaResponse> {

    protected RotaController(final IRotaMapper mapper, final IRotaService service) {
        super(mapper, service);
    }
}
