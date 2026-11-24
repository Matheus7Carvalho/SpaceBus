package com.spacebus.ops.domain.service.limpeza;

import com.spacebus.ops.domain.core.exception.DomainException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Apaga os dados deixados pelos testes E2E no banco de desenvolvimento (ressalva D8 do módulo 2). Só existe no
 * perfil dev. Alvo: só o que tem a marca: rotas e veículos cujo nome/descrição começa com ela, e tudo o que depende
 * deles (viagens, inclusive canceladas, reservas, pagamentos, histórico, localização), e os motoristas de teste
 * (nome com a marca). Os testes que criam viagem pela API usam vans de teste (descrição "E2E …"); nada sem a marca
 * é apagado.
 */
@Service
@Profile({"dev", "e2e"})
public class LimpezaDadosTesteService {

    /** Só marcas de teste: "E2E" ou "E2E-ABC123". Nunca um prefixo que pegue dados de demonstração. */
    private static final Pattern MARCA = Pattern.compile("E2E[A-Z0-9-]{0,17}");

    private static final String VIAGENS_ALVO = """
            select v.id from viagem v
              join rota r on r.id = v.rota_id
              join veiculo ve on ve.id = v.veiculo_id
            where r.nome like :prefixo or ve.descricao like :prefixo""";
    private static final String RESERVAS_ALVO = "select id from reserva where viagem_id in (" + VIAGENS_ALVO + ")";
    private static final String MOTORISTAS_ALVO = "select id from usuario where perfil = 'MOTORISTA' and nome like :prefixo";

    public record Resultado(int viagens, int rotas, int veiculos, int motoristas) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public LimpezaDadosTesteService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static boolean marcaValida(String marca) {
        return marca != null && MARCA.matcher(marca).matches();
    }

    @Transactional
    public Resultado limpar(String marca) {
        if (!marcaValida(marca)) {
            throw new DomainException("Informe uma marca de teste (ex.: E2E).");
        }
        var p = new MapSqlParameterSource().addValue("prefixo", marca + "%");
        jdbc.update("delete from reserva_historico where reserva_id in (" + RESERVAS_ALVO + ")", p);
        jdbc.update("delete from pagamento where reserva_id in (" + RESERVAS_ALVO + ")", p);
        jdbc.update("delete from reserva where viagem_id in (" + VIAGENS_ALVO + ")", p);
        jdbc.update("delete from localizacao_viagem where viagem_id in (" + VIAGENS_ALVO + ")", p);
        var viagens = jdbc.update("delete from viagem where id in (" + VIAGENS_ALVO + ")", p);
        jdbc.update("delete from rotina where veiculo_id in (select id from veiculo where descricao like :prefixo)", p);
        var rotas = jdbc.update("delete from rota where nome like :prefixo", p); // pontos e rotina em cascata
        var veiculos = jdbc.update("delete from veiculo where descricao like :prefixo", p);
        return new Resultado(viagens, rotas, veiculos, limparMotoristas(p));
    }

    /** Motorista de teste ainda ligado a uma viagem sem a marca (não deveria acontecer) perde só o vínculo. */
    private int limparMotoristas(MapSqlParameterSource p) {
        for (var coluna : new String[]{"motorista_id", "iniciada_por", "encerrada_por"}) {
            jdbc.update("update viagem set " + coluna + " = null where " + coluna + " in (" + MOTORISTAS_ALVO + ")", p);
        }
        jdbc.update("delete from auditoria_acesso where usuario_id in (" + MOTORISTAS_ALVO + ")", p);
        return jdbc.update("delete from usuario where id in (" + MOTORISTAS_ALVO + ")", p);
    }
}
