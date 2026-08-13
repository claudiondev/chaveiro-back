package com.chaveiro_abencoado.back.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tipos_servico")
public class TipoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoExterno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaServico categoria;

    @Column(nullable = false)
    private boolean ativo = true;

    // Indica se o serviço conta como chave no contador
    @Column(nullable = false)
    private boolean ehChave = false;

    @OneToMany(mappedBy = "tipoServico")
    private List<ServicoRealizado> servicos = new ArrayList<>();

    public TipoServico() {}

    public TipoServico(String nome, BigDecimal preco, CategoriaServico categoria, boolean ehChave) {
        this.nome = nome;
        this.preco = preco;
        this.categoria = categoria;
        this.ehChave = ehChave;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public BigDecimal getPrecoExterno() {
        return precoExterno;
    }

    public void setPrecoExterno(BigDecimal precoExterno) {
        this.precoExterno = precoExterno;
    }

    public CategoriaServico getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaServico categoria) {
        this.categoria = categoria;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public boolean isEhChave() {
        return ehChave;
    }

    public void setEhChave(boolean ehChave) {
        this.ehChave = ehChave;
    }

    public List<ServicoRealizado> getServicos() {
        return servicos;
    }
}
