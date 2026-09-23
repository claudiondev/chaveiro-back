package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ServicoDTO;
import com.chaveiro_abencoado.back.dto.ServicoRequest;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.exception.NotFoundException;
import com.chaveiro_abencoado.back.exception.UnauthorizedException;
import com.chaveiro_abencoado.back.model.*;
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

        if (servico.getFechamentoDiario().getStatus() == StatusFechamento.FECHADO) {
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
                    servico.getFechamentoDiario().getId()
            );
        }

        servicoRepository.delete(servico);
    }

    private FechamentoDiario buscarCaixaAberto() {
        return fechamentoRepository
                .findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO)
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
            servico.setValorTotal(subtotal.add(taxa));
        }

        return servico;
    }

    private void gerarMovimentacaoEntrada(ServicoRealizado servico, Usuario usuario,
                                           FechamentoDiario fechamento) {
        MovimentacaoCaixa movimentacao = new MovimentacaoCaixa();
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setValor(servico.getValorTotal());
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
