package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.MovimentacaoRequest;
import com.chaveiro_abencoado.back.dto.ServicoResumoDTO;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.exception.NotFoundException;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        try {
            fechamentoRepository.save(fechamento);
        } catch (DataIntegrityViolationException e) {
            // Duas aberturas concorrentes passaram pelo existsByData() antes de qualquer
            // uma gravar; a constraint UNIQUE(data) do banco pegou a que perdeu a corrida.
            throw new BusinessException("Já existe um caixa para hoje");
        }

        return FechamentoResponse.fromEntity(fechamento);
    }

    // Só leitura: nunca bloqueia linha nem altera a entidade gerenciada (ver montarComprovante)
    @Transactional(readOnly = true)
    public FechamentoResponse consultarHoje() {
        FechamentoDiario fechamento = fechamentoRepository
                .findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseGet(() -> fechamentoRepository.findTopByDataOrderByIdDesc(LocalDate.now())
                        .orElseThrow(() -> new NotFoundException("Caixa não foi aberto hoje")));

        return montarComprovante(fechamento);
    }

    // Histórico: consultar caixa de qualquer data. Só leitura, mesma garantia acima.
    @Transactional(readOnly = true)
    public FechamentoResponse consultarPorData(LocalDate data) {
        FechamentoDiario fechamento = fechamentoRepository.findTopByDataOrderByIdDesc(data)
                .orElseThrow(() -> new NotFoundException("Nenhum caixa encontrado para " + data));

        return montarComprovante(fechamento);
    }

    // Caixa fechado é congelado: os totais gravados no fechamento não são recalculados.
    // Caixa aberto: os totais são calculados só para a resposta — a entidade gerenciada
    // pelo Hibernate nunca é alterada aqui (@Transactional(readOnly = true) nos métodos
    // acima também desliga o auto-flush, então mesmo um dirty-check acidental não geraria
    // UPDATE; ainda assim colocamos os valores num objeto à parte, nunca nos setters da entidade).
    private FechamentoResponse montarComprovante(FechamentoDiario fechamento) {
        List<ServicoRealizado> servicos = servicoRepository.findByFechamentoDiarioId(fechamento.getId());

        FechamentoResponse response = FechamentoResponse.fromEntity(fechamento);
        if (fechamento.getStatus() == StatusFechamento.ABERTO) {
            TotaisCaixa totais = calcularTotais(fechamento, servicos);
            response.setTotalEntradas(totais.entradas());
            response.setTotalSaidas(totais.saidas());
            response.setSaldoFinal(totais.saldoFinal());
            response.setTotalServicos(totais.totalServicos());
            response.setTotalChaves(totais.totalChaves());
        }
        response.setServicos(ServicoResumoDTO.agruparPorTipo(servicos));
        return response;
    }

    @Transactional(readOnly = true)
    public Page<FechamentoResponse> listarHistorico(int pagina, int tamanho) {
        return fechamentoRepository
                .findByStatusOrderByDataDescIdDesc(
                        StatusFechamento.FECHADO,
                        PageRequest.of(pagina, tamanho)
                )
                .map(FechamentoResponse::fromEntity);
    }

    @Transactional
    public void registrarMovimentacao(MovimentacaoRequest request, String emailUsuario) {
        FechamentoDiario caixaAberto = fechamentoRepository
                .findByDataAndStatusParaAtualizar(LocalDate.now(), StatusFechamento.ABERTO)
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
        // Bloqueia a linha antes de calcular: nenhum registro/movimentação concorrente
        // termina de gravar depois que o fechamento já leu os totais (Task 6).
        FechamentoDiario fechamento = fechamentoRepository
                .findByDataAndStatusParaAtualizar(LocalDate.now(), StatusFechamento.ABERTO)
                .orElseThrow(() -> new BusinessException("Nenhum caixa aberto para fechar"));

        TotaisCaixa totais = calcularTotais(fechamento, servicoRepository.findByFechamentoDiarioId(fechamento.getId()));
        fechamento.setTotalEntradas(totais.entradas());
        fechamento.setTotalSaidas(totais.saidas());
        fechamento.setSaldoFinal(totais.saldoFinal());
        fechamento.setTotalServicos(totais.totalServicos());
        fechamento.setTotalChaves(totais.totalChaves());
        fechamento.setStatus(StatusFechamento.FECHADO);
        fechamento.setObservacao(observacao);
        fechamentoRepository.save(fechamento);

        return FechamentoResponse.fromEntity(fechamento);
    }

    // Serviços vêm pelo vínculo com o caixa, não pelo horário de registro. Não mexe na
    // entidade — devolve os números calculados para quem chamar decidir se persiste.
    private TotaisCaixa calcularTotais(FechamentoDiario fechamento, List<ServicoRealizado> servicos) {
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

        int totalChaves = servicos.stream()
                .filter(s -> s.getTipoServico().isEhChave())
                .mapToInt(ServicoRealizado::getQuantidade)
                .sum();

        BigDecimal saldoFinal = fechamento.getValorAbertura().add(entradas).subtract(saidas);
        return new TotaisCaixa(entradas, saidas, saldoFinal, servicos.size(), totalChaves);
    }

    private record TotaisCaixa(BigDecimal entradas, BigDecimal saidas, BigDecimal saldoFinal,
                               int totalServicos, int totalChaves) {
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
