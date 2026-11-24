package com.spacebus.ops.domain.service.auditoria;

import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.Perfil;

public interface IAuditoriaService {

    /** Grava o acesso antes de os dados serem entregues: sem registro, sem acesso. */
    void registrar(String usuarioId, Perfil perfil, AcaoAuditada acao, String recursoId);
}
