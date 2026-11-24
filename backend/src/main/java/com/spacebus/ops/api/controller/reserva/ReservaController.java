package com.spacebus.ops.api.controller.reserva;

import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoResponse;
import com.spacebus.ops.domain.model.reserva.PagamentoResponse;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.ReservaResponse;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reservas do passageiro logado. Restrito ao perfil Passageiro em SecurityConfiguration; cada um só vê as suas. */
@RestController
@RequestMapping(value = "/api/reserva", produces = "application/json")
@Tag(name = "Reserva", description = "Reserva de lugares e pagamento pelo passageiro.")
public class ReservaController {

    private final IReservaService service;
    private final ReservaRespostas respostas;
    private final ILocalizacaoService localizacao;

    public ReservaController(IReservaService service, ReservaRespostas respostas, ILocalizacaoService localizacao) {
        this.service = service;
        this.respostas = respostas;
        this.localizacao = localizacao;
    }

    @PostMapping
    @Operation(description = "Bloqueia os lugares por alguns minutos, aguardando pagamento.")
    public ReservaResponse reservar(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody ReservaRequest pedido) {
        return respostas.doPassageiro(service.reservar(usuario.getId(), pedido));
    }

    @GetMapping
    @Operation(description = "Reservas do passageiro, da mais recente para a mais antiga.")
    public List<ReservaResponse> minhas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.minhas(usuario.getId()).stream().map(respostas::doPassageiro).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse minha(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return respostas.doPassageiro(service.minha(id, usuario.getId()));
    }

    @PostMapping("/{id}/pagamento")
    @Operation(description = "Cria a cobrança no provedor (ou devolve a pendente) e informa a página de pagamento.")
    public PagamentoResponse pagar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return PagamentoResponse.de(service.iniciarPagamento(id, usuario.getId()));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(description = "Cancela a reserva (até o prazo antes da partida); pagamento aprovado é reembolsado.")
    public ReservaResponse cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return respostas.doPassageiro(service.cancelar(id, usuario.getId()));
    }

    @GetMapping("/{id}/localizacao")
    @Operation(description = "Localização da van da reserva confirmada (posição só com a viagem em andamento).")
    public LocalizacaoResponse localizacao(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return localizacao.daReserva(id, usuario.getId());
    }
}
