package com.spacebus.ops.domain.service.usuario;

import com.spacebus.ops.domain.core.service.IBaseService;
import com.spacebus.ops.domain.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService extends IBaseService<Usuario> {
    Optional<Usuario> buscarPorEmail(String email);

    /** Motoristas ativos, por nome. Candidatos a conduzir uma viagem. */
    List<Usuario> motoristasAtivos();
}
