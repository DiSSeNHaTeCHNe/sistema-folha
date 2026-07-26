# Ajuste Benefícios — Design

## Architecture Decision

### Abordagem: Remoção cirúrgica + reorganização de navegação

Remover camada de exposição do legado (controller/service/DTO/page) mantendo apenas entity+repository para fallback. Adicionar tela CRUD sob Cadastros e card de importação na tela existente.

**Impacto mínimo**: Não altera schema de banco, não altera lógica de negócio do módulo Benefícios Mensais, não altera `FolhaTotalizacaoService`.

---

## Component Changes

### 1. Backend — Remoções

| Arquivo | Ação | Justificativa |
|---------|------|---------------|
| `controller/BeneficioController.java` | DELETE | Endpoints `/beneficios` não mais expostos |
| `service/BeneficioService.java` | DELETE | Usado apenas pelo controller removido |
| `dto/BeneficioDTO.java` | DELETE | Usado apenas pelo controller removido |
| `exception/BeneficioNotFoundException.java` | DELETE | Usado apenas pelo service removido |
| `service/ImportacaoBeneficioService.java` | DELETE | CSV parser substituído por xlsx |
| Controller endpoint `POST /importacao/beneficios` | DELETE | Em `ImportacaoController` (se existir) ou inline |

**Verificação de dependência antes de remover**:
- `BeneficioService` é referenciado apenas por `BeneficioController` ✓
- `BeneficioRepository` é usado por `FolhaTotalizacaoService` → **MANTÉM**
- `Beneficio.java` entity → **MANTÉM** (FK ativa no banco)

### 2. Backend — Adições/Ajustes

Nenhuma adição no backend — `TipoBeneficioController` e `ImportacaoBeneficioMensalService` já existem da feature anterior. Apenas garantir que o endpoint de importação `.xlsx` está funcional em `/importacao/beneficios-mensais`.

### 3. Frontend — Remoções

| Arquivo/Código | Ação |
|----------------|------|
| `pages/Beneficios/index.tsx` | DELETE (toda a pasta) |
| `pages/BeneficiosMensais/ImportacaoDialog.tsx` | DELETE |
| `routes/index.tsx` → import e rota de `Beneficios` | REMOVER linhas |
| `Layout/index.tsx` → item "Benefícios" no `menuItems` | REMOVER objeto |
| `pages/BeneficiosMensais/index.tsx` → botão/referência a ImportacaoDialog | REMOVER |

### 4. Frontend — Adições

#### Nova página: `pages/TiposBeneficio/index.tsx`

Tela CRUD no **mesmo padrão de `Cargos/index.tsx`**:
- Lista com DataGrid ou Cards (conforme padrão vigente)
- Colunas: Código, Descrição, Ativo
- Botões: Novo, Editar, Desativar (visíveis para todos, como os demais cadastros)
- Dialog de criação/edição com validação (código unique)
- Feedback via toast

#### Ajuste em `pages/Importacao/index.tsx`

Substituir o card **"Importação de Benefícios"** (CSV, `.csv`) por:

```
┌──────────────────────────────────────────────┐
│ 🎁 Importação de Benefícios Mensais          │
│                                              │
│ Importe planilha Excel (.xlsx) com           │
│ benefícios mensais por competência.          │
│                                              │
│ Competência: [Mês ▼] [Ano ▼]               │
│                                              │
│ [Selecionar Arquivo (.xlsx)]                 │
│ Arquivo selecionado: xxx.xlsx                │
│                                              │
│ [Importar Benefícios Mensais]                │
└──────────────────────────────────────────────┘
```

Campos do card:
- Seletor de mês (1-12)
- Seletor de ano (ano corrente ± 2)
- File input `.xlsx`
- Botão importar
- Tratamento de 409 (dialog substituição, mesmo padrão da folha ADP que já existe)

#### Ajuste em `Layout/index.tsx`

Adicionar ao `cadastroItems`:
```typescript
{ text: 'Tipos de Benefício', icon: <CardGiftcard />, path: '/tipos-beneficio' },
```

#### Ajuste em `routes/index.tsx`

- Remover: `import { Beneficios }` + `<Route path="/beneficios" ... />`
- Adicionar: `import TiposBeneficio` + `<Route path="/tipos-beneficio" ... />`
- Adicionar: `<Route path="/beneficios" element={<Navigate to="/beneficios-mensais" replace />} />`

---

## Service Layer (Frontend)

### `importacaoService.ts` — ajustes

- Remover: `importarBeneficios(file: File)` (CSV legado)
- Adicionar: `importarBeneficiosMensais(file: File, competenciaInicio: string, competenciaFim: string, confirmar: boolean)`

### `tipoBeneficioService.ts` — já existe

Métodos: `listar()`, `criar()`, `atualizar()`, `remover()` — sem alteração.

---

## Migration Impact

**Nenhuma migration de banco necessária.** As tabelas `beneficios`, `tipo_beneficio` e `beneficio_mensal` permanecem inalteradas.

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Outros services importam `BeneficioService` | Compile fail | Verificar dependências antes de deletar (grep por imports) |
| Tela Importação fica complexa com 3+ cards | UX cluttered | Manter apenas 2 cards: Folha ADP + Benefícios Mensais |
| Usuário bookmarkou `/beneficios` | 404 | Redirect automático para `/beneficios-mensais` |
| ImportacaoDialog tinha lógica específica | Perda de funcionalidade | Migrar lógica para o card em `/importacao` |
