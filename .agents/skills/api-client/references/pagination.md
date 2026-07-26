# Paginação — Page do Spring

## Tipo único

```ts
// src/lib/api/page.ts
export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number   // página atual, 0-based — igual ao Spring, não reindexe
  size: number
}
```

Nunca redeclare esse shape dentro de um hook de feature. Todo endpoint de
listagem retorna `Page<TipoGeradoDoOpenAPI>`.

## Hook de listagem — padrão

```ts
// src/features/boletos/api/use-boletos-list.ts
import { useQuery } from '@tanstack/react-query'
import type { Page } from '@/lib/api/page'
import type { components } from '@/lib/api/generated/schema'

type Boleto = components['schemas']['BoletoResponse']

export function useBoletosList(page: number, size = 20) {
  return useQuery({
    queryKey: ['boletos', 'list', page, size],
    queryFn: () => fetchBoletos(page, size), // usa client de api-client
  })
}
```

`number` do Spring já é 0-based — se o componente de UI usa paginação 1-based,
converta só na borda (componente de paginação), nunca no hook nem no tipo.

## Zona cinzenta relacionada

Mudar o tamanho de página padrão (`size`) em uma tela específica não é Zona
Cinzenta (decisão de UI). Mudar o *shape* de `Page<T>` em si é — isso afeta
todas as listagens do app.
