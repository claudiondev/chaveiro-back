-- Usuário DONO padrão (senha: Admin123)
-- BCrypt hash gerado para "Admin123"
INSERT INTO usuarios (nome, email, senha, role, ativo)
VALUES ('Dono', 'dono@chaveiro.com',
        '$2a$10$8KxWzYx7GqNvGxQv8yZpVOJGZhY0xK6oLFJhwxGqK5.T8rMdG3K2e',
        'DONO', true);

-- Tipos de serviço comuns com preços (loja e externo/domicílio)
INSERT INTO tipos_servico (nome, descricao, preco, preco_externo, categoria, ativo, eh_chave) VALUES
('Chave simples', 'Cópia de chave residencial comum', 15.00, 25.00, 'CHAVE', true, true),
('Chave codificada', 'Cópia de chave codificada automotiva', 80.00, 120.00, 'CHAVE', true, true),
('Chave tetra', 'Cópia de chave tetra para fechaduras', 25.00, 40.00, 'CHAVE', true, true),
('Chave yale', 'Cópia de chave yale residencial', 15.00, 25.00, 'CHAVE', true, true),
('Chave gorjé', 'Cópia de chave gorjé para portões', 20.00, 35.00, 'CHAVE', true, true),
('Cópia controle', 'Cópia de controle remoto de portão', 60.00, 80.00, 'CONTROLE', true, false),
('Conserto fechadura', 'Reparo de fechadura com troca de peças', 80.00, 120.00, 'FECHADURA', true, false),
('Troca fechadura', 'Substituição completa de fechadura', 150.00, 200.00, 'FECHADURA', true, false),
('Abertura de porta', 'Abertura de porta trancada sem danos', 100.00, 150.00, 'FECHADURA', true, false),
('Carimbo automático', 'Confecção de carimbo automático personalizado', 45.00, 45.00, 'CARIMBO', true, false),
('Carimbo de bolso', 'Confecção de carimbo de bolso', 30.00, 30.00, 'CARIMBO', true, false);
