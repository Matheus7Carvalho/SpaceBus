package com.spacebus.ops.domain.model.reserva;

import com.spacebus.ops.domain.entity.enums.MotivoCancelamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Reserva vista pelo próprio passageiro. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponse {
    private String id;
    private String codigo;
    private StatusReserva status;
    private MotivoCancelamento motivoCancelamento;
    private String viagemId;
    private String rotaNome;
    private LocalDateTime partida;
    private String embarqueNome;
    private String embarqueReferencia;
    private String desembarqueNome;
    private Integer quantidade;
    private BigDecimal valorTotal;
    private LocalDateTime expiraEm;
    private LocalDateTime confirmadaEm;
    private LocalDateTime canceladaEm;
    /** Pode cancelar agora (situação e prazo). */
    private boolean cancelavel;
    /** Pode (re)iniciar o pagamento agora. */
    private boolean pagavel;
    /** Último pagamento (ou nulo). */
    private PagamentoResponse pagamento;
    /** Do mais antigo ao mais recente. */
    private List<EventoReservaResponse> historico;
    private LocalDateTime criadaEm;
}
