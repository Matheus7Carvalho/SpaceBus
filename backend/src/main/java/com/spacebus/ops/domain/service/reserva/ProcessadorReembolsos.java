package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.ReservaHistorico;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.OrigemAcao;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaHistoricoRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.service.pagamento.ClienteProvedorPagamento;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamentoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pede ao provedor os reembolsos marcados como REEMBOLSO_PENDENTE, sempre fora de trava: a chamada externa acontece
 * entre duas transações curtas, e só o resultado trava a cobrança. Falha conta uma tentativa; no limite, o reembolso
 * fica para ação manual (log de erro). Repetir é seguro: o reembolso é idempotente no provedor.
 */
@Slf4j
@Component
public class ProcessadorReembolsos {

    private final PagamentoRepository pagamentoRepository;
    private final ReservaHistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteProvedorPagamento provedor;
    private final TransactionTemplate transacao;
    private final int maxTentativas;

    public ProcessadorReembolsos(PagamentoRepository pagamentoRepository, ReservaHistoricoRepository historicoRepository,
                                 UsuarioRepository usuarioRepository, ClienteProvedorPagamento provedor,
                                 PlatformTransactionManager transacoes,
                                 @Value("${spacebus.reembolso.max-tentativas:12}") int maxTentativas) {
        this.pagamentoRepository = pagamentoRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.provedor = provedor;
        this.transacao = new TransactionTemplate(transacoes);
        // Roda depois do commit de outra transação, que ainda está ligada à thread: sempre uma transação nova.
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.maxTentativas = maxTentativas;
    }

    /**
     * Primeira tentativa logo depois do commit que decidiu reembolsar. Um erro aqui não pode virar erro da
     * operação que já foi gravada (cancelamento, retorno do provedor): a tarefa agendada tenta de novo.
     */
    @TransactionalEventListener
    public void aoSolicitar(ReembolsoSolicitado evento) {
        try {
            tentar(evento.pagamentoId(), evento.origem(), evento.responsavelId());
        } catch (RuntimeException e) {
            log.error("Falha ao processar o reembolso do pagamento {}; a tarefa agendada tenta de novo.", evento.pagamentoId(), e);
        }
    }

    /** Novas tentativas (tarefa agendada). Devolve quantos foram concluídos. */
    public int tentarPendentes() {
        var concluidos = 0;
        for (var id : pagamentoRepository.reembolsosPendentes(maxTentativas)) {
            if (tentar(id, OrigemAcao.SISTEMA, null)) concluidos++;
        }
        return concluidos;
    }

    /** true se o reembolso foi concluído nesta tentativa. */
    boolean tentar(String pagamentoId, OrigemAcao origem, String responsavelId) {
        var provedorId = transacao.execute(s -> pagamentoRepository.findById(pagamentoId)
                .filter(p -> p.getStatus() == StatusPagamento.REEMBOLSO_PENDENTE && p.getTentativasReembolso() < maxTentativas)
                .map(Pagamento::getProvedorId)
                .orElse(null));
        if (provedorId == null) return false;
        try {
            provedor.reembolsar(provedorId);
        } catch (ProvedorPagamentoException e) {
            log.warn("Reembolso da cobrança {} falhou; fica pendente para nova tentativa.", provedorId, e);
            transacao.executeWithoutResult(s -> registrarFalha(pagamentoId, origem, responsavelId));
            return false;
        }
        return Boolean.TRUE.equals(transacao.execute(s -> registrarSucesso(pagamentoId, origem, responsavelId)));
    }

    private boolean registrarSucesso(String pagamentoId, OrigemAcao origem, String responsavelId) {
        var pagamento = pagamentoRepository.travar(pagamentoId).orElseThrow();
        if (pagamento.getStatus() != StatusPagamento.REEMBOLSO_PENDENTE) return false; // outra tentativa já concluiu
        pagamento.setStatus(StatusPagamento.REEMBOLSADO);
        registrar(pagamento, AcaoReserva.REEMBOLSADA, origem, responsavelId);
        return true;
    }

    private void registrarFalha(String pagamentoId, OrigemAcao origem, String responsavelId) {
        var pagamento = pagamentoRepository.travar(pagamentoId).orElseThrow();
        if (pagamento.getStatus() != StatusPagamento.REEMBOLSO_PENDENTE) return;
        pagamento.setTentativasReembolso(pagamento.getTentativasReembolso() + 1);
        // O passageiro vê "reembolso pendente" uma vez só, na primeira falha.
        if (pagamento.getTentativasReembolso() == 1) registrar(pagamento, AcaoReserva.REEMBOLSO_PENDENTE, origem, responsavelId);
        if (pagamento.getTentativasReembolso() >= maxTentativas) {
            log.error("Reembolso da cobrança {} falhou {} vezes e precisa de ação manual.", pagamento.getProvedorId(), maxTentativas);
        }
    }

    private void registrar(Pagamento pagamento, AcaoReserva acao, OrigemAcao origem, String responsavelId) {
        var responsavel = responsavelId == null ? null : usuarioRepository.getReferenceById(responsavelId);
        historicoRepository.save(new ReservaHistorico(pagamento.getReserva(), acao, origem, responsavel, pagamento.getProvedorId()));
    }
}
