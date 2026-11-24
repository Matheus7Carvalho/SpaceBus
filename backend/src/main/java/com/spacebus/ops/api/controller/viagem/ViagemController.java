package com.spacebus.ops.api.controller.viagem;

import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.api.controller.reserva.ReservaRespostas;
import com.spacebus.ops.domain.core.controller.BaseController;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoOperacaoResponse;
import com.spacebus.ops.domain.model.reserva.ReservaDaViagemResponse;
import com.spacebus.ops.domain.model.viagem.IViagemMapper;
import com.spacebus.ops.domain.model.viagem.ViagemRequest;
import com.spacebus.ops.domain.model.viagem.ViagemResponse;
import com.spacebus.ops.domain.service.auditoria.IAuditoriaService;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Restrito a Administrador e Operador em SecurityConfiguration. A busca do passageiro tem endpoint próprio. */
@RestController
@RequestMapping(value = "/api/viagem", produces = "application/json")
@Tag(name = "Viagem", description = "Partidas de uma rota: horário, veículo, preço, capacidade e situação.")
public class ViagemController extends BaseController<Viagem, ViagemRequest, ViagemResponse> {

    private final IViagemService service;
    private final IReservaService reservas;
    private final ReservaRespostas respostas;
    private final IAuditoriaService auditoria;
    private final ILocalizacaoService localizacao;

    protected ViagemController(final IViagemMapper mapper, final IViagemService service, final IReservaService reservas,
                               final ReservaRespostas respostas, final IAuditoriaService auditoria,
                               final ILocalizacaoService localizacao) {
        super(mapper, service);
        this.service = service;
        this.reservas = reservas;
        this.respostas = respostas;
        this.auditoria = auditoria;
        this.localizacao = localizacao;
    }

    /** Ordem fixa por partida (o cliente não escolhe o campo) e no máximo 100 por página. */
    public static final int AGENDA_POR_PAGINA = 100;

    @GetMapping("/agenda")
    @Operation(description = "Viagens do operador filtradas no servidor, por horário de partida. Sem dia: de hoje em diante.")
    public Page<ViagemResponse> agenda(@RequestParam(required = false) LocalDate dia,
                                       @RequestParam(required = false) String rotaId,
                                       @RequestParam(required = false) StatusViagem status,
                                       @RequestParam(defaultValue = "0") int page) {
        var pagina = PageRequest.of(Math.max(page, 0), AGENDA_POR_PAGINA, Sort.by("partida", "id"));
        var viagens = service.agenda(dia, rotaId, status, pagina);
        var semSinal = localizacao.semSinal(viagens.getContent());
        return viagens.map(v -> {
            var resposta = getMapper().toResponse(v);
            resposta.setSemSinal(semSinal.contains(v.getId()));
            return resposta;
        });
    }

    @GetMapping("/{id}/reservas")
    @Operation(description = "Reservas e lista de passageiros da viagem, inclusive canceladas (a avisar).")
    public List<ReservaDaViagemResponse> reservas(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        // Lista com nome e e-mail de passageiros: todo acesso fica registrado (escopo, seção 3).
        service.findById(id); // viagem inexistente (ou id inválido) responde 404 e não vira registro de auditoria
        auditoria.registrar(usuario.getId(), usuario.getPerfil(), AcaoAuditada.CONSULTA_PASSAGEIROS_VIAGEM, id);
        return reservas.daViagem(id).stream().map(respostas::daViagem).toList();
    }

    @PostMapping("/{id}/publicar")
    @Operation(description = "Publica a viagem (rascunho ou suspensa).")
    public ViagemResponse publicar(@PathVariable String id) {
        return getMapper().toResponse(service.publicar(id));
    }

    @PostMapping("/{id}/suspender")
    @Operation(description = "Suspende uma viagem publicada.")
    public ViagemResponse suspender(@PathVariable String id) {
        return getMapper().toResponse(service.suspender(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(description = "Cancela a viagem (rascunho, publicada ou suspensa). Definitivo.")
    public ViagemResponse cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return getMapper().toResponse(service.cancelar(id, usuario.getId()));
    }

    @PostMapping("/{id}/iniciar")
    @Operation(description = "Inicia a viagem publicada (a partir de 30 min antes da partida). A posição vem do celular do motorista.")
    public ViagemResponse iniciar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return getMapper().toResponse(service.iniciar(id, usuario.getId(), null));
    }

    @PostMapping("/{id}/encerrar")
    @Operation(description = "Encerra a viagem em andamento. Definitivo.")
    public ViagemResponse encerrar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return getMapper().toResponse(service.encerrar(id, usuario.getId(), null));
    }

    @GetMapping("/{id}/localizacao")
    @Operation(description = "Última posição da viagem e diagnóstico do sinal (sem dados do motorista).")
    public LocalizacaoOperacaoResponse localizacao(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return localizacao.daViagem(id, usuario.getId(), usuario.getPerfil());
    }
}
