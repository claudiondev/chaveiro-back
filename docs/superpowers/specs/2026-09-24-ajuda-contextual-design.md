# Ajuda contextual e tutorial inicial

## Objetivo

Ensinar cada funcionalidade no contexto da tela em que ela aparece e manter uma ajuda acessível para usuários que precisem relembrar o fluxo. O recurso deve orientar sem executar ações de negócio, alterar formulários ou expor funcionalidades incompatíveis com o perfil autenticado.

## Experiência do usuário

- No primeiro acesso, o usuário recebe uma apresentação curta e pode iniciar ou dispensar o tutorial.
- Cada tela possui um botão de ajuda no canto superior direito.
- A ajuda abre um painel com uma explicação da tela, dicas rápidas e a opção de iniciar um passo a passo.
- O passo a passo destaca um elemento por vez e oferece voltar, avançar e sair.
- A conclusão ou dispensa de cada guia é salva por usuário no servidor.
- Guias exclusivos do dono não são exibidos ao funcionário.

## Arquitetura

### Backend

Uma tabela `progressos_ajuda` registra `usuario_id`, identificador do guia, versão, situação e data de atualização. A combinação de usuário e guia é única.

Endpoints autenticados:

- `GET /api/ajuda/progressos`: lista o progresso do usuário autenticado.
- `PUT /api/ajuda/progressos/{guia}`: cria ou atualiza a versão e situação do guia.

O usuário é obtido do JWT. O cliente não envia `usuarioId`. Identificadores de guia e situações aceitas são validados no servidor.

### Frontend

- `HelpProvider` concentra estado, carregamento do progresso e abertura da ajuda.
- `HelpButton` fornece o acesso permanente no cabeçalho.
- `HelpPanel` exibe resumo, dicas rápidas e início do guia.
- `GuidedTour` cria a sobreposição, destaca o alvo atual e controla o foco.
- Um catálogo declarativo guarda título, descrição, dicas, perfil e etapas de cada rota.
- Elementos guiados usam atributos `data-tour`, evitando dependência de classes visuais.

## Regras de segurança e comportamento

- A sobreposição bloqueia cliques na interface durante o guia.
- O guia nunca dispara registro de serviço, movimentação ou fechamento de caixa.
- Campos preenchidos permanecem intactos ao abrir e fechar a ajuda.
- Etapas cujo alvo não existe são mostradas sem destaque e continuam navegáveis.
- O progresso remoto é uma conveniência: falhas na API não impedem o uso do sistema.
- O tour fecha ao trocar de rota ou sair da conta.
- A interface suporta teclado, tecla Escape, foco visível e preferência por movimento reduzido.

## Conteúdo inicial

- Início: situação do caixa, movimento, resumo financeiro e últimos atendimentos.
- Serviços: busca, categorias, preços e início de atendimento.
- Registrar serviço: seleção, quantidade, pagamento, domicílio e confirmação.
- Caixa: abertura, resumo, movimentações e fechamento conforme perfil e estado.
- Fechamento: conferência, histórico e PDF conforme perfil.
- Relatórios: período, indicadores e detalhamentos do dono.
- Mais: perfil, rotinas operacionais e administração conforme perfil.

As dicas rápidas cobrem dúvidas frequentes, como bloqueio no registro sem caixa aberto, diferença entre entrada de serviço e entrada avulsa, preço externo e acesso ao fechamento.

## Validação

- Testes unitários do serviço de progresso verificam criação, atualização e isolamento por usuário.
- Build e análise estática do frontend verificam a integração.
- Validação manual cobre celular, desktop, PWA, perfis DONO/FUNCIONARIO, caixa aberto/fechado, navegação por teclado e formulário parcialmente preenchido.
