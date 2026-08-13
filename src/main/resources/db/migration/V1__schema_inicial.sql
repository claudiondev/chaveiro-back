CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    percentual_comissao DECIMAL(5, 2)
);

CREATE TABLE tipos_servico (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    preco DECIMAL(10, 2) NOT NULL,
    preco_externo DECIMAL(10, 2),
    categoria VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    eh_chave BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE fechamentos_diarios (
    id BIGSERIAL PRIMARY KEY,
    data DATE NOT NULL,
    valor_abertura DECIMAL(10, 2) NOT NULL,
    total_entradas DECIMAL(10, 2) DEFAULT 0,
    total_saidas DECIMAL(10, 2) DEFAULT 0,
    saldo_final DECIMAL(10, 2) DEFAULT 0,
    total_servicos INTEGER DEFAULT 0,
    total_chaves INTEGER DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'ABERTO',
    observacao VARCHAR(500),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id)
);

CREATE TABLE servicos_realizados (
    id BIGSERIAL PRIMARY KEY,
    quantidade INTEGER NOT NULL,
    valor_unitario DECIMAL(10, 2) NOT NULL,
    valor_total DECIMAL(10, 2) NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    status_pagamento VARCHAR(10) NOT NULL DEFAULT 'PAGO',
    observacao VARCHAR(500),
    domicilio BOOLEAN NOT NULL DEFAULT FALSE,
    endereco VARCHAR(300),
    taxa_deslocamento DECIMAL(10, 2),
    data_hora TIMESTAMP NOT NULL,
    is_garantia BOOLEAN NOT NULL DEFAULT FALSE,
    metadados JSONB,
    tipo_servico_id BIGINT NOT NULL REFERENCES tipos_servico(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    fechamento_diario_id BIGINT REFERENCES fechamentos_diarios(id)
);

CREATE TABLE movimentacoes_caixa (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(10) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    descricao VARCHAR(255),
    categoria_saida VARCHAR(20),
    data_hora TIMESTAMP NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    fechamento_diario_id BIGINT REFERENCES fechamentos_diarios(id)
);

CREATE INDEX idx_servicos_data_hora ON servicos_realizados(data_hora);
CREATE INDEX idx_servicos_usuario ON servicos_realizados(usuario_id);
CREATE INDEX idx_servicos_fechamento ON servicos_realizados(fechamento_diario_id);
CREATE INDEX idx_movimentacoes_fechamento ON movimentacoes_caixa(fechamento_diario_id);
CREATE INDEX idx_fechamentos_data ON fechamentos_diarios(data);
