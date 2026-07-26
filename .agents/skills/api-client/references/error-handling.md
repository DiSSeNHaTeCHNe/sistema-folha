# Erro RFC 7807 e dinheiro — detalhe

## Shape do erro

O backend Spring Boot 3 usa `ProblemDetail` nativo. Toda resposta de erro tem:

```json
{
  "type": "https://api.exemplo.com/problems/boleto-ja-registrado",
  "title": "Boleto já registrado",
  "status": 409,
  "detail": "O boleto 00190.00009 03395.140507 91020.150008 já existe.",
  "instance": "/api/boletos/12345",
  "errors": [
    { "field": "valor", "message": "deve ser maior que zero" }
  ]
}
```

`errors` é a extensão do Bean Validation (RFC 7807 permite campos extras) —
usada em 422/400. Ver `forms-validation` skill para mapear isso em formulário.

## Cliente HTTP — contrato

```ts
// src/lib/api/http-error.ts
export type ProblemDetail = {
  type: string
  title: string
  status: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  constructor(public problem: ProblemDetail) {
    super(problem.title)
  }
}
```

<!-- AJUSTAR: implementação real do fetch wrapper (fetch nativo vs axios/ky) não
     foi decidida — decida no primeiro PR que criar src/lib/api/client.ts e
     documente aqui a escolha. -->

Todo client (`fetch`/wrapper) que receber `!response.ok` deve fazer parse do
body como `ProblemDetail` e lançar `ApiError`. Nunca deixar o `catch` de um
componente inventar mensagem — a mensagem vem de `problem.detail` ou
`problem.title`.

## Dinheiro

Backend serializa `BigDecimal` como string (`"1234.56"`), para não perder
precisão em JSON. Regra:

- Nunca `Number(valor)` para somar, comparar ou multiplicar.
- Use uma lib de decimal (ex.: `decimal.js` — adicionar como dependência é
  Zona Cinzenta, confirme antes) para qualquer aritmética.
- Formatação de exibição (`R$ 1.234,56`) é a única operação permitida direto
  na string, via `Intl.NumberFormat`, e só na borda de UI.

## Datas

`LocalDate` chega como `"2026-07-25"`; instantes chegam com offset
(`"2026-07-25T14:30:00-03:00"`). Não converta para `Date` na camada de dados —
mantenha string até o componente de exibição, que formata com `Intl.DateTimeFormat`.
