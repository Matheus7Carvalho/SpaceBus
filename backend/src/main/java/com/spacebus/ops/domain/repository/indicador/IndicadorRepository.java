package com.spacebus.ops.domain.repository.indicador;

import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.indicador.IndicadorViagemResponse;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas agregadas dos indicadores (sem entidade): uma instrução por bloco, qualquer que seja o número de viagens.
 * Base: viagens não rascunho com partida em [inicio, fim), da rota se informada, com os lugares e o valor das
 * reservas confirmadas e os bloqueios vigentes (aguardando pagamento no prazo).
 */
@Repository
public class IndicadorRepository {

    private static final String BASE = """
            with confirmadas as (
                select viagem_id, sum(quantidade) as vendidos, sum(valor_total) as receita
                from reserva where status = 'CONFIRMADA' group by viagem_id),
            bloqueios as (
                select viagem_id, sum(quantidade) as bloqueados
                from reserva where status = 'AGUARDANDO_PAGAMENTO' and expira_em > :agora group by viagem_id),
            base as (
                select v.id, v.status, v.capacidade, v.data_hora_partida as partida, v.rota_id, v.veiculo_id,
                       coalesce(c.vendidos, 0) as vendidos, coalesce(c.receita, 0) as receita, coalesce(b.bloqueados, 0) as bloqueados
                from viagem v
                  left join confirmadas c on c.viagem_id = v.id
                  left join bloqueios b on b.viagem_id = v.id
                where v.status <> 'RASCUNHO' and v.data_hora_partida >= :inicio and v.data_hora_partida < :fim
                  and (cast(:rotaId as varchar) is null or v.rota_id = :rotaId))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public IndicadorRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Totais(ResumoIndicadoresResponse.Viagens viagens, long vendidos, long oferecidos, BigDecimal receita) {
    }

    private static MapSqlParameterSource parametros(LocalDateTime inicio, LocalDateTime fim, String rotaId, LocalDateTime agora) {
        return new MapSqlParameterSource().addValue("inicio", inicio).addValue("fim", fim).addValue("rotaId", rotaId).addValue("agora", agora);
    }

    /** Vendidos e oferecidos sem as canceladas; receita só das confirmadas (cancelada não tem confirmada). */
    public Totais totais(LocalDateTime inicio, LocalDateTime fim, String rotaId, LocalDateTime agora) {
        return jdbc.queryForObject(BASE + """
                select count(*) as total,
                       count(*) filter (where status = 'PUBLICADA') as publicadas,
                       count(*) filter (where status in ('EM_ANDAMENTO', 'ENCERRADA')) as realizadas,
                       count(*) filter (where status = 'CANCELADA') as canceladas,
                       count(*) filter (where vendidos > 0) as com_reserva,
                       coalesce(sum(vendidos) filter (where status <> 'CANCELADA'), 0) as vendidos,
                       coalesce(sum(capacidade) filter (where status <> 'CANCELADA'), 0) as oferecidos,
                       coalesce(sum(receita), 0) as receita
                from base""", parametros(inicio, fim, rotaId, agora), (rs, i) -> new Totais(
                new ResumoIndicadoresResponse.Viagens(rs.getLong("total"), rs.getLong("publicadas"), rs.getLong("realizadas"),
                        rs.getLong("canceladas"), rs.getLong("com_reserva")),
                rs.getLong("vendidos"), rs.getLong("oferecidos"), rs.getBigDecimal("receita")));
    }

    public record LinhaHorario(String horario, long viagens, long oferecidos, long vendidos) {
    }

    public List<LinhaHorario> porHorario(LocalDateTime inicio, LocalDateTime fim, String rotaId, LocalDateTime agora) {
        return jdbc.query(BASE + """
                select to_char(partida, 'HH24:MI') as horario, count(*) as viagens, sum(capacidade) as oferecidos, sum(vendidos) as vendidos
                from base where status <> 'CANCELADA'
                group by 1 order by 1""", parametros(inicio, fim, rotaId, agora),
                (rs, i) -> new LinhaHorario(rs.getString("horario"), rs.getLong("viagens"), rs.getLong("oferecidos"), rs.getLong("vendidos")));
    }

    /** Disponíveis = capacidade − confirmadas − bloqueios vigentes, nunca negativo (mesma regra das vagas). */
    public List<IndicadorViagemResponse> porViagem(LocalDateTime inicio, LocalDateTime fim, String rotaId, LocalDateTime agora,
                                                   int limite, long deslocamento) {
        var p = parametros(inicio, fim, rotaId, agora).addValue("limite", limite).addValue("deslocamento", deslocamento);
        return jdbc.query(BASE + """
                select b.id, b.status, b.capacidade, b.partida, b.vendidos, b.bloqueados, r.nome as rota_nome, ve.placa
                from base b join rota r on r.id = b.rota_id join veiculo ve on ve.id = b.veiculo_id
                order by b.partida desc, b.id
                limit :limite offset :deslocamento""", p, (rs, i) -> {
            var status = StatusViagem.valueOf(rs.getString("status"));
            var capacidade = rs.getInt("capacidade");
            var vendidos = rs.getLong("vendidos");
            var cancelada = status == StatusViagem.CANCELADA;
            var disponiveis = cancelada ? null : Math.max(0, capacidade - vendidos - rs.getLong("bloqueados"));
            var ocupacao = cancelada || capacidade == 0 ? null : (double) vendidos / capacidade;
            return new IndicadorViagemResponse(rs.getString("id"), rs.getTimestamp("partida").toLocalDateTime(), rs.getString("rota_nome"),
                    rs.getString("placa"), status, capacidade, vendidos, disponiveis, ocupacao);
        });
    }

    public long contarViagens(LocalDateTime inicio, LocalDateTime fim, String rotaId, LocalDateTime agora) {
        var total = jdbc.queryForObject(BASE + "select count(*) from base", parametros(inicio, fim, rotaId, agora), Long.class);
        return total == null ? 0 : total;
    }
}
