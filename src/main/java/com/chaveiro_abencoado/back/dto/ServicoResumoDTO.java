package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.ServicoRealizado;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ServicoResumoDTO(
        String nome,
        int quantidade,
        BigDecimal valorTotal
) {
    // Agrupa por nome do tipo, ordenado do maior valor pro menor
    public static List<ServicoResumoDTO> agruparPorTipo(List<ServicoRealizado> servicos) {
        Map<String, ServicoResumoDTO> porNome = new LinkedHashMap<>();
        for (ServicoRealizado s : servicos) {
            porNome.merge(
                    s.getTipoServico().getNome(),
                    new ServicoResumoDTO(s.getTipoServico().getNome(), s.getQuantidade(), s.getValorTotal()),
                    (a, b) -> new ServicoResumoDTO(a.nome(), a.quantidade() + b.quantidade(),
                            a.valorTotal().add(b.valorTotal()))
            );
        }
        return porNome.values().stream()
                .sorted(Comparator.comparing(ServicoResumoDTO::valorTotal).reversed()
                        .thenComparing(ServicoResumoDTO::nome))
                .toList();
    }
}
