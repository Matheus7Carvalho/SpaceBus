package com.spacebus.ops.domain.repository.rota;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Rotina;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RotinaRepository extends BaseRepository<Rotina> {

    /** Rotinas com vigência em aberto ou terminando hoje ou depois: as que ainda geram viagens. */
    @Query("select r from Rotina r where r.fim is null or r.fim >= :hoje")
    List<Rotina> vigentes(LocalDate hoje);

    /** Rotina vigente que usa o veículo. */
    @Query("select count(r) > 0 from Rotina r where r.veiculo.id = :veiculoId and (r.fim is null or r.fim >= :hoje)")
    boolean existeVigenteComVeiculo(String veiculoId, LocalDate hoje);

    /** Maior capacidade entre as rotinas vigentes do veículo (0 se não houver). */
    @Query("select coalesce(max(r.capacidade), 0) from Rotina r where r.veiculo.id = :veiculoId and (r.fim is null or r.fim >= :hoje)")
    int maiorCapacidadeVigente(String veiculoId, LocalDate hoje);
}
