package com.spacebus.ops.domain.model.indicador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Totais do período e ocupação por horário de partida. Ocupação = vendidos ÷ oferecidos (ponderada), de 0 a 1;
 * nula sem lugares oferecidos. Receita do pagamento simulado.
 */
public record ResumoIndicadoresResponse(LocalDate inicio, LocalDate fim, Viagens viagens, long vendidos, long oferecidos,
                                        Double ocupacao, BigDecimal receita, List<Horario> horarios) {

    /** Realizadas = em andamento + encerradas; com reserva = ao menos uma confirmada. Rascunho nunca entra. */
    public record Viagens(long total, long publicadas, long realizadas, long canceladas, long comReserva) {
    }

    /** Um horário de partida (HH:mm), sem viagens canceladas. */
    public record Horario(String horario, long viagens, long oferecidos, long vendidos, Double ocupacao) {
    }
}
