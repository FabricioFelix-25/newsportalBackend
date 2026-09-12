# AlpesNews — revisão técnica e melhorias

Revisão concluída localmente em 12/09/2026. Frontend, backend e bot estão na branch `codex/otimizacao-revisao-privada`, com alterações ainda sem commit, push ou implantação. O conteúdo e as credenciais de produção não foram alterados.

## Resultado para o leitor e para o editor

O portal recebeu ajustes para reduzir as mudanças visíveis de tamanho durante o carregamento, evitar consultas repetidas e entregar menos JavaScript na abertura. O novo fluxo editorial permite abrir pelo Telegram a matéria completa em uma prévia privada, conferir imagens e fontes, editar e publicar pela mesma experiência de revisão.

```text
Pauta recente → pesquisa com fontes → seleção de imagens → rascunho
                                                           ↓
Telegram: Ver prévia privada → login editorial → matéria completa
                                                    ├─ Editar → salvar → prévia
                                                    └─ Confirmar revisão → publicar
```

A URL é `/admin/articles/{id}/preview`. O link não contém senha, JWT nem API key. Conhecer o endereço não permite ler o rascunho: o backend exige uma conta ativa ADMIN ou EDITOR. Caso existam outros editores autorizados, eles também podem acessar; não se trata de uma restrição exclusiva a uma pessoa nem de um link com prazo próprio de expiração.

## Carregamento e estabilidade visual

| Problema observado no código | Mudança implementada |
| --- | --- |
| Requisições repetidas e resultados antigos competindo com a navegação atual | Serviço estável, cache público de 60 segundos, reaproveitamento de requisições em andamento e descarte de respostas ultrapassadas |
| Cartões recebendo o HTML completo das matérias | Listagens públicas usam `summary=true`; o corpo é carregado ao abrir a matéria |
| Consultas secundárias segurando a leitura principal | Matéria principal independente das sugestões; carregamentos paralelos quando possível |
| Destaques, imagens e esqueletos com dimensões diferentes | Espaço reservado para mídia, dimensões compatíveis no carregamento e transição de destaque preservando a imagem anterior |
| Barra de navegação mudando de altura ao rolar | Altura constante para evitar deslocamento do conteúdo |
| Código do painel incluído na abertura da página inicial | Rotas editoriais e secundárias carregadas sob demanda |
| Editor e painel apertados no celular | Menu recolhível, campos e ações responsivos; retorno à prévia pelo início da página |
| Edição digitada perdida quando autores terminavam de carregar | Atualização da lista de autores preserva os campos já editados |

O backend também passou a comprimir respostas de texto/JSON a partir de 1 KB, buscar autores junto com artigos e carregar tags em lotes. A contagem de visualizações usa uma operação explícita e atômica; abrir uma prévia não incrementa o contador.

### Medida do build

| JavaScript necessário na abertura da página inicial | Antes | Depois | Redução |
| --- | ---: | ---: | ---: |
| Tamanho sem compressão | 333,20 KB | 235,14 KB | 29,4% |
| Tamanho estimado com gzip | 90,79 KB | 73,06 KB | 19,5% |

O valor atual soma o arquivo inicial de 71,28 KB e o pacote compartilhado React de 163,86 KB. Essa comparação mede o build, não o tempo real de carregamento, LCP ou CLS de produção. O CSS atual tem 49,88 KB. Imagens, conexão e a suspensão do Render continuam influenciando a experiência real.

## Prévia e publicação

- A prévia e a página pública compartilham a apresentação da matéria: capa, texto, imagens, legendas, blocos de citação, vídeos, fontes e etiquetas.
- O login preserva o destino privado e retorna à matéria solicitada. O redirecionamento aceita somente caminhos editoriais locais.
- A prévia permite abrir o editor e publicar depois de confirmar fatos/fontes, direitos das imagens e conteúdo sensível. As confirmações começam desmarcadas na revisão.
- A publicação envia somente o checklist e as referências, sem reenviar um corpo de matéria potencialmente desatualizado. A data de publicação representa a aprovação; repetir a operação não muda novamente essa data.
- Rotas públicas de ID, slug, listagens, categorias, busca, destaques, autor e tags não entregam rascunhos.
- Conteúdo editorial privado não entra no cache público. Respostas privadas usam `no-store`; a prévia recebe instruções de não indexação.
- HTML inserido pelo editor ou retornado pela API é sanitizado na apresentação. Scripts, manipuladores de eventos, URLs perigosas, estilos de sobreposição e embeds não permitidos são removidos. Embeds YouTube aceitos usam o domínio de privacidade.

Os contratos e as regras de autorização estão detalhados em [revisao-privada.md](./revisao-privada.md).

## Qualidade do bot e das imagens

O bot autônomo e o webhook interativo adotam as mesmas regras editoriais. RSS sem data válida, futuro ou mais antigo que sete dias é descartado. A geração exige pesquisa com Google Search, referências de grounding, data do fato e datas das fontes dentro da janela; essas datas são conferidas novamente antes de salvar. A IA não marca o próprio conteúdo como revisado.

Para imagens, a busca usa termos da entidade citada na pauta e confere os metadados de candidatos do Wikimedia Commons, incluindo crédito, licença e dimensões. As imagens selecionadas usam versões de até 1600 pixels. Pexels fica restrito a assuntos conceituais com autorização explícita no pedido de imagem. Fotos de arquivo/ilustrativas recebem identificação e atribuição.

Quando não há correspondência suficiente, o bot deixa a capa vazia para escolha no editor. Foram removidos os preenchimentos com imagem aleatória ou sintética. Essa escolha evita apresentar uma foto sem relação com o fato como se fosse registro jornalístico.

A avaliação é por metadados: ainda cabe ao editor conferir visualmente a identidade, o contexto e a licença. Datas declaradas pelo modelo também precisam ser confrontadas com as fontes. A janela de sete dias é aplicada ao gerar/salvar; uma matéria deixada vários dias em rascunho exige nova conferência antes de publicar.

O Telegram recebe título, resumo e botões com o ID realmente salvo. Uma falha de notificação não provoca outro POST de criação. O workflow mantém a execução a cada seis horas, impede execuções simultâneas do mesmo fluxo e roda os testes offline antes do bot.

## Segurança e configuração necessária

A API key agora concede apenas `ROLE_BOT`, permitindo criar rascunhos. Não permite ler prévias, publicar, editar ou excluir matérias. Chaves vazias não autenticam; um header de autorização JWT não é substituído silenciosamente pela API key. Contas desativadas não conseguem usar a prévia. Cadastro de usuários continua restrito a administradores, com proteção também na camada de serviço.

O webhook verifica o segredo enviado pelo Telegram e o chat permitido; pode também restringir o remetente por `TELEGRAM_USER_ID`. Chamadas não autenticadas não disparam geração de matérias. O mecanismo antigo de imprimir tokens de recuperação de senha quando a opção de depuração estava habilitada foi removido. O envio de recuperação por e-mail ainda não existe; a tela agora orienta procurar o administrador, sem afirmar que enviou um token.

### Ordem de implantação

1. Conferir no Render a API key, o segredo JWT próprio de produção e a origem CORS do portal. Implantar o backend com os novos contratos.
2. Preparar as variáveis do frontend/webhook na Vercel e registrar no Telegram o mesmo segredo no parâmetro `secret_token` do webhook. Implantar o frontend compatível.
3. Implantar o bot autônomo compatível no GitHub. Ele depende das novas rotas privadas do frontend e das permissões de rascunho do backend.
4. Conferir em ambiente implantado o login pelo link, a recusa de acesso anônimo, a edição, a publicação e a aparição pública da matéria; medir carregamento com cache vazio e com backend acordado/frio.

| Ambiente | Configuração |
| --- | --- |
| Render | `APP_SECURITY_JWT_SECRET`, `APP_AI_API_KEY`, `APP_CORS_ALLOWED_ORIGINS` e as configurações existentes do banco |
| Build Vercel | `VITE_API_BASE_URL=https://api-newsportal.onrender.com/api` |
| Webhook Vercel | `GEMINI_API_KEY`, `APP_AI_API_KEY`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, novo `TELEGRAM_WEBHOOK_SECRET` |
| Webhook — opcionais | `TELEGRAM_USER_ID`, `PEXELS_API_KEY`, `SITE_URL`, `BOT_AUTHOR_ID`, `NEWSPORTAL_API_URL` |
| GitHub Actions | Secrets `GEMINI_API_KEY`, `APP_AI_API_KEY`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`; opcionais `PEXELS_API_KEY`, `NEWSPORTAL_API_URL`; variables `SITE_URL`, `BOT_AUTHOR_ID` |

`NEWSPORTAL_API_URL`, quando usado, inclui `/api/articles`. `SITE_URL` tem como padrão `https://alpesnews.vercel.app`; `BOT_AUTHOR_ID` tem padrão `1` e precisa identificar um autor existente. O segredo do webhook é enviado em um header pelo Telegram; não deve ser incluído nos links de revisão. Sem esse segredo configurado, o novo webhook responde 503. A configuração solicita duração máxima de 300 segundos na Vercel; sua disponibilidade deve ser confirmada no projeto hospedado.

Não foram executados cadastro de webhook, envio real ao Telegram, geração paga com Gemini, alteração de secrets, disparo de workflow ou publicação de notícias reais durante os testes.

## Validação executada

| Componente | Resultado |
| --- | --- |
| Frontend | 34 testes aprovados: cache/requisições, segurança editorial, preservação da edição e webhook com serviços simulados |
| Backend | 30 testes aprovados, incluindo 11 de integração com JWT, cadeia de segurança real e banco H2 descartável |
| Bot autônomo | 19 testes offline aprovados para recência, geração, seleção de imagens e notificações |
| Build frontend | TypeScript e Vite concluídos com sucesso |
| Lint frontend | Nenhum erro; três avisos de organização de exports para Fast Refresh nos contextos |
| Git | `git diff --check` sem erros nos três repositórios |
| Navegador | Página inicial em desktop/celular e fluxo privado com API local simulada: login, revisão, edição, salvamento, retorno à prévia e publicação fictícia |

Total: **83 testes aprovados**. Os testes não usaram o PostgreSQL de produção. O Maven foi executado com o JDK 25 instalado nesta máquina; a configuração de compilação do projeto continua a existente. Há avisos de bibliotecas sobre APIs Java e de migração futura do React Router, sem falhas de teste.

## Pendências que permanecem

| Prioridade | Pendência e efeito |
| --- | --- |
| Antes da ativação | Implantar os três componentes de forma coordenada e configurar o segredo do webhook; os links novos dependem dessa implantação |
| Segurança das dependências | Após atualizações compatíveis, o último `npm audit` ainda informou seis ocorrências no frontend: cinco moderadas e uma alta, envolvendo Vite/esbuild, Vitest e React Router. A correção indicada exige migrações de versões principais, que não foram forçadas nesta mudança |
| Confiabilidade do webhook | A deduplicação por `update_id` existe somente na memória da instância; uma fila e armazenamento persistente são necessários para impedir duplicidade entre instâncias ou reinicializações |
| Revisão simultânea | Não há bloqueio por versão entre abrir a prévia e publicar. Outro editor pode alterar a matéria nesse intervalo |
| Render | A suspensão por inatividade e o plano de hospedagem não foram alterados; o cold start ainda pode atrasar a primeira leitura |
| Banco | `summary=true` reduz a resposta HTTP, mas o corpo ainda é materializado na consulta SQL; uma projeção específica é uma otimização posterior |
| Recuperação de acesso | Falta integrar o envio privado de recuperação por e-mail |
| Componentes legados | O bot Python de `C:\programas\alpes-bot` e o painel estático do backend não foram migrados para esse fluxo. O Python ainda tem convenções antigas; não deve operar em paralelo sem atualização. A nova API key recusa publicação direta |
| Medição de audiência | O contador é atômico, mas não deduplica visitantes nem limita incrementos por origem |

Esta revisão cobre os fluxos públicos, editoriais, de autenticação e de geração envolvidos no pedido, suas integrações e verificações automatizadas. Não representa certificação de segurança de todas as dependências nem validação editorial das matérias já publicadas.
