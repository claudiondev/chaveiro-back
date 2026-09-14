# 🔑 Chaveiro Abençoado — Backend API

<div align="center">

![Java](https://img.shields.io/badge/Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%204.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

![Status](https://img.shields.io/badge/Status-🚀%20MVP%20completo-0F6E56?style=for-the-badge)
![License](https://img.shields.io/badge/License-Proprietária-red?style=for-the-badge)

**API REST para gestão completa de um chaveiro: serviços, caixa, produção de chaves, atendimento a domicílio e fechamento diário.**

</div>

---

## 📖 Sobre o Projeto

O **Chaveiro Abençoado** é um sistema mobile-first pensado para o dia a dia de um chaveiro de verdade: dois perfis de usuário (**DONO** e **FUNCIONÁRIO**), tabela de preços sempre à mão, registro rápido de serviço e um caixa que fecha sozinho no fim do dia.

Princípio de design que guiou todo o backend: **cada toque na tela deve registrar algo útil**. O funcionário não quer preencher formulário — quer bater o olho no preço, registrar o serviço e seguir pro próximo cliente.

Este repositório é o **backend** da plataforma — uma API REST em Java 17 e Spring Boot 4, com autenticação JWT, controle de acesso por perfil e PostgreSQL como banco.

> **Fase atual:** MVP completo — auth, tipos de serviço, registro de serviços, caixa/movimentações e relatórios implementados e validados ponta a ponta. Preparando deploy (Render + Neon).

---

## ✨ Funcionalidades

### 🔐 Autenticação & Segurança
- ✅ Login com **JWT** e dois perfis (`DONO`, `FUNCIONARIO`)
- ✅ Cadastro de funcionário (só o DONO cadastra)
- ✅ Troca de senha, listagem e ativação/desativação de funcionários
- ✅ Rate limiting no login (5 tentativas, bloqueio de 15 min)
- ✅ CORS configurável por variável de ambiente

### 🏷️ Tipos de Serviço
- ✅ CRUD completo (nome, preço loja, preço domicílio, categoria, se conta como chave)
- ✅ Tabela de preços visível para todos os perfis

### 🛠️ Serviços Realizados
- ✅ Registro rápido (tipo, quantidade, forma de pagamento, domicílio)
- ✅ Cancelamento no mesmo dia (dono ou quem criou)
- ✅ Atendimento a domicílio com endereço e taxa de deslocamento
- ✅ Serviço em garantia (refeito, sem impacto no caixa)
- ✅ Fiado — status `PENDENTE`/`PAGO`, com tela de "contas a receber"

### 💰 Caixa & Movimentações
- ✅ Abertura de caixa com valor inicial (um por dia)
- ✅ Entradas/saídas avulsas categorizadas
- ✅ Fechamento diário (só o DONO confirma)
- ✅ Consulta de caixa por data

### 📊 Relatórios
- ✅ Diário, semanal e mensal
- ✅ Contagem de chaves produzidas por período

---

## 🏗️ Arquitetura e Estrutura

```
chaveiro.back/
└── src/main/java/com/chaveiro_abencoado/back/
    ├── config/
    │   ├── SecurityConfig.java          🔒 Spring Security + CORS
    │   └── JwtAuthenticationFilter.java 🔑 Filtro JWT
    │
    ├── controller/
    │   ├── AuthController.java          🔐 Login, cadastro, senha, funcionários
    │   ├── TipoServicoController.java   🏷️ CRUD de tipos de serviço
    │   ├── ServicoController.java       🛠️ Registro/consulta de serviços
    │   ├── CaixaController.java         💰 Abertura, movimentação, fechamento
    │   └── RelatorioController.java     📊 Relatórios e contagem de chaves
    │
    ├── service/
    │   ├── AuthService.java / JwtService.java
    │   ├── TipoServicoService.java
    │   ├── ServicoService.java
    │   ├── CaixaService.java
    │   └── RelatorioService.java
    │
    ├── model/
    │   ├── Usuario, TipoServico, ServicoRealizado
    │   ├── MovimentacaoCaixa, FechamentoDiario
    │   └── enums: UserRole, FormaPagamento, CategoriaServico,
    │              TipoMovimentacao, StatusFechamento,
    │              StatusPagamento, CategoriaSaida
    │
    ├── repository/                      Spring Data JPA
    ├── dto/                             Requests/Responses (Bean Validation)
    ├── exception/                       GlobalExceptionHandler + tipos
    └── BackApplication.java

src/main/resources/db/migration/         Flyway (V1 schema, V2 seed)
```

### 📊 Fluxo de Dados

```
HTTP Request
     ↓
JwtAuthenticationFilter (valida token, define SecurityContext)
     ↓
Controller (valida entrada — Bean Validation)
     ↓
Service (regras de negócio, verificação de perfil)
     ↓
Repository (Spring Data JPA)
     ↓
PostgreSQL
     ↓
Response JSON 200 / 400 / 401 / 403 / 404
```

---

## 🗃️ Modelo de Dados

| Entidade | Campos principais | Relacionamentos |
|---|---|---|
| `Usuario` | nome, email, senha, role (DONO/FUNCIONARIO), ativo, percentualComissao | 1:N Serviço, Movimentação, Fechamento |
| `TipoServico` | nome, preço, preçoExterno, categoria, ativo, ehChave | 1:N ServicoRealizado |
| `ServicoRealizado` | quantidade, valorUnitario/Total, formaPagamento, statusPagamento, domicílio, endereço, taxaDeslocamento, isGarantia, metadados (JSONB) | N:1 TipoServico, Usuario, FechamentoDiario |
| `MovimentacaoCaixa` | tipo (ENTRADA/SAIDA), valor, descrição, categoriaSaida | N:1 Usuario, FechamentoDiario |
| `FechamentoDiario` | data, valorAbertura, totalEntradas/Saídas, saldoFinal, totalServiços/Chaves, status | N:1 Usuario, 1:N Serviço, Movimentação |

---

## 🔌 Endpoints da API

### 🔑 Autenticação (`/api/auth`)

| Método | Endpoint | Descrição | Acesso |
|--------|----------|-----------|--------|
| POST | `/api/auth/login` | Login, retorna JWT | Público |
| POST | `/api/auth/cadastro` | Cadastra funcionário | DONO |
| PUT | `/api/auth/senha` | Alterar a própria senha | Todos |
| GET | `/api/auth/usuarios` | Listar funcionários | DONO |
| PATCH | `/api/auth/usuarios/{id}/status` | Ativar/desativar funcionário | DONO |

### 🏷️ Tipos de Serviço (`/api/tipos-servico`)

| Método | Endpoint | Descrição | Acesso |
|--------|----------|-----------|--------|
| GET | `/api/tipos-servico` | Lista todos (tabela de preços) | Todos |
| POST | `/api/tipos-servico` | Cria novo tipo | DONO |
| PUT | `/api/tipos-servico/{id}` | Atualiza preço/dados | DONO |
| DELETE | `/api/tipos-servico/{id}` | Desativa tipo | DONO |

### 🛠️ Serviços Realizados (`/api/servicos`)

| Método | Endpoint | Descrição | Acesso |
|--------|----------|-----------|--------|
| POST | `/api/servicos` | Registra serviço | Todos |
| GET | `/api/servicos?data=` | Lista do dia | Todos |
| GET | `/api/servicos/pendentes` | Contas a receber (fiado) | DONO |
| PATCH | `/api/servicos/{id}/pagar` | Marca pendente como pago | Todos |
| DELETE | `/api/servicos/{id}` | Cancela serviço (mesmo dia, dono ou criador) | Todos |

### 💰 Caixa & Movimentações (`/api/caixa`)

| Método | Endpoint | Descrição | Acesso |
|--------|----------|-----------|--------|
| POST | `/api/caixa/abertura` | Abre o caixa com valor inicial | Todos |
| GET | `/api/caixa/hoje` | Status do caixa atual | Todos |
| POST | `/api/caixa/movimentacao` | Registra entrada/saída avulsa | Todos |
| POST | `/api/caixa/fechamento` | Fecha o dia | DONO |
| GET | `/api/caixa/historico?data=` | Consulta caixa por data | DONO |

### 📊 Relatórios (`/api/relatorios`)

| Método | Endpoint | Descrição | Acesso |
|--------|----------|-----------|--------|
| GET | `/api/relatorios/diario?data=` | Resumo do dia | DONO |
| GET | `/api/relatorios/semanal` | Resumo da semana | DONO |
| GET | `/api/relatorios/mensal?mes=&ano=` | Resumo do mês | DONO |
| GET | `/api/relatorios/chaves?periodo=` | Contagem de chaves | DONO |

---

## 📐 Regras de Negócio

- **Caixa:** não é possível registrar serviço sem o caixa aberto; só um caixa aberto por dia; `saldoFinal = valorAbertura + totalEntradas - totalSaidas`; só o DONO confirma o fechamento; todo serviço gera automaticamente uma movimentação de ENTRADA.
- **Contagem de chaves:** soma automática de `quantidade` dos serviços com `ehChave = true` no período — aparece no fechamento diário e nos relatórios.
- **Domicílio:** `valorTotal = (valorUnitario × quantidade) + taxaDeslocamento`, com taxa configurável pelo dono.
- **Garantia:** serviço refeito (`isGarantia = true`) tem valor zero e não afeta o caixa — mede produtividade sem duplicar receita.
- **Fiado:** `statusPagamento` (`PAGO`/`PENDENTE`) alimenta a tela de contas a receber, sem bloquear o registro do serviço.

---

## 🚀 Tecnologias

<div align="center">

| Tecnologia | Versão | Função |
|---|---|---|
| **Java** | 17 | Linguagem principal |
| **Spring Boot** | 4.1.0 | Framework Web/REST |
| **Spring Security** | 6.x | Autenticação e autorização |
| **Spring Data JPA** | — | ORM com Hibernate |
| **JWT (JJWT)** | 0.12.6 | Tokens stateless |
| **PostgreSQL** | 16 | Banco de dados relacional |
| **Flyway** | — | Versionamento de schema |
| **Lombok** | — | Redução de boilerplate |
| **Bean Validation** | — | Validação de entrada |
| **Springdoc OpenAPI** | 2.8.0 | Documentação Swagger |
| **Maven** | 3.9+ | Build e dependências |

</div>

---

## 💻 Como Rodar Localmente

### Pré-requisitos
- Java 17+
- PostgreSQL rodando em `localhost:5432`
- Maven (ou use o `./mvnw` incluso)

### Passos

```bash
# Clone o repositório
git clone https://github.com/claudiondev/chaveiro-back.git
cd chaveiro-back
```

```sql
-- Crie o banco de dados
CREATE DATABASE chaveiro;
```

```bash
# Configure as variáveis de ambiente (todas têm default de dev, ajuste se necessário)
export DB_URL=jdbc:postgresql://localhost:5432/chaveiro
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=chave-secreta-dev-apenas-nao-usar-em-producao-32chars!

# Rode a aplicação (profile dev por padrão)
./mvnw spring-boot:run
```

API disponível em `http://localhost:8080`. O Flyway aplica o schema (`V1`) e o seed de tipos de serviço (`V2`) automaticamente no primeiro boot.

### Testes

```bash
./mvnw test
```

24 testes (JUnit 5 + Mockito), banco H2 em memória — nenhuma dependência do PostgreSQL local.

### Rodar via Docker

```bash
docker build -t chaveiro-back .
docker run -p 8080:8080 \
  -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... \
  chaveiro-back
```

---

## 🌐 Deploy (planejado)

| Camada | Serviço | Variáveis |
|---|---|---|
| Backend | Render (Docker, profile `prod`) | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS` |
| Banco | Neon PostgreSQL (free tier) | — |
| Frontend | Vercel | `VITE_API_URL` |

---

## 🔗 Frontend

O frontend do Chaveiro Abençoado é desenvolvido em **React 18 + Vite + Tailwind CSS**, mobile-first, com PWA instalável no celular.

📦 Repositório: [chaveiro-front](https://github.com/claudiondev/chaveiro-front)

---

## 👨‍💻 Autor

**Claudio Nascimento**

- 🔗 GitHub: [@claudiondev](https://github.com/claudiondev)
- 📧 Email: claudiondev@gmail.com

---

## 📄 Licença

**LICENÇA PROPRIETÁRIA - VISUALIZAÇÃO APENAS**

Este código é um **projeto de portfólio** protegido por direitos autorais.

### ✅ Permitido:
- 👀 Visualizar e estudar o código
- 💼 Usar como referência em entrevistas
- 📚 Aprender com as implementações

### ❌ Proibido:
- 🚫 Copiar ou usar comercialmente sem autorização
- 🚫 Distribuir sem permissão do autor

```
Copyright © 2026 Claudio Nascimento. Todos os direitos reservados.
```

---

<div align="center">

**Desenvolvido por Claudio Nascimento**

</div>
