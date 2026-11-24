package com.spacebus.ops.domain.repository.auditoria;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.AuditoriaAcesso;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaAcessoRepository extends BaseRepository<AuditoriaAcesso> {

    List<AuditoriaAcesso> findByRecursoIdOrderByDataCriacaoAsc(String recursoId);
}
