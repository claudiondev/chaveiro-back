-- A senha documentada para o dono semeado na V2 nunca correspondeu ao hash gravado
-- (confirmado com BCryptPasswordEncoder), então ninguém nunca conseguiu logar com aquela
-- conta. Como o login sempre falhou, é seguro assumir que ninguém trocou a senha por essa
-- via (alterarSenha exige logar primeiro) — então o fingerprint abaixo só bate se a conta
-- ainda estiver exatamente como a V2 a deixou.
--
-- Não apagamos a linha (poderia quebrar FKs de dados que alguém tenha criado manualmente
-- com esse usuario_id); só desativamos. O DonoInicialInitializer cria/reativa o acesso
-- real a partir de DONO_INICIAL_EMAIL/DONO_INICIAL_SENHA na subida da aplicação.
UPDATE usuarios
SET ativo = false
WHERE email = 'dono@chaveiro.com'
  AND senha = '$2a$10$8KxWzYx7GqNvGxQv8yZpVOJGZhY0xK6oLFJhwxGqK5.T8rMdG3K2e';
