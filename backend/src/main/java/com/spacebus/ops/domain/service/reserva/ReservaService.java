package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.ReservaHistorico;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.MotivoCancelamento;
import com.spacebus.ops.domain.entity.enums.OrigemAcao;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaHistoricoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamento;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamentoException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ReservaService implements IReservaService {

    /** Sem caracteres que se confundem (0/O, 1/I/L). */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final Set<StatusReserva> ATIVAS = Set.of(StatusReserva.AGUARDANDO_PAGAMENTO, StatusReserva.CONFIRMADA);
    private static final String SEM_EXCECAO = "";
    private static final String NAO_ENCONTRADA = "Reserva não encontrada.";

    private final ReservaRepository reservaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ReservaHistoricoRepository historicoRepository;
    private final ViagemRepository viagemRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProvedorPagamento provedor;
    private final Clock clock;
    private final int bloqueioMinutos;
    private final int cancelamentoHoras;
    private final SecureRandom aleatorio = new SecureRandom();

    public ReservaService(ReservaRepository reservaRepository, PagamentoRepository pagamentoRepository,
                          ReservaHistoricoRepository historicoRepository, ViagemRepository viagemRepository,
                          UsuarioRepository usuarioRepository, ProvedorPagamento provedor, Clock clock,
                          @Value("${spacebus.reserva.bloqueio-minutos:10}") int bloqueioMinutos,
                          @Value("${spacebus.reserva.cancelamento-horas:2}") int cancelamentoHoras) {
        this.reservaRepository = reservaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.historicoRepository = historicoRepository;
        this.viagemRepository = viagemRepository;
        this.usuarioRepository = usuarioRepository;
        this.provedor = provedor;
        this.clock = clock;
        this.bloqueioMinutos = bloqueioMinutos;
        this.cancelamentoHoras = cancelamentoHoras;
    }

    @Override
    @Transactional
    public Reserva reservar(String passageiroId, ReservaRequest pedido) {
        var agora = agora();
        var viagem = viagemRepository.travar(pedido.getViagemId())
                .orElseThrow(() -> new DomainException("Esta viagem não está mais disponível."));
        if (viagem.getStatus() != StatusViagem.PUBLICADA || !viagem.getPartida().isAfter(agora)) {
            throw new DomainException("Esta viagem não está mais disponível.");
        }
        var quantidade = pedido.getQuantidade();
        if (quantidade < 1 || quantidade > ReservaRequest.MAX_LUGARES) {
            throw new DomainException("Reserve de 1 a %d lugares.", ReservaRequest.MAX_LUGARES);
        }
        var embarque = ponto(viagem, pedido.getEmbarqueOrdem());
        var desembarque = ponto(viagem, pedido.getDesembarqueOrdem());
        if (embarque.getOrdem() >= desembarque.getOrdem()) {
            throw new DomainException("O embarque deve vir antes do desembarque no percurso.");
        }
        var disponiveis = viagem.getCapacidade() - reservaRepository.lugaresOcupados(viagem.getId(), agora, SEM_EXCECAO);
        if (quantidade > disponiveis) {
            throw new DomainException(disponiveis <= 0
                    ? "Não há lugares disponíveis nesta viagem."
                    : "Não há lugares suficientes. Restam %d.".formatted(disponiveis));
        }
        var passageiro = usuarioRepository.findById(passageiroId)
                .filter(u -> u.getPerfil() == Perfil.PASSAGEIRO && Boolean.TRUE.equals(u.getAtivo()))
                .orElseThrow(() -> new DomainException("Só passageiros podem reservar."));

        var reserva = new Reserva();
        reserva.setCodigo(novoCodigo());
        reserva.setViagem(viagem);
        reserva.setPassageiro(passageiro);
        reserva.setEmbarqueOrdem(embarque.getOrdem());
        reserva.setEmbarqueNome(embarque.getNome());
        reserva.setEmbarqueReferencia(embarque.getReferencia());
        reserva.setDesembarqueOrdem(desembarque.getOrdem());
        reserva.setDesembarqueNome(desembarque.getNome());
        reserva.setQuantidade(quantidade);
        reserva.setValorTotal(viagem.getPreco().multiply(BigDecimal.valueOf(quantidade)));
        reserva.setStatus(StatusReserva.AGUARDANDO_PAGAMENTO);
        reserva.setExpiraEm(agora.plusMinutes(bloqueioMinutos));
        var salva = reservaRepository.save(reserva);
        registrar(salva, AcaoReserva.RESERVADA, OrigemAcao.PASSAGEIRO, passageiroId, quantidade + " lugar(es)");
        return salva;
    }

    @Override
    public List<Reserva> minhas(String passageiroId) {
        return reservaRepository.findByPassageiroIdOrderByDataCriacaoDesc(passageiroId);
    }

    @Override
    public Reserva minha(String id, String passageiroId) {
        return reservaRepository.findByIdAndPassageiroId(id, passageiroId)
                .orElseThrow(() -> new EntityNotFoundException(NAO_ENCONTRADA));
    }

    /** Travada: duas chamadas ao mesmo tempo não criam duas cobranças no provedor. */
    @Override
    @Transactional
    public Pagamento iniciarPagamento(String reservaId, String passageiroId) {
        var reserva = travarDoPassageiro(reservaId, passageiroId);
        if (!pagavel(reserva)) {
            throw new DomainException(reserva.getStatus() == StatusReserva.AGUARDANDO_PAGAMENTO
                    ? "O prazo para pagar esta reserva acabou. Faça uma nova reserva."
                    : "Esta reserva não está aguardando pagamento.");
        }
        var pendente = pagamentoRepository.findFirstByReservaIdAndStatus(reservaId, StatusPagamento.PENDENTE);
        if (pendente.isPresent()) return pendente.get();

        ProvedorPagamento.Cobranca cobranca;
        try {
            cobranca = provedor.criarCobranca(reserva.getCodigo(), reserva.getValorTotal());
        } catch (ProvedorPagamentoException e) {
            log.warn("Falha ao criar cobrança da reserva {}.", reserva.getCodigo(), e);
            throw new DomainException("Não foi possível iniciar o pagamento agora. Tente de novo em instantes.");
        }
        var pagamento = pagamentoRepository.save(new Pagamento(reserva, cobranca.provedorId(), provedor.metodo(),
                reserva.getValorTotal(), StatusPagamento.PENDENTE, cobranca.urlPagamento()));
        registrar(reserva, AcaoReserva.PAGAMENTO_INICIADO, OrigemAcao.PASSAGEIRO, passageiroId, cobranca.provedorId());
        return pagamento;
    }

    /**
     * Trava viagem → reserva → pagamento antes de ler qualquer dado, para decidir com a situação atual
     * (capacidade, viagem cancelada, reserva cancelada) e nunca com o que foi lido antes da trava.
     */
    @Override
    @Transactional
    public void processarRetorno(String provedorId, RetornoPagamentoRequest.Resultado resultado) {
        var ids = pagamentoRepository.viagemEReservaDaCobranca(provedorId);
        if (ids.isEmpty()) throw new EntityNotFoundException("Cobrança não encontrada.");
        var viagem = viagemRepository.travar((String) ids.get(0)[0]).orElseThrow();
        var reserva = reservaRepository.travar((String) ids.get(0)[1]).orElseThrow();
        var pagamento = pagamentoRepository.travarPorProvedorId(provedorId).orElseThrow();
        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            log.info("Retorno repetido da cobrança {} ignorado (já {}).", provedorId, pagamento.getStatus());
            return;
        }
        if (resultado == RetornoPagamentoRequest.Resultado.RECUSADO) {
            pagamento.setStatus(StatusPagamento.RECUSADO);
            registrar(reserva, AcaoReserva.PAGAMENTO_RECUSADO, OrigemAcao.PROVEDOR, null, provedorId);
            return; // a reserva continua aguardando: o passageiro pode tentar de novo dentro do prazo
        }

        pagamento.setStatus(StatusPagamento.APROVADO);
        registrar(reserva, AcaoReserva.PAGAMENTO_APROVADO, OrigemAcao.PROVEDOR, null, provedorId);
        var agora = agora();
        // Viagem que já partiu nunca confirma: o passageiro não seria cobrado por uma van que já saiu.
        var aindaVaiPartir = viagem.getPartida().isAfter(agora);
        var noPrazo = reserva.getStatus() == StatusReserva.AGUARDANDO_PAGAMENTO && reserva.getExpiraEm().isAfter(agora);
        // Fora do prazo: confirma só se a reserva não foi cancelada, a viagem segue publicada e ainda há lugar.
        var cabeForaDoPrazo = !noPrazo
                && reserva.getStatus() != StatusReserva.CANCELADA
                && viagem.getStatus() == StatusViagem.PUBLICADA
                && reserva.getQuantidade() <= viagem.getCapacidade() - reservaRepository.lugaresOcupados(viagem.getId(), agora, reserva.getId());
        if (aindaVaiPartir && ((noPrazo && viagem.getStatus() != StatusViagem.CANCELADA) || cabeForaDoPrazo)) {
            reserva.setStatus(StatusReserva.CONFIRMADA);
            reserva.setConfirmadaEm(agora);
            registrar(reserva, AcaoReserva.CONFIRMADA, OrigemAcao.PROVEDOR, null, null);
            return;
        }
        if (reserva.getStatus() == StatusReserva.AGUARDANDO_PAGAMENTO) {
            reserva.setStatus(StatusReserva.EXPIRADA);
            registrar(reserva, AcaoReserva.EXPIRADA, OrigemAcao.PROVEDOR, null, "Pagamento aprovado depois do prazo, sem lugar.");
        }
        reembolsar(pagamento, OrigemAcao.PROVEDOR, null);
    }

    @Override
    @Transactional
    public Reserva cancelar(String reservaId, String passageiroId) {
        var reserva = travarDoPassageiro(reservaId, passageiroId);
        if (!ATIVAS.contains(reserva.getStatus())) throw new DomainException("Esta reserva não pode mais ser cancelada.");
        if (!reserva.ocupaLugares(agora())) throw new DomainException("O prazo para pagar esta reserva acabou; ela já não segura lugares.");
        if (!cancelavel(reserva)) {
            throw new DomainException("O cancelamento é permitido até %d horas antes da partida.", cancelamentoHoras);
        }
        encerrar(reserva, MotivoCancelamento.PASSAGEIRO, OrigemAcao.PASSAGEIRO, passageiroId);
        return reserva;
    }

    @Override
    @Transactional
    public void cancelarPorViagem(String viagemId, String responsavelId) {
        // Relê cada reserva travada: uma expiração em andamento (SKIP LOCKED) não é sobrescrita.
        reservaRepository.findByViagemIdAndStatusIn(viagemId, ATIVAS).stream()
                .map(r -> reservaRepository.travar(r.getId()).orElseThrow())
                .filter(r -> ATIVAS.contains(r.getStatus()))
                .forEach(r -> encerrar(r, MotivoCancelamento.VIAGEM_CANCELADA, OrigemAcao.OPERADOR, responsavelId));
    }

    @Override
    public List<Reserva> daViagem(String viagemId) {
        return reservaRepository.findByViagemIdOrderByDataCriacao(viagemId);
    }

    @Override
    public List<ReservaHistorico> historico(String reservaId) {
        return historicoRepository.findByReservaIdOrderByOrdemAsc(reservaId);
    }

    @Override
    public int lugaresDisponiveis(Viagem viagem) {
        return Math.max(0, viagem.getCapacidade() - lugaresOcupados(viagem.getId()));
    }

    @Override
    public int lugaresOcupados(String viagemId) {
        return reservaRepository.lugaresOcupados(viagemId, agora(), SEM_EXCECAO);
    }

    @Override
    public Optional<Pagamento> ultimoPagamento(String reservaId) {
        return pagamentoRepository.findFirstByReservaIdOrderByDataCriacaoDesc(reservaId);
    }

    /** "Até 2 horas antes": exatamente 2 horas antes ainda pode. */
    @Override
    public boolean cancelavel(Reserva reserva) {
        return ATIVAS.contains(reserva.getStatus()) && reserva.ocupaLugares(agora())
                && !agora().plusHours(cancelamentoHoras).isAfter(reserva.getViagem().getPartida());
    }

    @Override
    public boolean pagavel(Reserva reserva) {
        return reserva.getStatus() == StatusReserva.AGUARDANDO_PAGAMENTO && reserva.getExpiraEm().isAfter(agora());
    }

    @Override
    @Transactional
    public int expirarVencidas() {
        var vencidas = reservaRepository.travarVencidas(agora());
        vencidas.forEach(r -> {
            r.setStatus(StatusReserva.EXPIRADA);
            registrar(r, AcaoReserva.EXPIRADA, OrigemAcao.SISTEMA, null, "Prazo de pagamento vencido.");
        });
        return vencidas.size();
    }

    @Override
    @Transactional
    public int tentarReembolsosPendentes() {
        var concluidos = 0;
        for (var pagamento : pagamentoRepository.travarReembolsosPendentes()) {
            if (reembolsar(pagamento, OrigemAcao.SISTEMA, null)) concluidos++;
        }
        return concluidos;
    }

    /** Cancela a reserva e reembolsa o que já foi aprovado. Uma cobrança pendente que aprovar depois também é reembolsada. */
    private void encerrar(Reserva reserva, MotivoCancelamento motivo, OrigemAcao origem, String responsavelId) {
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setMotivoCancelamento(motivo);
        reserva.setCanceladaEm(agora());
        registrar(reserva, AcaoReserva.CANCELADA, origem, responsavelId,
                motivo == MotivoCancelamento.VIAGEM_CANCELADA ? "Viagem cancelada pela transportadora." : null);
        pagamentoRepository.findByReservaIdAndStatus(reserva.getId(), StatusPagamento.APROVADO)
                .forEach(p -> reembolsar(p, origem, responsavelId));
    }

    /**
     * Falha no provedor não desfaz o cancelamento: o pagamento fica REEMBOLSO_PENDENTE e a tarefa agendada
     * tenta de novo. O reembolso é idempotente no provedor (mesma cobrança), então repetir não devolve em dobro.
     */
    private boolean reembolsar(Pagamento pagamento, OrigemAcao origem, String responsavelId) {
        var reserva = pagamento.getReserva();
        try {
            provedor.reembolsar(pagamento.getProvedorId());
            pagamento.setStatus(StatusPagamento.REEMBOLSADO);
            registrar(reserva, AcaoReserva.REEMBOLSADA, origem, responsavelId, pagamento.getProvedorId());
            return true;
        } catch (ProvedorPagamentoException e) {
            log.error("Reembolso da cobrança {} falhou; fica pendente para nova tentativa.", pagamento.getProvedorId(), e);
            if (pagamento.getStatus() != StatusPagamento.REEMBOLSO_PENDENTE) {
                pagamento.setStatus(StatusPagamento.REEMBOLSO_PENDENTE);
                registrar(reserva, AcaoReserva.REEMBOLSO_PENDENTE, origem, responsavelId, pagamento.getProvedorId());
            }
            return false;
        }
    }

    /** Trava a viagem da reserva (descoberta sem carregar a reserva) e depois a própria reserva. */
    private Reserva travarDoPassageiro(String reservaId, String passageiroId) {
        var viagemId = reservaRepository.viagemDaReservaDoPassageiro(reservaId, passageiroId)
                .orElseThrow(() -> new EntityNotFoundException(NAO_ENCONTRADA));
        viagemRepository.travar(viagemId);
        return reservaRepository.travar(reservaId).orElseThrow(() -> new EntityNotFoundException(NAO_ENCONTRADA));
    }

    private void registrar(Reserva reserva, AcaoReserva acao, OrigemAcao origem, String responsavelId, String detalhe) {
        var responsavel = responsavelId == null ? null : usuarioRepository.getReferenceById(responsavelId);
        historicoRepository.save(new ReservaHistorico(reserva, acao, origem, responsavel, detalhe));
    }

    private PontoParada ponto(Viagem viagem, Integer ordem) {
        return viagem.getRota().getPontos().stream()
                .filter(p -> p.getOrdem().equals(ordem))
                .findFirst()
                .orElseThrow(() -> new DomainException("Ponto de parada não encontrado nesta rota."));
    }

    private String novoCodigo() {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            var sb = new StringBuilder("SB-");
            for (int i = 0; i < TAMANHO_CODIGO; i++) sb.append(ALFABETO_CODIGO.charAt(aleatorio.nextInt(ALFABETO_CODIGO.length())));
            if (!reservaRepository.existsByCodigo(sb.toString())) return sb.toString();
        }
        throw new IllegalStateException("Não foi possível gerar um código de reserva único.");
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
