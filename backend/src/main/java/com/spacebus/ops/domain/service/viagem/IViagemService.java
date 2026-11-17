package com.spacebus.ops.domain.service.viagem;

import com.spacebus.ops.domain.core.service.IBaseService;
import com.spacebus.ops.domain.entity.Viagem;

public interface IViagemService extends IBaseService<Viagem> {

    /** RASCUNHO ou SUSPENSA → PUBLICADA. Revalida as regras da viagem. */
    Viagem publicar(String id);

    /** PUBLICADA → SUSPENSA: some da busca, mas pode voltar a ser publicada. */
    Viagem suspender(String id);

    /** Qualquer situação → CANCELADA. Definitivo. */
    Viagem cancelar(String id);
}
