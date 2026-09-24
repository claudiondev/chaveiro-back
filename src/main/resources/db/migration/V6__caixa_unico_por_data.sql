-- "Só pode haver um caixa por dia" já era regra de negócio (verificada em CaixaService antes
-- de salvar), mas sem constraint no banco duas aberturas simultâneas podiam passar pelo
-- existsByData() antes de qualquer uma gravar. Isto torna a regra à prova de concorrência.
--
-- Se esta migration falhar por causa de UNIQUE constraint violation, existem duplicidades
-- reais no banco que precisam de conferência manual (decidir qual fechamento é o válido)
-- antes de rodar de novo — de propósito não fazemos essa escolha automaticamente aqui.
ALTER TABLE fechamentos_diarios ADD CONSTRAINT uk_fechamentos_diarios_data UNIQUE (data);
