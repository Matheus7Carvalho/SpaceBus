package com.spacebus.ops.api.controller.motorista;

import com.spacebus.ops.api.config.security.UsuarioAutenticado;
import com.spacebus.ops.domain.model.localizacao.LoteLocalizacaoRequest;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;
import com.spacebus.ops.domain.model.localizacao.ViagemMotoristaResponse;
import com.spacebus.ops.domain.service.localizacao.ILocalizacaoService;
import com.spacebus.ops.domain.service.localizacao.IViagensMotoristaService;
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

/** Viagens do motorista logado. Restrito ao perfil Motorista em SecurityConfiguration; cada um só age nas suas. */
@RestController
@RequestMapping(value = "/api/motorista/viagem", produces = "application/json")
@Tag(name = "Motorista", description = "Viagens do motorista: iniciar, transmitir a localização e encerrar.")
public class MotoristaViagemController {

    private final IViagensMotoristaService viagens;
    private final ILocalizacaoService localizacao;

    public MotoristaViagemController(IViagensMotoristaService viagens, ILocalizacaoService localizacao) {
        this.viagens = viagens;
        this.localizacao = localizacao;
    }

    @GetMapping
    @Operation(description = "Viagens de hoje atribuídas ao motorista, mais a que estiver em andamento.")
    public List<ViagemMotoristaResponse> deHoje(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return viagens.deHoje(usuario.getId());
    }

    @PostMapping("/{id}/iniciar")
    @Operation(description = "Inicia a viagem (a partir de 30 min antes da partida) e liga a localização.")
    public ViagemMotoristaResponse iniciar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return viagens.iniciar(id, usuario.getId());
    }

    @PostMapping("/{id}/encerrar")
    @Operation(description = "Encerra a viagem e desliga a localização. Definitivo.")
    public ViagemMotoristaResponse encerrar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return viagens.encerrar(id, usuario.getId());
    }

    @PostMapping("/{id}/localizacao")
    @Operation(description = "Lote de 1 a 20 posições do celular. Rejeitadas e ignoradas não fazem o lote falhar.")
    public ResultadoEnvioResponse localizacao(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id,
                                              @Valid @RequestBody LoteLocalizacaoRequest lote) {
        return localizacao.registrar(id, usuario.getId(), lote.getPosicoes());
    }
}
