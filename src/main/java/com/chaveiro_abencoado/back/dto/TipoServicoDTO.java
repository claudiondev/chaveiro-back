package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.CategoriaServico;
import com.chaveiro_abencoado.back.model.TipoServico;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipoServicoDTO {

    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private BigDecimal precoExterno;
    private CategoriaServico categoria;
    private boolean ativo;
    private boolean ehChave;

    public static TipoServicoDTO fromEntity(TipoServico entity) {
        TipoServicoDTO dto = new TipoServicoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setPreco(entity.getPreco());
        dto.setPrecoExterno(entity.getPrecoExterno());
        dto.setCategoria(entity.getCategoria());
        dto.setAtivo(entity.isAtivo());
        dto.setEhChave(entity.isEhChave());
        return dto;
    }
}
