# Geração de tipos a partir do OpenAPI

<!-- AJUSTAR: escolha de ferramenta assumida abaixo (openapi-typescript). Não
     confirmado com o usuário — reveja se preferir orval ou outra. -->

## Ferramenta: `openapi-typescript`

Gera só *tipos*, sem runtime, sem client HTTP embutido. Motivo da escolha
sobre alternativas (`orval`, que também gera hooks do TanStack Query):

- Este domínio (RFC 7807, `Page<T>`, dinheiro como string, JWT) precisa de
  parsing e mapeamento de erro específicos que um gerador de hooks genérico
  não acerta sem customização pesada — mais fácil manter o wrapper de fetch
  e os hooks do TanStack Query escritos à mão, e usar o codegen só para o
  contrato de tipos.
- Menos superfície de configuração para o agente errar.

Se o time preferir hooks gerados automaticamente (menos código manual, mais
acoplamento ao gerador), trocar para `orval` é uma decisão de Zona Cinzenta —
pergunte antes de migrar.

## Setup

```bash
npm install -D openapi-typescript
```

```jsonc
// package.json (trecho)
{
  "scripts": {
    "gen:api-types": "openapi-typescript http://localhost:8080/v3/api-docs -o src/lib/api/generated/schema.d.ts"
  }
}
```

<!-- AJUSTAR: URL do endpoint OpenAPI do backend não confirmada — troque pelo
     endereço real (ex.: /v3/api-docs para springdoc-openapi, ou uma URL de
     ambiente fixo/arquivo estático se o time preferir não depender do
     backend rodando localmente para gerar tipos). -->

## Quando regenerar

- Antes de abrir PR que consome endpoint novo ou alterado.
- Rodar `gen:api-types` é passo de CI (falha o build se o diff de
  `schema.d.ts` gerado divergir do commitado) — evita tipo desatualizado
  merged sem querer.
- Mudança de versão *major* do schema, ou campo obrigatório↔opcional: Zona
  Cinzenta, avisar antes de aceitar o novo tipo.

## Uso

```ts
import type { components } from '@/lib/api/generated/schema'

type BoletoResponse = components['schemas']['BoletoResponse']
type CriarBoletoRequest = components['schemas']['CriarBoletoRequest']
```

Nunca declare `interface BoletoResponse { ... }` manualmente — se o tipo
gerado não existir ainda, é sinal de que o schema não foi regenerado.
