package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.FormaPagamento;
import com.chaveiro_abencoado.back.model.ServicoRealizado;
import com.chaveiro_abencoado.back.model.StatusPagamento;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServicoDTO {

    private Long id;
    private String tipoServicoNome;
    private Integer quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
    private FormaPagamento formaPagamento;
    private StatusPagamento statusPagamento;
    private String observacao;
    private boolean domicilio;
    private String endereco;
    private BigDecimal taxaDeslocamento;
    private LocalDateTime dataHora;
    private boolean garantia;
    private String funcionarioNome;

    public static ServicoDTO fromEntity(ServicoRealizado entity) {
        ServicoDTO dto = new ServicoDTO();
        dto.setId(entity.getId());
        dto.setTipoServicoNome(entity.getTipoServico().getNome());
        dto.setQuantidade(entity.getQuantidade());
        dto.setValorUnitario(entity.getValorUnitario());
        dto.setValorTotal(entity.getValorTotal());
        dto.setFormaPagamento(entity.getFormaPagamento());
        dto.setStatusPagamento(entity.getStatusPagamento());
        dto.setObservacao(entity.getObservacao());
        dto.setDomicilio(entity.isDomicilio());
        dto.setEndereco(entity.getEndereco());
        dto.setTaxaDeslocamento(entity.getTaxaDeslocamento());
        dto.setDataHora(entity.getDataHora());
        dto.setGarantia(entity.isGarantia());
        dto.setFuncionarioNome(entity.getUsuario().getNome());
        return dto;
    }
}
