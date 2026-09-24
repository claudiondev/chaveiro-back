CREATE TABLE progressos_ajuda (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    guia VARCHAR(30) NOT NULL,
    versao INTEGER NOT NULL CHECK (versao > 0),
    status VARCHAR(20) NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_progresso_ajuda_usuario_guia UNIQUE (usuario_id, guia)
);

CREATE INDEX idx_progressos_ajuda_usuario ON progressos_ajuda(usuario_id);
