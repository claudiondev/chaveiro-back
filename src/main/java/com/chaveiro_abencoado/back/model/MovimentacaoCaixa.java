package com.chaveiro_abencoado.back.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_caixa")
public class MovimentacaoCaixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(length = 255)
    private String descricao;

    // Categoria só se aplica quando tipo = SAIDA
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoriaSaida categoriaSaida;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fechamento_diario_id")
    private FechamentoDiario fechamentoDiario;

    public MovimentacaoCaixa() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public CategoriaSaida getCategoriaSaida() {
        return categoriaSaida;
    }

    public void setCategoriaSaida(CategoriaSaida categoriaSaida) {
        this.categoriaSaida = categoriaSaida;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public FechamentoDiario getFechamentoDiario() {
        return fechamentoDiario;
    }

    public void setFechamentoDiario(FechamentoDiario fechamentoDiario) {
        this.fechamentoDiario = fechamentoDiario;
    }
}
