# Checklist de acessibilidade — todo componente interativo

- **Semântica antes de ARIA.** `<button>` para ação, `<a>` para navegação,
  `<table>` para dado tabular. ARIA é o último recurso, não o primeiro.
- **Todo campo de formulário tem `<label htmlFor>` associado** — nunca só
  `placeholder` como label.
- **Erro de campo usa `aria-invalid="true"` + `aria-describedby` apontando
  para o elemento da mensagem de erro.** (Ver skill `forms-validation` para
  onde a mensagem de erro nasce.)
- **Foco é gerenciado em toda interação que muda o contexto:** abrir modal →
  foco vai para dentro do modal (primeiro elemento focável ou título); fechar
  → foco volta para quem abriu. Navegar de rota → foco vai para o `<h1>` da
  página nova (ou um live region anuncia a navegação).
- **Tudo operável só por teclado.** Se só funciona com `onClick` de mouse
  (ex.: `<div onClick>`), não passa — use elemento nativo focável ou
  `tabIndex` + `onKeyDown` para Enter/Space.
- **Tabela com ação por linha:** cada ação tem texto acessível que identifica
  a linha (`aria-label="Editar boleto 12345"`, não só `aria-label="Editar"`
  repetido em todas as linhas).
- **Teste por papel é o critério de aceite.** Se `getByRole('button', { name: /editar boleto 12345/i })`
  não encontra o elemento, a acessibilidade está quebrada — não é só o teste
  que está errado.

## Como testar

Ver skill `testing-a11y` para o padrão de teste (`getByRole`/`getByLabelText`,
nunca `data-testid`).
