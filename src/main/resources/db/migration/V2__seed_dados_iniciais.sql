-- Usuário DONO padrão (senha: Admin123)
-- BCrypt hash gerado para "Admin123"
INSERT INTO usuarios (nome, email, senha, role, ativo)
VALUES ('Dono', 'dono@chaveiro.com',
        '$2a$10$8KxWzYx7GqNvGxQv8yZpVOJGZhY0xK6oLFJhwxGqK5.T8rMdG3K2e',
        'DONO', true);

-- Tipos de serviço comuns com preços exemplo
INSERT INTO tipos_servico (nome, descricao, preco, categoria, ativo, eh_chave) VALUES
('Chave simples', 'Cópia de chave residencial comum', 15.00, 'CHAVE', true, true),
('Chave codificada', 'Chave com codificação especial', 80.00, 'CHAVE', true, true),
('Chave tetra', 'Cópia de chave tetra (4 lados)', 25.00, 'CHAVE', true, true),
('Chave yale', 'Cópia de chave yale', 20.00, 'CHAVE', true, true),
('Chave gorje', 'Cópia de chave gorje (porta de entrada)', 35.00, 'CHAVE', true, true),
('Cópia controle', 'Cópia de controle de portão/alarme', 60.00, 'CONTROLE', true, false),
('Conserto fechadura', 'Reparo em fechadura com defeito', 80.00, 'FECHADURA', true, false),
('Troca de fechadura', 'Substituição completa da fechadura', 120.00, 'FECHADURA', true, false),
('Abertura de porta', 'Abertura de porta trancada', 100.00, 'FECHADURA', true, false),
('Carimbo automático', 'Confecção de carimbo automático', 45.00, 'CARIMBO', true, false),
('Carimbo de bolso', 'Confecção de carimbo de bolso', 30.00, 'CARIMBO', true, false);
