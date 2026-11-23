package com.spacebus.ops.domain.service.viagem;

import com.spacebus.ops.domain.core.service.IBaseService;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IViagemService extends IBaseService<Viagem> {

    /** RASCUNHO ou SUSPENSA → PUBLICADA. Revalida as regras da viagem. */
    Viagem publicar(String id);

    /** PUBLICADA → SUSPENSA: some da busca, mas pode voltar a ser publicada. */
    Viagem suspender(String id);

    /** Qualquer situação → CANCELADA. Definitivo. Cancela as reservas ativas em nome do responsável (histórico). */
    Viagem cancelar(String id, String responsavelId);

    /**
     * Agenda do operador filtrada no servidor. Com dia: as viagens daquele dia (inclusive passadas);
     * sem dia: de hoje em diante. Rota e situação opcionais.
     */
    Page<Viagem> agenda(LocalDate dia, String rotaId, StatusViagem status, Pageable pageable);
}
