package com.spacebus.ops.domain.service.auditoria;

import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.Perfil;

import java.time.LocalDateTime;

public interface IAuditoriaService {

    /** Grava o acesso antes de os dados serem entregues: sem registro, sem acesso. */
    void registrar(String usuarioId, Perfil perfil, AcaoAuditada acao, String recursoId);

    /**
     * Grava o acesso no máximo uma vez por usuário, ação, recurso e hora cheia de {@code agora} (consultas repetidas
     * por atualização automática não viram um registro cada). Garantido no banco, inclusive com consultas simultâneas.
     */
    void registrarUmaVezPorHora(String usuarioId, Perfil perfil, AcaoAuditada acao, String recursoId, LocalDateTime agora);
}
