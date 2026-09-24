package com.chaveiro_abencoado.back.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fechamentos_diarios")
public class FechamentoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate data;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAbertura;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalEntradas = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalSaidas = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal saldoFinal = BigDecimal.ZERO;

    private Integer totalServicos = 0;

    private Integer totalChaves = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusFechamento status = StatusFechamento.ABERTO;

    @Column(length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "fechamentoDiario")
    private List<ServicoRealizado> servicos = new ArrayList<>();

    @OneToMany(mappedBy = "fechamentoDiario")
    private List<MovimentacaoCaixa> movimentacoes = new ArrayList<>();

    public FechamentoDiario() {}

    public FechamentoDiario(LocalDate data, BigDecimal valorAbertura, Usuario usuario) {
        this.data = data;
        this.valorAbertura = valorAbertura;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    public void setValorAbertura(BigDecimal valorAbertura) {
        this.valorAbertura = valorAbertura;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(BigDecimal totalEntradas) {
        this.totalEntradas = totalEntradas;
    }

    public BigDecimal getTotalSaidas() {
        return totalSaidas;
    }

    public void setTotalSaidas(BigDecimal totalSaidas) {
        this.totalSaidas = totalSaidas;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public Integer getTotalServicos() {
        return totalServicos;
    }

    public void setTotalServicos(Integer totalServicos) {
        this.totalServicos = totalServicos;
    }

    public Integer getTotalChaves() {
        return totalChaves;
    }

    public void setTotalChaves(Integer totalChaves) {
        this.totalChaves = totalChaves;
    }

    public StatusFechamento getStatus() {
        return status;
    }

    public void setStatus(StatusFechamento status) {
        this.status = status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<ServicoRealizado> getServicos() {
        return servicos;
    }

    public List<MovimentacaoCaixa> getMovimentacoes() {
        return movimentacoes;
    }
}
