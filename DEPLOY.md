# Chaveiro Abençoado — Deploy (Render + Neon)

Backend no **Render** (Docker, plano free), banco no **Neon** (PostgreSQL free), frontend já publicado na **Vercel**
(repo `chaveiro-front`). Criar contas, gerar segredos e confirmar os passos no navegador é com você — aqui vai o
roteiro.

**O que já está pronto no repositório:** `Dockerfile`, `render.yaml` (Blueprint do Render), `GET /api/health`
(público, sem autenticação — usado pelo health check do Render e pelo monitor de uptime), `application-prod.properties`
com CORS dinâmico, Flyway habilitado e Swagger desligado.

> **Plano free muda com o tempo.** Confira limites atuais nos sites antes de criar as contas. O que importa aqui:
> o **Render free dorme** depois de ~15 min sem tráfego (a primeira requisição depois disso demora uns 30s–1min pra
> acordar), e o **Neon free suspende o banco** quando fica ocioso, além de ter teto de horas de computação por mês.
> Resolvemos o Render com um monitor de uptime (passo 4); o Neon acorda sozinho na primeira query após a suspensão,
> só acompanhe o consumo no painel no primeiro mês.

## 1. Neon (banco)

1. Crie a conta e um **projeto** Postgres. Escolha a região **Ohio (us-east-2)** — combina com o Render (passo 2) e,
   partindo do Brasil, é mais perto que a costa oeste (Oregon): a conectividade do Brasil com os EUA passa
   majoritariamente pelo Atlântico/costa leste.
2. Em **Connect**, desligue _Connection pooling_ e copie a string **direta** (host sem `-pooler`). Formato:
   `postgresql://usuario:senha@host.neon.tech/chaveiro?sslmode=require`.
3. Quebre essa string em três variáveis (é o formato que `application-prod.properties` espera):
   - `DB_URL` = `jdbc:postgresql://host.neon.tech/chaveiro?sslmode=require` (troca `postgresql://` por `jdbc:postgresql://` e tira usuário/senha da URL)
   - `DB_USERNAME` = o usuário da string
   - `DB_PASSWORD` = a senha da string

## 2. Render (API)

1. **New > Blueprint**, escolha o repositório `claudiondev/chaveiro.back`. O Render lê o `render.yaml`.
2. O blueprint já vem com `region: ohio` (combina com o Neon em Ohio, passo 1). Só troque se escolher outra região.
3. Ele vai pedir os valores marcados `sync: false` no `render.yaml`:

   | Variável | Valor |
   |---|---|
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | do Neon (passo 1) |
   | `CORS_ORIGINS` | a URL **https** do front na Vercel que você já publicou (ex: `https://chaveiro-front.vercel.app`) |
   | `DONO_INICIAL_NOME`, `DONO_INICIAL_EMAIL`, `DONO_INICIAL_SENHA` | seu acesso de DONO (senha: 8+ caracteres, 1 maiúscula, 1 número) — **remova essas 3 variáveis do Render depois do primeiro login bem-sucedido**, elas só são lidas enquanto não existe um DONO ativo |

   `JWT_SECRET` já vem como `generateValue: true` — o Render gera sozinho, você não precisa definir.

4. **Apply.** O Dockerfile builda e sobe a aplicação; o `healthCheckPath: /api/health` no blueprint é o que o Render
   usa pra saber que o serviço está de pé. Anote o endereço `https://chaveiro-back-xxxx.onrender.com`.

## 3. Conferir

1. `curl https://chaveiro-back-xxxx.onrender.com/api/health` → `{"status":"UP"}`.
2. Login com o `DONO_INICIAL_EMAIL`/`DONO_INICIAL_SENHA` pelo front publicado na Vercel (confere CORS e JWT juntos).
3. Depois do primeiro login OK, **apague as 3 variáveis `DONO_INICIAL_*`** no painel do Render (Environment).

## 4. Manter a API acordada

No [UptimeRobot](https://uptimerobot.com) (plano gratuito), crie um monitor HTTP em
`https://chaveiro-back-xxxx.onrender.com/api/health` a cada **5 minutos** (menor intervalo do plano free). Sem isso,
o Render derruba a instância por inatividade e o primeiro acesso do dia vem lento.

## Operação

- **Primeira requisição lenta:** se o monitor falhar por algum motivo ou o Render reiniciar, a API leva uns 30s–1min
  pra acordar. Normal no plano gratuito.
- **Consumo do Neon:** acompanhe as horas de computação do mês no painel nas primeiras semanas.
- **Logs:** painel do Render (aba Logs), em texto simples.
- **Atualizar:** cada push em `main` redeploya a API (auto-deploy do Render ligado por padrão no Blueprint).
  Migrations do Flyway só avançam — pra desfazer algo, escreva uma migration nova.
- **Backup:** Neon free guarda histórico curto de point-in-time restore; `pg_dump` periódico a partir do `DB_URL` é
  o seguro de verdade.
- **CORS_ORIGINS:** se o domínio da Vercel mudar (domínio próprio, por exemplo), atualize essa variável no Render.
