---
name: api-client
description: Regras da camada de acesso a dados deste frontend contra o backend Java/Spring Boot 3 — cliente HTTP, formato de erro RFC 7807, paginação Page do Spring, header JWT, dinheiro como string decimal, datas ISO-8601, e geração de tipos a partir do OpenAPI. Use sempre que for criar ou tocar em um hook de dados, um fetch, um endpoint novo, ou qualquer código que leia response de API — mesmo que o usuário não mencione "api", "http", "backend" ou "openapi" explicitamente. Também use antes de tipar manualmente qualquer DTO: o tipo já deve existir gerado.
---

> **Status: TARGET (não obrigatório ainda)**  
> Fonte obrigatória atual: `_docs/specs/CONVENTIONS.md`, `STRUCTURE.md`, `TESTING.md` + código em `frontend/src/pages|services|...`.  
> Só aplique esta skill como obrigação quando o ROADMAP/AD liberar a adequação frontend correspondente.  
> Até lá: use esta skill como referência de destino, não como gate de PR.

## O que eu faço

Defino como este frontend fala com o backend Java, para que nenhum hook reinvente
o parsing de erro, de paginação, de dinheiro ou de data.

## Regras

1. **Tipos vêm do OpenAPI, nunca escritos à mão.** O schema do backend gera
   `src/lib/api/generated/schema.d.ts` via `npm run gen:api-types`
   (`openapi-typescript`). Ver `references/openapi-codegen.md` antes de criar
   qualquer `interface`/`type` que espelhe um DTO do backend.
2. **Erro é sempre `ProblemDetail` (RFC 7807).** Todo client HTTP lança/retorna
   um erro tipado com `type, title, status, detail, instance`. Ver
   `references/error-handling.md` antes de escrever qualquer `catch` ou
   `onError` de mutation.
3. **Paginação usa `Page<T>` único.** Definido uma vez em `src/lib/api/page.ts`
   (`content, totalElements, totalPages, number, size`). Ver
   `references/pagination.md` antes de criar um hook de listagem.
4. **JWT vai no header `Authorization: Bearer`.** Nunca em cookie ou query
   string. Armazenamento (memória vs. `localStorage`) é decisão de Zona
   Cinzenta — não decida sozinho, pergunte.
5. **Dinheiro é `string` decimal (BigDecimal serializado).** Nunca `Number()`
   para somar/comparar. Use a lib de decimal indicada em
   `references/error-handling.md` (seção "Dinheiro").
6. **Data é ISO-8601 como o backend manda.** Não reformate na camada de dados;
   formate só no componente que exibe.

## Quando ler cada referência

| Arquivo | Ler quando |
|---|---|
| `references/error-handling.md` | Escrever `catch`, `onError`, ou qualquer tela de erro que leia resposta de API |
| `references/pagination.md` | Criar hook de listagem/tabela paginada |
| `references/openapi-codegen.md` | O schema do backend mudou, ou é a primeira vez configurando o build |
