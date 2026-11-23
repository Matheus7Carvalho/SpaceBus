package com.spacebus.ops.domain.repository.reserva;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends BaseRepository<Pagamento> {

    /** Lê a cobrança já travada. Chamar depois de travar a viagem e a reserva. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pagamento p where p.provedorId = :provedorId")
    Optional<Pagamento> travarPorProvedorId(String provedorId);

    Optional<Pagamento> findByProvedorId(String provedorId);

    /** Viagem e reserva da cobrança, sem carregar entidades (para travar na ordem viagem → reserva → pagamento). */
    @Query("select p.reserva.viagem.id, p.reserva.id from Pagamento p where p.provedorId = :provedorId")
    List<Object[]> viagemEReservaDaCobranca(String provedorId);

    @Query("select count(p) > 0 from Pagamento p where p.provedorId = :provedorId and p.reserva.passageiro.id = :passageiroId")
    boolean cobrancaDoPassageiro(String provedorId, String passageiroId);

    Optional<Pagamento> findFirstByReservaIdAndStatus(String reservaId, StatusPagamento status);

    Optional<Pagamento> findFirstByReservaIdOrderByDataCriacaoDesc(String reservaId);

    List<Pagamento> findByReservaIdAndStatus(String reservaId, StatusPagamento status);

    /** Reembolsos que o provedor ainda não concluiu, travados; os já em tratamento ficam para a próxima rodada. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select p from Pagamento p where p.status = com.spacebus.ops.domain.entity.enums.StatusPagamento.REEMBOLSO_PENDENTE")
    List<Pagamento> travarReembolsosPendentes();
}
