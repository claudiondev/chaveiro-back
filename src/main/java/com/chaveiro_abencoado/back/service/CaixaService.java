package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.MovimentacaoRequest;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.exception.NotFoundException;
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
public class CaixaService {

    private final FechamentoDiarioRepository fechamentoRepository;
    private final MovimentacaoCaixaRepository movimentacaoRepository;
    private final ServicoRealizadoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;

    public CaixaService(FechamentoDiarioRepository fechamentoRepository,
                        MovimentacaoCaixaRepository movimentacaoRepository,
                        ServicoRealizadoRepository servicoRepository,
                        UsuarioRepository usuarioRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public FechamentoResponse abrirCaixa(AberturaRequest request, String emailUsuario) {
        if (fechamentoRepository.existsByData(LocalDate.now())) {
            throw new BusinessException("Já existe um caixa para hoje");
        }

        Usuario usuario = buscarUsuario(emailUsuario);
        FechamentoDiario fechamento = new FechamentoDiario(LocalDate.now(), request.getValorAbertura(), usuario);
        fechamentoRepository.save(fechamento);

        return FechamentoResponse.fromEntity(fechamento);
    }

    public FechamentoResponse consultarHoje() {
        FechamentoDiario fechamento = fechamentoRepository
                .findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseGet(() -> fechamentoRepository.findTopByDataOrderByIdDesc(LocalDate.now())
                        .orElseThrow(() -> new NotFoundException("Caixa não foi aberto hoje")));

        atualizarTotais(fechamento);
        return FechamentoResponse.fromEntity(fechamento);
    }

    // Histórico: consultar caixa de qualquer data
    public FechamentoResponse consultarPorData(LocalDate data) {
        FechamentoDiario fechamento = fechamentoRepository.findTopByDataOrderByIdDesc(data)
                .orElseThrow(() -> new NotFoundException("Nenhum caixa encontrado para " + data));

        atualizarTotais(fechamento, data);
        return FechamentoResponse.fromEntity(fechamento);
    }

    @Transactional
    public void registrarMovimentacao(MovimentacaoRequest request, String emailUsuario) {
        FechamentoDiario caixaAberto = fechamentoRepository
                .findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseThrow(() -> new BusinessException("Caixa não está aberto"));

        Usuario usuario = buscarUsuario(emailUsuario);

        MovimentacaoCaixa movimentacao = new MovimentacaoCaixa();
        movimentacao.setTipo(request.getTipo());
        movimentacao.setValor(request.getValor());
        movimentacao.setDescricao(request.getDescricao());
        movimentacao.setCategoriaSaida(request.getCategoriaSaida());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setUsuario(usuario);
        movimentacao.setFechamentoDiario(caixaAberto);

        movimentacaoRepository.save(movimentacao);
    }

    @Transactional
    public FechamentoResponse fecharCaixa(String observacao) {
        FechamentoDiario fechamento = fechamentoRepository
                .findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseThrow(() -> new BusinessException("Nenhum caixa aberto para fechar"));

        atualizarTotais(fechamento);
        fechamento.setStatus(StatusFechamento.FECHADO);
        fechamento.setObservacao(observacao);
        fechamentoRepository.save(fechamento);

        return FechamentoResponse.fromEntity(fechamento);
    }

    private void atualizarTotais(FechamentoDiario fechamento) {
        atualizarTotais(fechamento, LocalDate.now());
    }

    private void atualizarTotais(FechamentoDiario fechamento, LocalDate data) {
        List<MovimentacaoCaixa> movimentacoes = movimentacaoRepository
                .findByFechamentoDiarioId(fechamento.getId());

        BigDecimal entradas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA)
                .map(MovimentacaoCaixa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saidas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.SAIDA)
                .map(MovimentacaoCaixa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(LocalTime.MAX);

        List<ServicoRealizado> servicos = servicoRepository.findByDataHoraBetween(inicioDia, fimDia);
        int totalServicos = servicos.size();
        int totalChaves = servicoRepository.contarChavesNoPeriodo(inicioDia, fimDia);

        fechamento.setTotalEntradas(entradas);
        fechamento.setTotalSaidas(saidas);
        fechamento.setSaldoFinal(fechamento.getValorAbertura().add(entradas).subtract(saidas));
        fechamento.setTotalServicos(totalServicos);
        fechamento.setTotalChaves(totalChaves);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
