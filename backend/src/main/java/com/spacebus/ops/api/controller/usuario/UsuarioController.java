package com.spacebus.ops.api.controller.usuario;

import com.spacebus.ops.domain.model.usuario.IUsuarioMapper;
import com.spacebus.ops.domain.model.usuario.UsuarioResponse;
import com.spacebus.ops.domain.service.usuario.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultas de usuários para montar cadastros (sem CRUD de usuários nesta etapa).
 * Restrito a Administrador e Operador em SecurityConfiguration.
 */
@RestController
@RequestMapping(value = "/api/usuario", produces = "application/json")
@Tag(name = "Usuário", description = "Consultas de usuários.")
public class UsuarioController {

    private final IUsuarioService service;
    private final IUsuarioMapper mapper;

    public UsuarioController(IUsuarioService service, IUsuarioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/motoristas")
    @Operation(description = "Motoristas ativos, candidatos a conduzir uma viagem.")
    public List<UsuarioResponse> motoristas() {
        return mapper.toListResponse(service.motoristasAtivos());
    }
}
