package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.ReservaHistorico;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest;

import java.util.List;
import java.util.Optional;

/**
 * Reserva e pagamento (escopo 4.3). Toda operação que muda ocupação ou situação trava primeiro a viagem
 * e só então lê a reserva e o pagamento (ordem viagem → reserva → pagamento). Toda mudança grava histórico.
 */
public interface IReservaService {

    /** Bloqueia os lugares por alguns minutos, aguardando pagamento. Nunca vende além da capacidade. */
    Reserva reservar(String passageiroId, ReservaRequest pedido);

    List<Reserva> minhas(String passageiroId);

    /** Só do próprio passageiro; de outro passageiro é "não encontrada". */
    Reserva minha(String id, String passageiroId);

    /** Cria a cobrança no provedor ou devolve a pendente (nunca duas pendentes). */
    Pagamento iniciarPagamento(String reservaId, String passageiroId);

    /** Retorno do provedor. Idempotente: o mesmo retorno repetido não muda nada. */
    void processarRetorno(String provedorId, RetornoPagamentoRequest.Resultado resultado);

    /** Passageiro cancela (até o prazo antes da partida); pagamento aprovado é reembolsado. */
    Reserva cancelar(String reservaId, String passageiroId);

    /** A transportadora cancelou a viagem (já travada pelo chamador): cancela as reservas ativas e reembolsa as pagas. */
    void cancelarPorViagem(String viagemId, String responsavelId);

    List<Reserva> daViagem(String viagemId);

    List<ReservaHistorico> historico(String reservaId);

    /** Capacidade − (confirmadas + aguardando dentro do prazo). */
    int lugaresDisponiveis(Viagem viagem);

    int lugaresOcupados(String viagemId);

    Optional<Pagamento> ultimoPagamento(String reservaId);

    boolean cancelavel(Reserva reserva);

    boolean pagavel(Reserva reserva);

    /** Marca como expiradas as reservas sem pagamento vencidas. Retorna quantas. */
    int expirarVencidas();

    /** Tenta de novo os reembolsos que o provedor não concluiu. Retorna quantos foram concluídos. */
    int tentarReembolsosPendentes();
}
