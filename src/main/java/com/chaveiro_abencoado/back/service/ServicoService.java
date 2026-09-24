package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ServicoDTO;
import com.chaveiro_abencoado.back.dto.ServicoRequest;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.exception.NotFoundException;
import com.chaveiro_abencoado.back.exception.UnauthorizedException;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.validation.LimitesFinanceiros;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRealizadoRepository servicoRepository;
    private final TipoServicoService tipoServicoService;
    private final UsuarioRepository usuarioRepository;
    private final FechamentoDiarioRepository fechamentoRepository;
    private final MovimentacaoCaixaRepository movimentacaoRepository;

    public ServicoService(ServicoRealizadoRepository servicoRepository,
                          TipoServicoService tipoServicoService,
                          UsuarioRepository usuarioRepository,
                          FechamentoDiarioRepository fechamentoRepository,
                          MovimentacaoCaixaRepository movimentacaoRepository) {
        this.servicoRepository = servicoRepository;
        this.tipoServicoService = tipoServicoService;
        this.usuarioRepository = usuarioRepository;
        this.fechamentoRepository = fechamentoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public ServicoDTO registrar(ServicoRequest request, String emailUsuario) {
        validarRequest(request);
        FechamentoDiario caixaAberto = buscarCaixaAberto();
        Usuario usuario = buscarUsuario(emailUsuario);
        TipoServico tipoServico = tipoServicoService.buscarPorId(request.getTipoServicoId());

        ServicoRealizado servico = toEntity(request, tipoServico, usuario, caixaAberto);
        servicoRepository.save(servico);

        // Garantia não gera movimentação de entrada
        if (!servico.isGarantia() && servico.getStatusPagamento() == StatusPagamento.PAGO) {
            gerarMovimentacaoEntrada(servico, usuario, caixaAberto);
        }

        return ServicoDTO.fromEntity(servico);
    }

    public List<ServicoDTO> listarPorData(LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(LocalTime.MAX);

        return servicoRepository.findByDataHoraBetween(inicio, fim).stream()
                .map(ServicoDTO::fromEntity)
                .toList();
    }

    // Contas a receber: serviços com pagamento pendente
    public List<ServicoDTO> listarPendentes() {
        return servicoRepository.findByStatusPagamento(StatusPagamento.PENDENTE).stream()
                .map(ServicoDTO::fromEntity)
                .toList();
    }

    // Marcar serviço pendente como pago
    @Transactional
    public ServicoDTO marcarComoPago(Long id, String emailUsuario) {
        ServicoRealizado servico = servicoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        if (servico.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new BusinessException("Serviço já está pago");
        }

        servico.setStatusPagamento(StatusPagamento.PAGO);
        servicoRepository.save(servico);

        // Gera movimentação de entrada agora que foi pago
        Usuario usuario = buscarUsuario(emailUsuario);
        FechamentoDiario caixaAberto = buscarCaixaAberto();
        gerarMovimentacaoEntrada(servico, usuario, caixaAberto);

        return ServicoDTO.fromEntity(servico);
    }

    @Transactional
    public void cancelar(Long id, String emailUsuario) {
        ServicoRealizado servico = servicoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        if (!servico.getDataHora().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessException("Só é possível cancelar serviços do dia atual");
        }

        // Bloqueia a linha do caixa e reconfere o status nela, não na associação já
        // carregada em memória — um fechamento concorrente pode ter fechado o caixa
        // entre o momento em que este serviço foi lido e este ponto.
        FechamentoDiario caixa = fechamentoRepository
                .findByIdParaAtualizar(servico.getFechamentoDiario().getId())
                .orElseThrow(() -> new NotFoundException("Caixa do serviço não encontrado"));

        if (caixa.getStatus() == StatusFechamento.FECHADO) {
            throw new BusinessException("Caixa já fechado; não é possível cancelar o serviço");
        }

        // Verificar se é o dono do serviço ou DONO do sistema
        Usuario usuario = buscarUsuario(emailUsuario);
        if (!servico.getUsuario().getId().equals(usuario.getId())
                && usuario.getRole() != UserRole.DONO) {
            throw new UnauthorizedException("Sem permissão para cancelar este serviço");
        }

        // Remover movimentação de entrada associada (usa ID do serviço para precisão)
        if (!servico.isGarantia()) {
            movimentacaoRepository.deleteByDescricaoStartingWithAndFechamentoDiarioId(
                    "Serviço #" + servico.getId() + ":",
                    caixa.getId()
            );
        }

        servicoRepository.delete(servico);
    }

    // Regras que dependem de mais de um campo — Bean Validation cobre só campos isolados
    private void validarRequest(ServicoRequest request) {
        if (request.isDomicilio() && (request.getEndereco() == null || request.getEndereco().isBlank())) {
            throw new BusinessException("Endereço é obrigatório em atendimento a domicílio");
        }

        boolean temTaxa = request.getTaxaDeslocamento() != null
                && request.getTaxaDeslocamento().compareTo(BigDecimal.ZERO) != 0;
        if (!request.isDomicilio() && temTaxa) {
            throw new BusinessException("Taxa de deslocamento só se aplica a atendimento a domicílio");
        }

        if (request.isGarantia() && request.getStatusPagamento() == StatusPagamento.PENDENTE) {
            throw new BusinessException("Garantia não gera cobrança; não pode ficar com pagamento pendente");
        }
    }

    // Bloqueia a linha do caixa: registrar serviço/pagamento e fechar o caixa nunca
    // terminam entrelaçados na mesma data (Task 6). No máximo uma linha de fechamento
    // é bloqueada por transação, então não há ordem a coordenar entre chamadas diferentes.
    private FechamentoDiario buscarCaixaAberto() {
        return fechamentoRepository
                .findByDataAndStatusParaAtualizar(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseThrow(() -> new BusinessException("Caixa não está aberto. Abra o caixa antes de registrar serviços"));
    }

    private ServicoRealizado toEntity(ServicoRequest request, TipoServico tipoServico,
                                       Usuario usuario, FechamentoDiario fechamento) {
        ServicoRealizado servico = new ServicoRealizado();
        servico.setTipoServico(tipoServico);
        servico.setUsuario(usuario);
        servico.setFechamentoDiario(fechamento);
        servico.setQuantidade(request.getQuantidade());
        servico.setFormaPagamento(request.getFormaPagamento());
        servico.setStatusPagamento(request.getStatusPagamento());
        servico.setObservacao(request.getObservacao());
        servico.setDomicilio(request.isDomicilio());
        servico.setEndereco(request.getEndereco());
        servico.setTaxaDeslocamento(request.getTaxaDeslocamento());
        servico.setDataHora(LocalDateTime.now());
        servico.setGarantia(request.isGarantia());
        servico.setMetadados(request.getMetadados());

        // Preço: externo se domicílio e preço externo configurado
        BigDecimal precoUnitario = (request.isDomicilio() && tipoServico.getPrecoExterno() != null)
                ? tipoServico.getPrecoExterno()
                : tipoServico.getPreco();
        servico.setValorUnitario(precoUnitario);

        // Garantia: valor zero
        if (request.isGarantia()) {
            servico.setValorTotal(BigDecimal.ZERO);
        } else {
            BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(request.getQuantidade()));
            BigDecimal taxa = request.getTaxaDeslocamento() != null ? request.getTaxaDeslocamento() : BigDecimal.ZERO;
            BigDecimal total = subtotal.add(taxa);

            // Defesa final: preço do tipo de serviço não passa pelos mesmos limites do request
            if (total.compareTo(LimitesFinanceiros.VALOR_MAXIMO) > 0) {
                throw new BusinessException("Valor total do serviço excede o limite permitido");
            }
            servico.setValorTotal(total);
        }

        return servico;
    }

    private void gerarMovimentacaoEntrada(ServicoRealizado servico, Usuario usuario,
                                           FechamentoDiario fechamento) {
        MovimentacaoCaixa movimentacao = new MovimentacaoCaixa();
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setValor(servico.getValorTotal());
        movimentacao.setFormaPagamento(servico.getFormaPagamento());
        movimentacao.setDescricao(descricaoMovimentacao(servico));
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setUsuario(usuario);
        movimentacao.setFechamentoDiario(fechamento);
        movimentacaoRepository.save(movimentacao);
    }

    private String descricaoMovimentacao(ServicoRealizado servico) {
        return "Serviço #" + servico.getId() + ": " + servico.getTipoServico().getNome();
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
