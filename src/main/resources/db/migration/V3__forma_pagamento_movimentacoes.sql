-- Forma de pagamento nas entradas, para o relatório fechar com o faturamento
-- (inclui fiado pago depois, que entra no caixa do dia do pagamento)
ALTER TABLE movimentacoes_caixa ADD COLUMN forma_pagamento VARCHAR(20);

-- Preenche as entradas de serviço já existentes pela descrição "Serviço #<id>: ..."
UPDATE movimentacoes_caixa m
SET forma_pagamento = s.forma_pagamento
FROM servicos_realizados s
WHERE m.tipo = 'ENTRADA'
  AND m.forma_pagamento IS NULL
  AND m.descricao LIKE 'Serviço #' || s.id || ':%';
