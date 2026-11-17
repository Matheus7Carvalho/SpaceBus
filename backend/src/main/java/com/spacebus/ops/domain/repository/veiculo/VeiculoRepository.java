package com.spacebus.ops.domain.repository.veiculo;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Veiculo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoRepository extends BaseRepository<Veiculo> {

    /** Placa já usada por outro veículo. id nulo = cadastro novo. */
    @Query("select count(v) > 0 from Veiculo v where v.placa = :placa and (:id is null or v.id <> :id)")
    boolean existePlaca(String placa, String id);
}
