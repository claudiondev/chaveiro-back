package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ServicoDTO;
import com.chaveiro_abencoado.back.dto.ServicoRequest;
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
        FechamentoDiario caixaAberto = fechamentoRepository
                .findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseThrow(() -> new RuntimeException("Caixa não está aberto. Abra o caixa antes de registrar serviços"));

        Usuario usuario = buscarUsuario(emailUsuario);
        TipoServico tipoServico = tipoServicoService.buscarPorId(request.getTipoServicoId());

        ServicoRealizado servico = toEntity(request, tipoServico, usuario, caixaAberto);
        servicoRepository.save(servico);

        // Garantia não gera movimentação de entrada
        if (!servico.isGarantia()) {
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

    @Transactional
    public void cancelar(Long id) {
        ServicoRealizado servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        if (!servico.getDataHora().toLocalDate().equals(LocalDate.now())) {
            throw new RuntimeException("Só é possível cancelar serviços do dia atual");
        }

        servicoRepository.delete(servico);
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
        movimentacao.setDescricao("Serviço: " + servico.getTipoServico().getNome());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setUsuario(usuario);
        movimentacao.setFechamentoDiario(fechamento);
        movimentacaoRepository.save(movimentacao);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}
