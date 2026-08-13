package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.TipoServicoDTO;
import com.chaveiro_abencoado.back.dto.TipoServicoRequest;
import com.chaveiro_abencoado.back.model.TipoServico;
import com.chaveiro_abencoado.back.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoServicoService {

    private final TipoServicoRepository tipoServicoRepository;

    public TipoServicoService(TipoServicoRepository tipoServicoRepository) {
        this.tipoServicoRepository = tipoServicoRepository;
    }

    public List<TipoServicoDTO> listarAtivos() {
        return tipoServicoRepository.findByAtivoTrue().stream()
                .map(TipoServicoDTO::fromEntity)
                .toList();
    }

    public TipoServicoDTO criar(TipoServicoRequest request) {
        TipoServico tipo = toEntity(request);
        return TipoServicoDTO.fromEntity(tipoServicoRepository.save(tipo));
    }

    public TipoServicoDTO atualizar(Long id, TipoServicoRequest request) {
        TipoServico tipo = buscarPorId(id);
        tipo.setNome(request.getNome());
        tipo.setDescricao(request.getDescricao());
        tipo.setPreco(request.getPreco());
        tipo.setPrecoExterno(request.getPrecoExterno());
        tipo.setCategoria(request.getCategoria());
        tipo.setEhChave(request.isEhChave());
        return TipoServicoDTO.fromEntity(tipoServicoRepository.save(tipo));
    }

    public void desativar(Long id) {
        TipoServico tipo = buscarPorId(id);
        tipo.setAtivo(false);
        tipoServicoRepository.save(tipo);
    }

    public TipoServico buscarPorId(Long id) {
        return tipoServicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de serviço não encontrado"));
    }

    private TipoServico toEntity(TipoServicoRequest request) {
        TipoServico tipo = new TipoServico(
                request.getNome(),
                request.getPreco(),
                request.getCategoria(),
                request.isEhChave()
        );
        tipo.setDescricao(request.getDescricao());
        tipo.setPrecoExterno(request.getPrecoExterno());
        return tipo;
    }
}
