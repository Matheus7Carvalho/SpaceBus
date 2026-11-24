package com.spacebus.ops.domain.repository.auditoria;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.AuditoriaAcesso;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaAcessoRepository extends BaseRepository<AuditoriaAcesso> {

    List<AuditoriaAcesso> findByRecursoIdOrderByDataCriacaoAsc(String recursoId);

    /**
     * Insere o acesso, a menos que já exista um do mesmo usuário, ação e recurso na mesma hora cheia (índice único
     * uk_auditoria_acesso_por_hora). Retorna 0 quando já havia registro.
     */
    @Modifying
    @Query(value = """
            insert into auditoria_acesso (id, data_criacao, usuario_id, perfil, acao, recurso_id)
            values (:id, cast(:agora as timestamp), :usuarioId, :perfil, :acao, :recursoId)
            on conflict do nothing""", nativeQuery = true)
    int inserirSeNovoNaHora(String id, LocalDateTime agora, String usuarioId, String perfil, String acao, String recursoId);
}
