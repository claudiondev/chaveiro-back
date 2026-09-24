package com.chaveiro_abencoado.back.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "progressos_ajuda",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_progresso_ajuda_usuario_guia",
                columnNames = {"usuario_id", "guia"}
        )
)
public class ProgressoAjuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuiaAjuda guia;

    @Column(nullable = false)
    private int versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProgressoAjuda status;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public ProgressoAjuda() {}

    public ProgressoAjuda(Usuario usuario, GuiaAjuda guia) {
        this.usuario = usuario;
        this.guia = guia;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public GuiaAjuda getGuia() {
        return guia;
    }

    public int getVersao() {
        return versao;
    }

    public void setVersao(int versao) {
        this.versao = versao;
    }

    public StatusProgressoAjuda getStatus() {
        return status;
    }

    public void setStatus(StatusProgressoAjuda status) {
        this.status = status;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
