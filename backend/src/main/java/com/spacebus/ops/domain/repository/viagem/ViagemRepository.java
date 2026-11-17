package com.spacebus.ops.domain.repository.viagem;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViagemRepository extends BaseRepository<Viagem> {

    boolean existsByVeiculoId(String veiculoId);

    boolean existsByRotaId(String rotaId);

    /** Outra viagem não cancelada do mesmo veículo no mesmo horário. id nulo = cadastro novo. */
    @Query("""
            select count(v) > 0 from Viagem v
            where v.veiculo.id = :veiculoId and v.partida = :partida and v.status <> :cancelada
              and (:id is null or v.id <> :id)""")
    boolean existeConflitoVeiculo(String veiculoId, LocalDateTime partida, String id, StatusViagem cancelada);

    /** Viagens na situação informada com partida no intervalo [inicio, fim) e depois de agora, por horário. */
    @Query("""
            select v from Viagem v
            where v.status = :status and v.partida >= :inicio and v.partida < :fim and v.partida > :agora
            order by v.partida""")
    List<Viagem> publicadasEntre(LocalDateTime inicio, LocalDateTime fim, LocalDateTime agora, StatusViagem status);

    @Query("select v from Viagem v where v.status = :status and v.partida > :agora")
    List<Viagem> publicadasFuturas(LocalDateTime agora, StatusViagem status);

    /**
     * Agenda do operador: partida a partir de {@code inicio} e, se informado, antes de {@code fim};
     * rota e situação opcionais. Os opcionais vêm como texto para o PostgreSQL saber o tipo do parâmetro nulo.
     */
    @Query("""
            select v from Viagem v
            where v.partida >= :inicio and v.partida < :fim
              and (:rotaId is null or v.rota.id = :rotaId)
              and (:status is null or cast(v.status as string) = :status)""")
    Page<Viagem> agenda(LocalDateTime inicio, LocalDateTime fim, String rotaId, String status, Pageable pageable);

    /** Há viagem futura não cancelada com este veículo. */
    @Query("""
            select count(v) > 0 from Viagem v
            where v.veiculo.id = :veiculoId and v.partida > :agora and v.status <> :cancelada""")
    boolean existeViagemFutura(String veiculoId, LocalDateTime agora, StatusViagem cancelada);

    /** Maior capacidade entre as viagens futuras não canceladas do veículo (0 se não houver). */
    @Query("""
            select coalesce(max(v.capacidade), 0) from Viagem v
            where v.veiculo.id = :veiculoId and v.partida > :agora and v.status <> :cancelada""")
    int maiorCapacidadeFutura(String veiculoId, LocalDateTime agora, StatusViagem cancelada);
}
