package com.spacebus.ops.domain.model.auth;

import com.spacebus.ops.domain.entity.enums.Perfil;

/**
 * Usuário da sessão e o fuso da operação. A tela calcula "hoje" e "agora" neste fuso, o mesmo do servidor,
 * e não no fuso do navegador (ressalva D5 do módulo 2).
 */
public record SessaoResponse(String id, String nome, String email, Perfil perfil, String fusoHorario) {
}
