package com.spacebus.ops.domain.service.viagem;

import com.spacebus.ops.domain.core.service.IBaseService;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface IViagemService extends IBaseService<Viagem> {

    /** RASCUNHO ou SUSPENSA → PUBLICADA. Revalida as regras da viagem. */
    Viagem publicar(String id);

    /** PUBLICADA → SUSPENSA: some da busca, mas pode voltar a ser publicada. */
    Viagem suspender(String id);

    /** Rascunho, publicada ou suspensa → CANCELADA. Definitivo. Cancela as reservas ativas em nome do responsável (histórico). */
    Viagem cancelar(String id, String responsavelId);

    /**
     * PUBLICADA → EM_ANDAMENTO, dentro da janela de início. {@code motoristaId} informado: quem pede é motorista e só
     * inicia a viagem atribuída a ele; nulo: operação (sem conferir o motorista).
     */
    Viagem iniciar(String id, String responsavelId, String motoristaId);

    /** EM_ANDAMENTO → ENCERRADA (definitivo). {@code motoristaId} como em {@link #iniciar}. */
    Viagem encerrar(String id, String responsavelId, String motoristaId);

    /** Motivo de a viagem não poder ser iniciada agora (situação ou janela); vazio se pode. */
    Optional<String> impedimentoParaIniciar(Viagem viagem);

    /** Encerra as viagens esquecidas em andamento (iniciadas há mais que o limite). Retorna quantas. */
    int encerrarEsquecidas();

    /**
     * Agenda do operador filtrada no servidor. Com dia: as viagens daquele dia (inclusive passadas);
     * sem dia: de hoje em diante. Rota e situação opcionais.
     */
    Page<Viagem> agenda(LocalDate dia, String rotaId, StatusViagem status, Pageable pageable);
}
