package com.chaveiro_abencoado.back.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class  Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentualComissao;

    @OneToMany(mappedBy = "usuario")
    private List<ServicoRealizado> servicos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario")
    private List<MovimentacaoCaixa> movimentacoes = new ArrayList<>();

    @OneToMany(mappedBy = "usuario")
    private List<FechamentoDiario> fechamentos = new ArrayList<>();

    public Usuario() {}

    public Usuario(String nome, String email, String senha, UserRole role) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.role = role;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public BigDecimal getPercentualComissao() {
        return percentualComissao;
    }

    public void setPercentualComissao(BigDecimal percentualComissao) {
        this.percentualComissao = percentualComissao;
    }

    public List<ServicoRealizado> getServicos() {
        return servicos;
    }

    public List<MovimentacaoCaixa> getMovimentacoes() {
        return movimentacoes;
    }

    public List<FechamentoDiario> getFechamentos() {
        return fechamentos;
    }
}
