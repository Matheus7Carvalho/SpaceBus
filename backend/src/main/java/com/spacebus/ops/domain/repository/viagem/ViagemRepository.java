package com.spacebus.ops.domain.repository.viagem;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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
