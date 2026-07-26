# Mapeando erro de Bean Validation para o formulário

## Formato que chega (via skill `api-client`)

```json
{
  "type": "https://api.exemplo.com/problems/validacao",
  "title": "Erro de validação",
  "status": 422,
  "errors": [
    { "field": "valor", "message": "deve ser maior que zero" },
    { "field": "vencimento", "message": "não pode ser data passada" }
  ]
}
```

## Padrão de mapeamento

```ts
import type { ApiError } from '@/lib/api/http-error'

function mapApiErrorToForm<T extends Record<string, unknown>>(
  error: ApiError,
  form: UseFormReturn<T>,
) {
  const fields = form.getValues()
  for (const { field, message } of error.problem.errors ?? []) {
    if (field in fields) {
      form.setError(field as Path<T>, { type: 'server', message })
    } else {
      // backend apontou um campo que o form não conhece — não descarte
      form.setError('root', { type: 'server', message: `${field}: ${message}` })
    }
  }
  if (!error.problem.errors?.length) {
    form.setError('root', { type: 'server', message: error.problem.detail ?? error.problem.title })
  }
}
```

Uso no submit:

```ts
const onSubmit = form.handleSubmit(async (values) => {
  try {
    await criarBoleto(values)
  } catch (e) {
    if (e instanceof ApiError) mapApiErrorToForm(e, form)
    else throw e
  }
})
```

## Por que não confiar só na validação client-side

O Zod cobre o caso feliz de UX (feedback imediato), mas a regra de negócio
final é do backend (ex.: regra que depende de outro registro no banco). O
mapeamento acima é o que fecha esse gap sem duplicar regra de negócio no
frontend.
