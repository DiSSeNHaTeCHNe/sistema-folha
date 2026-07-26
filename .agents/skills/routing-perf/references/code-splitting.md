# Code splitting e orçamento de bundle

## Setup de rota (React Router v7)

```tsx
// src/app/router.tsx
import { lazy, Suspense } from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router'

const BoletosListPage = lazy(() => import('@/features/boletos/routes/boletos-list-page'))
const BoletoDetailPage = lazy(() => import('@/features/boletos/routes/boleto-detail-page'))

const router = createBrowserRouter([
  {
    path: '/boletos',
    element: <Suspense fallback={<RouteFallback />}><BoletosListPage /></Suspense>,
  },
  {
    path: '/boletos/:id',
    element: <Suspense fallback={<RouteFallback />}><BoletoDetailPage /></Suspense>,
  },
])
```

`RouteFallback` é um componente de loading acessível (`role="status"`, texto
para leitor de tela) — ver skill `component-architecture` para o padrão de
acessibilidade.

## Medindo o orçamento

- `npm run build` gera relatório de tamanho por chunk (Vite/Rolldown já
  reporta no output do build).
<!-- AJUSTAR: se o time quer um plugin de visualização (ex.: rollup-plugin-visualizer
     equivalente para Rolldown) não foi decidido — adicionar é Zona Cinzenta
     (dependência nova). -->
- 200KB gzip por chunk de rota é o teto antes de investigar. Estourou:
  primeiro suspeite de uma lib pesada importada sem necessidade (ex.: uma
  lib de gráfico inteira para um componente que usa 5% dela) antes de
  aceitar o tamanho.

## Quando NÃO dividir mais

Não crie `lazy` para cada componente pequeno dentro de uma rota já dividida
— isso vira mais requisições de rede sem ganho real. A unidade de split é a
rota, não o componente, a menos que um componente específico dentro da rota
seja comprovadamente pesado (ex.: um editor rich-text, um gráfico grande) e
raramente usado — nesse caso, `lazy` nele também é justificado, com o
comentário do motivo (mesma regra do `memo` no AGENTS.md).
