package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.StatusProgressoAjuda;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProgressoAjudaRequest {

    @NotNull
    @Min(1)
    private Integer versao;

    @NotNull
    private StatusProgressoAjuda status;

    public Integer getVersao() {
        return versao;
    }

    public void setVersao(Integer versao) {
        this.versao = versao;
    }

    public StatusProgressoAjuda getStatus() {
        return status;
    }

    public void setStatus(StatusProgressoAjuda status) {
        this.status = status;
    }
}
