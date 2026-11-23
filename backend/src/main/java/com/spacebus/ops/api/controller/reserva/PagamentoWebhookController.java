package com.spacebus.ops.api.controller.reserva;

import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Retorno do provedor de pagamento (sem sessão: quem chama é o provedor). Autenticado pelo segredo
 * combinado com o provedor no cabeçalho; comparação em tempo constante.
 */
@RestController
@RequestMapping(value = "/api/pagamento/webhook", produces = "application/json")
@Tag(name = "Pagamento", description = "Retorno do provedor de pagamento.")
public class PagamentoWebhookController {

    public static final String CABECALHO_SEGREDO = "X-SpaceBus-Webhook-Segredo";

    private final IReservaService service;
    private final byte[] segredo;

    public PagamentoWebhookController(IReservaService service, @Value("${spacebus.pagamento.webhook-segredo}") String segredo) {
        // Segredo em branco aceitaria um cabeçalho vazio: o backend não sobe assim.
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException("Defina spacebus.pagamento.webhook-segredo (SPACEBUS_WEBHOOK_SEGREDO) com um valor não vazio.");
        }
        this.service = service;
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    @Operation(description = "Resultado de uma cobrança. Idempotente.")
    public ResponseEntity<Void> retorno(@RequestHeader(value = CABECALHO_SEGREDO, required = false) String segredoRecebido,
                                        @Valid @RequestBody RetornoPagamentoRequest retorno) {
        if (segredoRecebido == null || !MessageDigest.isEqual(segredo, segredoRecebido.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.processarRetorno(retorno.getProvedorId(), retorno.getResultado());
        return ResponseEntity.noContent().build();
    }
}
