# Playwright — E2E

## O que vira E2E (e o que não vira)

E2E cobre o caminho feliz + 1-2 desvios críticos de uma feature de negócio
completa (ex.: "criar boleto → ver na listagem → cancelar"). Variação de
validação de campo, estado de loading, mensagens de erro específicas —
isso é teste unitário (Vitest + Testing Library), não E2E. Regra prática: se
o teste não precisa de um browser real navegando entre rotas, não é E2E.

## Padrão

```ts
// e2e/boletos/criar-boleto.spec.ts
import { test, expect } from '@playwright/test'

test('cria um boleto e ele aparece na listagem', async ({ page }) => {
  await page.goto('/boletos/novo')

  await page.getByLabel('Valor').fill('150.00')
  await page.getByLabel('Vencimento').fill('2026-08-30')
  await page.getByRole('button', { name: 'Criar boleto' }).click()

  await expect(page.getByRole('status')).toHaveText(/boleto criado/i)
  await page.getByRole('link', { name: 'Ver listagem' }).click()

  await expect(page.getByRole('cell', { name: 'R$ 150,00' })).toBeVisible()
})
```

- Consulta por papel, igual ao teste unitário — mesma regra, ambiente
  diferente.
- E2E roda contra um backend real (ambiente de teste/staging) ou contra o
  mesmo MSW em modo browser (`msw/browser`) — decisão de infraestrutura de
  CI, não decidida aqui.

<!-- AJUSTAR: se E2E vai rodar contra backend real de staging ou contra MSW
     browser mode não foi confirmado com o usuário — depende de como o time
     de backend disponibiliza um ambiente de teste. -->
