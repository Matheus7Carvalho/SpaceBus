package com.spacebus.ops.domain.repository.reserva;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Ordem das travas em todo o módulo: viagem → reserva → pagamento. Quem precisa da viagem de uma reserva
 * descobre o id por consulta escalar (sem carregar a reserva), trava a viagem e só então lê a reserva.
 */
@Repository
public interface ReservaRepository extends BaseRepository<Reserva> {

    /**
     * Lugares que seguram vaga na viagem: confirmadas + aguardando pagamento dentro do prazo.
     * {@code exceto} tira uma reserva da conta (a que está sendo confirmada fora do prazo).
     */
    @Query("""
            select coalesce(sum(r.quantidade), 0) from Reserva r
            where r.viagem.id = :viagemId
              and (r.status = com.spacebus.ops.domain.entity.enums.StatusReserva.CONFIRMADA
                   or (r.status = com.spacebus.ops.domain.entity.enums.StatusReserva.AGUARDANDO_PAGAMENTO and r.expiraEm > :agora))
              and r.id <> :exceto""")
    int lugaresOcupados(String viagemId, LocalDateTime agora, String exceto);

    boolean existsByCodigo(String codigo);

    List<Reserva> findByPassageiroIdOrderByDataCriacaoDesc(String passageiroId);

    Optional<Reserva> findByIdAndPassageiroId(String id, String passageiroId);

    List<Reserva> findByViagemIdOrderByDataCriacao(String viagemId);

    List<Reserva> findByViagemIdAndStatusIn(String viagemId, Collection<StatusReserva> status);

    /** Viagem de uma reserva do passageiro, sem carregar a reserva (para travar a viagem antes de lê-la). */
    @Query("select r.viagem.id from Reserva r where r.id = :id and r.passageiro.id = :passageiroId")
    Optional<String> viagemDaReservaDoPassageiro(String id, String passageiroId);

    /** Lê a reserva já com a linha travada. Chamar depois de travar a viagem. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> travar(String id);

    /**
     * Reservas vencidas ainda aguardando pagamento, travadas. SKIP LOCKED: as que estão sendo tratadas
     * agora (ex.: retorno do provedor) ficam para a próxima rodada, sem esperar e sem deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select r from Reserva r
            where r.status = com.spacebus.ops.domain.entity.enums.StatusReserva.AGUARDANDO_PAGAMENTO and r.expiraEm <= :agora""")
    List<Reserva> travarVencidas(LocalDateTime agora);
}
