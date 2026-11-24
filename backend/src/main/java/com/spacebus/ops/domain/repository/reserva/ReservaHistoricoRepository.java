package com.spacebus.ops.domain.repository.reserva;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.ReservaHistorico;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaHistoricoRepository extends BaseRepository<ReservaHistorico> {

    List<ReservaHistorico> findByReservaIdOrderByOrdemAsc(String reservaId);
}
