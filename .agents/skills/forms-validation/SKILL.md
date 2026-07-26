---
name: forms-validation
description: React Hook Form + Zod neste projeto, incluindo como mapear o array de erros de Bean Validation que o backend devolve (RFC 7807 + errors[]) para os campos certos do formulário. Use ao criar, editar ou revisar qualquer formulário — mesmo que o usuário peça só "adiciona um campo" ou "valida esse input" sem mencionar React Hook Form, Zod ou validação explicitamente. Também use ao tratar erro 400/422 vindo de um submit.
---

> **Status: TARGET (não obrigatório ainda)**  
> Fonte obrigatória atual: `_docs/specs/CONVENTIONS.md`, `STRUCTURE.md`, `TESTING.md` + código em `frontend/src/pages|services|...`.  
> Só aplique esta skill como obrigação quando o ROADMAP/AD liberar a adequação frontend correspondente.  
> Até lá: use esta skill como referência de destino, não como gate de PR.

## O que eu faço

Garanto que todo formulário deste projeto usa o mesmo padrão de validação
client-side e o mesmo mapeamento de erro server-side — nunca mensagem
genérica tipo "algo deu errado".

## Regras

1. **Schema Zod é a fonte de verdade da validação client-side.** Um schema
   por formulário, em `src/features/<feature>/schemas/`. Tipo do form vem de
   `z.infer<typeof schema>` — nunca duplique como `interface` separada.
2. **`useForm` com `zodResolver`.** Padrão:

   ```ts
   const form = useForm<FormValues>({
     resolver: zodResolver(schema),
     defaultValues,
   })
   ```

3. **Erro de Bean Validation vira `setError` por campo, não mensagem genérica.**
   Ver `references/bean-validation-mapping.md` antes de escrever o
   `onSubmit`/`onError` de qualquer formulário que faz POST/PUT.
4. **Erro de campo que o backend não conhece (não bate com nenhum `field` do
   `errors[]`) vira um erro de formulário geral (`form.setError('root', ...)`),
   nunca é descartado silenciosamente.**
5. Label, `aria-describedby` do erro, e `aria-invalid` são obrigatórios em
   todo campo — ver skill `component-architecture` para o padrão de
   acessibilidade do campo em si.

## Quando ler a referência

| Arquivo | Ler quando |
|---|---|
| `references/bean-validation-mapping.md` | Escrever o handler de erro de um submit que chama a API |
