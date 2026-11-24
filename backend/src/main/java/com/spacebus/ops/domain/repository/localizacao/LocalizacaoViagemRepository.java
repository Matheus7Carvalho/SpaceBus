package com.spacebus.ops.domain.repository.localizacao;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.LocalizacaoViagem;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Gravação só por SQL condicional, nunca por comparação em memória: requisições simultâneas da mesma viagem passam
 * pela trava da linha, e o PostgreSQL reavalia o WHERE com a versão mais nova antes de atualizar.
 * Chamar sempre depois de travar a viagem (ordem viagem → localização).
 */
@Repository
public interface LocalizacaoViagemRepository extends BaseRepository<LocalizacaoViagem> {

    Optional<LocalizacaoViagem> findByViagemId(String viagemId);

    List<LocalizacaoViagem> findByViagemIdIn(Collection<String> viagemIds);

    /** Cria a linha da viagem na primeira posição; se já existir (ou outra requisição criar junto), não faz nada. */
    @Modifying
    @Query(value = """
            insert into localizacao_viagem (id, data_criacao, viagem_id) values (:id, :agora, :viagemId)
            on conflict (viagem_id) do nothing""", nativeQuery = true)
    void garantirLinha(String id, String viagemId, LocalDateTime agora);

    /**
     * Grava a posição só se for mais nova que a gravada e respeitar o intervalo mínimo ({@code limiteFrequente} = hora
     * da posição − intervalo). Conta aceita, lacuna e maior intervalo na mesma instrução (o lado direito do SET usa a
     * linha antiga). Retorna 0 quando a posição foi ignorada (fora de ordem ou frequente).
     */
    @Modifying
    @Query(value = """
            update localizacao_viagem set
              lacunas = lacunas + case when registrada_em is not null
                  and extract(epoch from (cast(:registrada as timestamp) - registrada_em)) > :lacunaSegundos then 1 else 0 end,
              maior_intervalo_s = greatest(maior_intervalo_s,
                  coalesce(cast(extract(epoch from (cast(:registrada as timestamp) - registrada_em)) as integer), 0)),
              latitude = :latitude, longitude = :longitude, precisao_m = :precisao,
              registrada_em = cast(:registrada as timestamp), recebida_em = cast(:agora as timestamp),
              simulada = :simulada, aceitas = aceitas + 1, data_atualizacao = cast(:agora as timestamp)
            where viagem_id = :viagemId
              and (registrada_em is null or registrada_em <= cast(:limiteFrequente as timestamp))""", nativeQuery = true)
    int gravarSeMaisNova(String viagemId, double latitude, double longitude, double precisao, LocalDateTime registrada,
                         LocalDateTime limiteFrequente, LocalDateTime agora, boolean simulada, int lacunaSegundos);

    /** Soma rejeições (por motivo) e ignoradas do lote; grava o último motivo quando houver rejeição. */
    @Modifying
    @Query(value = """
            update localizacao_viagem set
              rejeitadas_adiantada = rejeitadas_adiantada + :adiantadas,
              rejeitadas_antiga = rejeitadas_antiga + :antigas,
              rejeitadas_imprecisa = rejeitadas_imprecisa + :imprecisas,
              ignoradas = ignoradas + :ignoradas,
              ultimo_motivo_rejeicao = coalesce(cast(:motivo as varchar), ultimo_motivo_rejeicao),
              ultima_rejeicao_em = case when cast(:motivo as varchar) is null then ultima_rejeicao_em else cast(:agora as timestamp) end
            where viagem_id = :viagemId""", nativeQuery = true)
    void contar(String viagemId, int adiantadas, int antigas, int imprecisas, int ignoradas, String motivo, LocalDateTime agora);

    /**
     * Apaga as coordenadas das viagens encerradas até {@code limite}; contadores e horários ficam. SKIP LOCKED: uma
     * linha ocupada agora fica para a próxima rodada.
     */
    @Modifying
    @Query(value = """
            update localizacao_viagem set latitude = null, longitude = null, precisao_m = null, expurgada_em = :agora
            where id in (select l.id from localizacao_viagem l join viagem v on v.id = l.viagem_id
                         where v.status = 'ENCERRADA' and v.encerrada_em <= :limite and l.latitude is not null
                         for update of l skip locked)""", nativeQuery = true)
    int expurgarEncerradasAte(LocalDateTime limite, LocalDateTime agora);
}
