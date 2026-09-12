# Revisão privada e publicação editorial

As URLs abaixo são relativas à base `/api`. Os exemplos não contêm credenciais reais.

## Contratos

### Prévia privada

`GET /api/articles/admin/{id}/preview`

- Exige `Authorization: Bearer <JWT>` de uma conta ativa com papel `ADMIN` ou `EDITOR`.
- Retorna o `ArticleResponse` completo, incluindo conteúdo, imagens, fontes e checklist.
- Pode consultar rascunhos e artigos publicados. O ID na URL não concede acesso por si só.
- Responde com `Cache-Control: no-store, private` e `X-Robots-Tag: noindex, nofollow, noarchive`.
- Não registra visualização nem modifica a matéria.
- Sem autenticação: `401`; sem permissão: `403`; ID inexistente: `404`.

A lista editorial continua em `GET /api/articles/admin?page=0&size=20`, com as mesmas permissões e cache privado. A autenticação ocorre no backend; esconder apenas a rota no React não basta.

### Publicação da versão revisada

`POST /api/articles/{id}/publish`

Exige o JWT editorial e um JSON com o checklist:

```json
{
  "sourceReferences": "https://fonte.example/materia",
  "reviewedBy": "Nome do responsável",
  "factChecked": true,
  "rightsCleared": true,
  "sensitiveContentReviewed": true
}
```

O retorno é o `ArticleResponse` completo. Campos ausentes, fontes/revisor vazios ou confirmações falsas produzem `400`. A operação atualiza o checklist, o estado de publicação e a data de publicação; o cliente não reenvia título, corpo, imagens, tags ou autor. Atualizações Hibernate incluem somente os campos alterados.

Uma repetição após a matéria já ter sido publicada retorna sua versão atual, sem mudar novamente a data. `publishedAt` passa a representar a aprovação, inclusive quando o rascunho foi criado dias antes. Uma criação ou edição pelo endpoint completo que transforme rascunho em publicação também aplica a data correta.

### Listas mais leves

O parâmetro opcional `summary=true` está disponível em:

- `GET /api/articles`
- `GET /api/articles/featured`
- `GET /api/articles/category/{category}`
- `GET /api/articles/author/{authorId}`
- `GET /api/articles/search?q=...` ou `?tag=...`

O formato permanece compatível com `ArticleResponse`; somente `content` fica vazio. Sem o parâmetro, o corpo completo continua presente. As páginas aceitam no máximo 100 resultados; `page` negativo vira 0 e `size` menor que 1 vira 1. Clientes que precisam de todos os resultados devem percorrer `totalPages`.

`GET /api/articles/{id}` e `GET /api/articles/slug/{slug}` retornam somente publicações, mesmo quando o visitante possui JWT editorial. Rascunhos retornam `404`; o editor deve usar a prévia privada.

### Visualizações

`GET /api/articles/slug/{slug}` é uma leitura sem gravações. O frontend deve enviar uma chamada explícita `POST /api/articles/{id}/view` após uma leitura pública válida. A atualização do contador é atômica, não altera a data editorial e rejeita rascunhos com `404`. A prévia não deve chamar essa rota.

## Segurança

- Uma API key válida recebe somente `ROLE_BOT`, com permissão para criar rascunhos por `POST /api/articles`.
- O bot não pode publicar, editar artigos existentes, excluir, consultar prévias ou acessar a lista editorial. `isDraft: false` é rejeitado com `403`; `null` é rejeitado com `400`. Rascunhos sem imagem continuam aceitos.
- Autores humanos podem criar rascunhos; edição, revisão e publicação exigem `ADMIN`/`EDITOR`.
- Uma chave configurada em branco não autentica. A comparação usa `MessageDigest.isEqual`.
- Se houver header `Authorization`, a API key não substitui a autenticação JWT. JWT de uma conta desativada não libera acesso.
- `POST /api/auth/register` exige `ADMIN`, inclusive na camada de serviço. Um JSON com `role: ADMIN` não permite cadastro anônimo.
- As consultas públicas de lista, destaque, categoria, autor, busca e tags filtram `isDraft = false`. Um estado nulo tampouco é tratado como publicado nas rotas individuais.

## Otimizações

- Compressão HTTP habilitada para JSON e texto a partir de 1 KB.
- Carregamento de autor junto à consulta de artigos, evitando uma consulta por autor em cada cartão.
- Tags carregadas em lotes e copiadas para o DTO ainda dentro da transação.
- Respostas resumidas removem o corpo extenso das matérias dos cartões.
- O carregamento público de uma matéria não aguarda gravações de visualizações.

## Implantação e validação

1. Definir no Render um segredo JWT forte próprio em `APP_SECURITY_JWT_SECRET`, além da configuração da API key já utilizada (`app.ai.api-key`, por exemplo via `APP_AI_API_KEY`). Não utilizar os valores de desenvolvimento presentes no repositório.
2. Implantar este backend antes do frontend que chama `/admin/{id}/preview` e `/{id}/publish`.
3. Implantar o frontend e o bot compatíveis. A notificação deve apontar para a rota privada de revisão do frontend, sem JWT nem API key na URL. O frontend conduz ao login e volta à revisão.
4. Verificar em ambiente de homologação: acesso anônimo negado à prévia, revisão com JWT, edição e publicação com checklist, e artigo aparecendo nas listas públicas após a aprovação.

A mudança de `publishedAt` é de comportamento da aplicação: a coluna existente continua sendo usada, sem renomeação nem migração de dados históricos. As configurações de teste substituem explicitamente o datasource por H2 em memória e desabilitam bootstrap de administrador; não usam o banco de produção.

Validação local executada com `./mvnw.cmd -q test`: 30 testes passaram, incluindo 11 testes de integração com a cadeia real de segurança, JWT e banco H2. Há cobertura de rotas privadas e públicas, publicação sem alteração do conteúdo, checklist incompleto, cadastro de administrador, chave vazia/incorreta, prioridade do JWT e conta desativada. Não houve commit, push nem implantação nesta revisão.

## Limitações e próximos pontos de revisão

- A prévia usa autenticação editorial, e não um token temporário compartilhável. O acesso dura enquanto a conta e seu JWT forem válidos.
- `summary=true` reduz o tráfego HTTP; a consulta ainda materializa o corpo no banco. Uma projeção SQL específica pode reduzir também a transferência banco/aplicação se o volume justificar.
- Não foi adicionado controle de versão entre abrir a prévia e clicar em publicar. Em edições simultâneas, convém atualizar a prévia antes de confirmar; uma futura revisão pode exigir `updatedAt` esperado ou versão otimista.
- Não há deduplicação nem limitação de frequência por visitante na rota de visualizações. O contador evita perda de incremento concorrente, mas não identifica pessoas únicas.
- O backend não decide relevância/licença das imagens, não verifica fontes externas e não aplica sozinho a janela de sete dias. O bot deve preservar essas validações e a aprovação humana deve confirmar o checklist. Conteúdo HTML precisa continuar sanitizado na renderização do frontend.
- O painel estático legado em `src/main/resources/static` não possui o checklist editorial; sua aprovação já era incompatível com a validação existente. O fluxo integrado de revisão é o painel React do AlpesNews.
- Não foram alterados o plano do Render, a suspensão por inatividade ou dados/credenciais de produção. O efeito final no tempo real depende também do serviço hospedado e deve ser medido após implantação autorizada.
