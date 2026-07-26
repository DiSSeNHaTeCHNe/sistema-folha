# MSW — handlers de teste

## Padrão

```ts
// src/features/boletos/api/boletos.handlers.ts (mock só desta feature)
import { http, HttpResponse } from 'msw'
import type { Page } from '@/lib/api/page'
import type { components } from '@/lib/api/generated/schema'

type Boleto = components['schemas']['BoletoResponse']

export const boletosHandlers = [
  http.get('/api/boletos', () => {
    const page: Page<Boleto> = {
      content: [/* ... */],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    }
    return HttpResponse.json(page)
  }),

  http.post('/api/boletos', async ({ request }) => {
    const body = await request.json()
    if (!body.valor || Number(body.valor) <= 0) {
      return HttpResponse.json(
        {
          type: 'https://api.exemplo.com/problems/validacao',
          title: 'Erro de validação',
          status: 422,
          errors: [{ field: 'valor', message: 'deve ser maior que zero' }],
        },
        { status: 422 },
      )
    }
    return HttpResponse.json({ id: '123', ...body }, { status: 201 })
  }),
]
```

## Regra: mock reflete o contrato real

O shape de resposta do handler MSW deve bater com o tipo gerado do OpenAPI
(`components['schemas'][...]`) e com o shape de erro RFC 7807 da skill
`api-client` — nunca um shape "conveniente para o teste passar". Se o mock
precisa de um shape diferente do real para o teste passar, o bug está no
componente, não no mock.

## Setup global

```ts
// src/test/setup.ts
import { setupServer } from 'msw/node'
import { boletosHandlers } from '@/features/boletos/api/boletos.handlers'

export const server = setupServer(...boletosHandlers /* + handlers de outras features */)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

`onUnhandledRequest: 'error'` é proposital: uma chamada de API sem handler
mockado deve falhar o teste, não passar silenciosamente batendo na rede real.
