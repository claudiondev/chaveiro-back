package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.StatusProgressoAjuda;
import java.time.LocalDateTime;

public record ProgressoAjudaResponse(
        GuiaAjuda guia,
        int versao,
        StatusProgressoAjuda status,
        LocalDateTime atualizadoEm
) {}
