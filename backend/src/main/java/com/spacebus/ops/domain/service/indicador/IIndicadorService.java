package com.spacebus.ops.domain.service.indicador;

import com.spacebus.ops.domain.model.indicador.IndicadorViagemResponse;
import com.spacebus.ops.domain.model.indicador.ResumoIndicadoresResponse;
import org.springframework.data.domain.Page;

/**
 * Indicadores básicos da operação (escopo 4.6), calculados sob demanda. Datas em texto (AAAA-MM-DD): ausente ou
 * inválida vale o período padrão (hoje − 30 dias até hoje + 7 dias).
 */
public interface IIndicadorService {

    ResumoIndicadoresResponse resumo(String inicio, String fim, String rotaId);

    Page<IndicadorViagemResponse> porViagem(String inicio, String fim, String rotaId, int pagina);
}
