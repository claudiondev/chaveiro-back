-- Até aqui, a única forma de ligar uma ENTRADA ao serviço que a gerou era o texto da
-- descrição ("Serviço #<id>: <nome>"), usado tanto para cancelar quanto (potencialmente)
-- para auditoria. Isso é frágil: nada impede duas ENTRADAs para o mesmo serviço, e o
-- cancelamento por LIKE pode combinar com descrições parecidas.
ALTER TABLE movimentacoes_caixa
    ADD COLUMN servico_realizado_id BIGINT REFERENCES servicos_realizados(id) ON DELETE SET NULL;

-- Preenche o vínculo só quando inequívoco: exatamente uma ENTRADA cuja descrição bate com
-- "Serviço #<id>:" para aquele serviço. Se algum dia existir mais de uma (o que só
-- aconteceria por causa do próprio bug que esta migration corrige), nenhuma das duas é
-- vinculada — ficam como estavam, para conferência manual; a query abaixo lista esses casos.
WITH candidatos AS (
    SELECT m.id AS movimentacao_id, s.id AS servico_id,
           COUNT(*) OVER (PARTITION BY s.id) AS ocorrencias
    FROM movimentacoes_caixa m
    JOIN servicos_realizados s ON m.descricao LIKE 'Serviço #' || s.id || ':%'
    WHERE m.tipo = 'ENTRADA' AND m.servico_realizado_id IS NULL
)
UPDATE movimentacoes_caixa m
SET servico_realizado_id = c.servico_id
FROM candidatos c
WHERE m.id = c.movimentacao_id AND c.ocorrencias = 1;

-- Garante no banco que um serviço nunca tem mais de uma ENTRADA vinculada (índice único
-- parcial: avulsas, que ficam com servico_realizado_id nulo, não entram nessa restrição).
CREATE UNIQUE INDEX uk_movimentacoes_servico_realizado
    ON movimentacoes_caixa (servico_realizado_id)
    WHERE servico_realizado_id IS NOT NULL;

CREATE INDEX idx_movimentacoes_servico_realizado ON movimentacoes_caixa(servico_realizado_id);
