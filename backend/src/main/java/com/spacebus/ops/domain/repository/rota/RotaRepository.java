package com.spacebus.ops.domain.repository.rota;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Rota;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RotaRepository extends BaseRepository<Rota> {

    /** Nome já usado por outra rota. id nulo = cadastro novo. */
    @Query("select count(r) > 0 from Rota r where lower(r.nome) = lower(:nome) and (:id is null or r.id <> :id)")
    boolean existeNome(String nome, String id);
}
