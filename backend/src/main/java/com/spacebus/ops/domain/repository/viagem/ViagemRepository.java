package com.spacebus.ops.domain.repository.viagem;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemRepository extends BaseRepository<Viagem> {

    boolean existsByVeiculoId(String veiculoId);

    /**
     * Trava a linha da viagem até o fim da transação (SELECT ... FOR UPDATE). Toda reserva e confirmação
     * da mesma viagem passa por aqui, então a conta de lugares nunca é feita por duas transações ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Viagem v where v.id = :id")
    Optional<Viagem> travar(String id);

    /** O horário já foi gerado por esta rotina (em qualquer situação, inclusive cancelada). */
    boolean existsByRotinaIdAndPartida(String rotinaId, LocalDateTime partida);

    /** Remove os rascunhos futuros gerados pela rotina (nunca foram oferecidos ao passageiro). */
    // Sem clearAutomatically: limpar o contexto desanexaria a rota que está sendo salva na mesma transação.
    @Modifying(flushAutomatically = true)
    @Query("delete from Viagem v where v.rotina.id = :rotinaId and v.status = :rascunho and v.partida > :agora")
    int excluirRascunhosFuturos(String rotinaId, LocalDateTime agora, StatusViagem rascunho);

    boolean existsByRotaId(String rotaId);

    /** Outra viagem não cancelada do mesmo veículo no mesmo horário. id nulo = cadastro novo. */
    @Query("""
            select count(v) > 0 from Viagem v
            where v.veiculo.id = :veiculoId and v.partida = :partida and v.status <> :cancelada
              and (:id is null or v.id <> :id)""")
    boolean existeConflitoVeiculo(String veiculoId, LocalDateTime partida, String id, StatusViagem cancelada);

    /**
     * Viagens na situação informada com partida no intervalo [inicio, fim) e depois de agora, por horário.
     * Rota, pontos, rotina e veículo vêm na mesma consulta (a busca lê todos). Sem o fetch da rotina, a rota sem
     * rotina (OneToOne inverso, EAGER) gera uma consulta por viagem: o Hibernate não guarda o "nulo".
     */
    @Query("""
            select v from Viagem v
              join fetch v.rota r join fetch r.pontos left join fetch r.rotina join fetch v.veiculo left join fetch v.motorista
            where v.status = :status and v.partida >= :inicio and v.partida < :fim and v.partida > :agora
            order by v.partida""")
    List<Viagem> publicadasEntre(LocalDateTime inicio, LocalDateTime fim, LocalDateTime agora, StatusViagem status);

    /** Com rota e pontos na mesma consulta (a lista de locais lê todos). */
    @Query("""
            select v from Viagem v
              join fetch v.rota r join fetch r.pontos left join fetch r.rotina join fetch v.veiculo left join fetch v.motorista
            where v.status = :status and v.partida > :agora""")
    List<Viagem> publicadasFuturas(LocalDateTime agora, StatusViagem status);

    /**
     * Agenda do operador: partida a partir de {@code inicio} e, se informado, antes de {@code fim};
     * rota e situação opcionais. Os opcionais vêm como texto para o PostgreSQL saber o tipo do parâmetro nulo.
     * Rota, rotina e veículo vêm junto (só associações simples: a paginação continua no banco).
     */
    @Query(value = """
            select v from Viagem v
              join fetch v.rota r left join fetch r.rotina join fetch v.veiculo left join fetch v.motorista
            where v.partida >= :inicio and v.partida < :fim
              and (:rotaId is null or v.rota.id = :rotaId)
              and (:status is null or cast(v.status as string) = :status)""",
            countQuery = """
            select count(v) from Viagem v
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
