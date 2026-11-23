package com.spacebus.ops.api.controller.reserva;

import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * "Página do provedor" do pagamento simulado: o passageiro dono da cobrança aprova ou recusa,
 * e o resultado entra pelo mesmo caminho do webhook. Só existe com o provedor simulado.
 */
@RestController
@RequestMapping(value = "/api/provedor-simulado/cobrancas", produces = "application/json")
@Tag(name = "Provedor simulado", description = "Pagamento de teste do piloto.")
@ConditionalOnProperty(name = "spacebus.pagamento.provedor", havingValue = "simulado")
public class ProvedorSimuladoController {

    public record CobrancaSimulada(String provedorId, String reservaId, String reservaCodigo, BigDecimal valor, StatusPagamento status) {
    }

    private final PagamentoRepository pagamentos;
    private final IReservaService reservas;

    public ProvedorSimuladoController(PagamentoRepository pagamentos, IReservaService reservas) {
        this.pagamentos = pagamentos;
        this.reservas = reservas;
    }

    @GetMapping("/{provedorId}")
    @Transactional(readOnly = true)
    public CobrancaSimulada cobranca(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String provedorId) {
        var p = doPassageiro(provedorId, usuario);
        return new CobrancaSimulada(p.getProvedorId(), p.getReserva().getId(), p.getReserva().getCodigo(), p.getValor(), p.getStatus());
    }

    /**
     * Sem transação aqui: o dono é conferido por consulta escalar e o retorno abre a própria transação,
     * que trava viagem → reserva → pagamento antes de ler qualquer dado.
     */
    @PostMapping("/{provedorId}/aprovar")
    @Operation(description = "Simula o retorno de pagamento aprovado.")
    public ResponseEntity<Void> aprovar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String provedorId) {
        exigirDono(provedorId, usuario);
        reservas.processarRetorno(provedorId, RetornoPagamentoRequest.Resultado.APROVADO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{provedorId}/recusar")
    @Operation(description = "Simula o retorno de pagamento recusado.")
    public ResponseEntity<Void> recusar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String provedorId) {
        exigirDono(provedorId, usuario);
        reservas.processarRetorno(provedorId, RetornoPagamentoRequest.Resultado.RECUSADO);
        return ResponseEntity.noContent().build();
    }

    private void exigirDono(String provedorId, UsuarioAutenticado usuario) {
        if (!pagamentos.cobrancaDoPassageiro(provedorId, usuario.getId())) throw new EntityNotFoundException("Cobrança não encontrada.");
    }

    /** Cobrança de outro passageiro é "não encontrada". */
    private Pagamento doPassageiro(String provedorId, UsuarioAutenticado usuario) {
        return pagamentos.findByProvedorId(provedorId)
                .filter(p -> p.getReserva().getPassageiro().getId().equals(usuario.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Cobrança não encontrada."));
    }
}
