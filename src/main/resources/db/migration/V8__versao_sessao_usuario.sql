-- Trocar a senha não invalidava tokens já emitidos; um JWT vazado continuava valendo até
-- expirar (até 24h por padrão). O front carrega a versão vigente no momento do login; se
-- não bater mais com a do usuário, o token é tratado como revogado mesmo sem ter expirado.
ALTER TABLE usuarios ADD COLUMN versao_sessao INTEGER NOT NULL DEFAULT 1;
