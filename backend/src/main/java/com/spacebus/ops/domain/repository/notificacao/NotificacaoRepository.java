package com.spacebus.ops.domain.repository.notificacao;

import com.spacebus.ops.domain.core.repository.BaseRepository;
import com.spacebus.ops.domain.entity.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificacaoRepository extends BaseRepository<Notificacao> {

    /** Cria a notificação, a menos que a chave já exista (evento repetido ou simultâneo). Retorna 0 se já existia. */
    @Modifying
    @Query(value = """
            insert into notificacao (id, data_criacao, usuario_id, reserva_id, tipo, titulo, mensagem, link, chave)
            values (:id, cast(:agora as timestamp), :usuarioId, :reservaId, :tipo, :titulo, :mensagem, :link, :chave)
            on conflict (chave) do nothing""", nativeQuery = true)
    int inserirSeNova(String id, LocalDateTime agora, String usuarioId, String reservaId, String tipo, String titulo,
                      String mensagem, String link, String chave);

    @EntityGraph(attributePaths = "envios")
    Page<Notificacao> findByUsuarioIdOrderByDataCriacaoDescIdDesc(String usuarioId, Pageable pageable);

    Optional<Notificacao> findByIdAndUsuarioId(String id, String usuarioId);

    long countByUsuarioIdAndLidaEmIsNull(String usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lidaEm = :agora where n.usuarioId = :usuarioId and n.lidaEm is null")
    int marcarTodasComoLidas(String usuarioId, LocalDateTime agora);
}
