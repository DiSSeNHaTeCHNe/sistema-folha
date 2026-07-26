# Ajuste Benefícios — Context

## Decisions Log

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Remover página `/beneficios` e `BeneficioController` | Stub com mock data, nunca funcional, confunde com BeneficiosMensais | 2026-06-20 |
| 2 | Manter `Beneficio.java` e `BeneficioRepository` no código | `FolhaTotalizacaoService` usa como fallback quando não há dados mensais | 2026-06-20 |
| 3 | Cadastro Tipos de Benefício na seção "Cadastros" do menu | Seguir padrão: Cargos, CentrosCusto, Rubricas, LinhasNegocio → Tipos de Benefício | 2026-06-20 |
| 4 | Importação .xlsx centralizada em `/importacao` | Ponto único de entrada de dados; mesmo UX que folha ADP | 2026-06-20 |
| 5 | Remover `ImportacaoDialog.tsx` de BeneficiosMensais | Duplicação; importação fica exclusivamente em `/importacao` | 2026-06-20 |
| 6 | Substituir importação CSV legada pela .xlsx com competência | CSV não grava por competência; modelo antigo é incompatível | 2026-06-20 |

---

## Inventory: O que será REMOVIDO

### Frontend

| Arquivo | Motivo |
|---------|--------|
| `frontend/src/pages/Beneficios/index.tsx` | Página legada com mock data |
| `frontend/src/pages/BeneficiosMensais/ImportacaoDialog.tsx` | Importação vai para `/importacao` |
| Import de `{ Beneficios }` em `routes/index.tsx` | Rota removida |
| Item "Benefícios" no `menuItems` do Layout | Menu removido |

### Backend

| Arquivo | Motivo |
|---------|--------|
| `BeneficioController.java` | Endpoints legados `/beneficios` |
| `BeneficioService.java` | Service exposto pelo controller legado |
| `BeneficioDTO.java` | DTO do controller legado |
| `ImportacaoBeneficioService.java` (CSV parser) | Substituído por `ImportacaoBeneficioMensalService` (.xlsx) |
| Endpoint `POST /importacao/beneficios` | Substituído por `/importacao/beneficios-mensais` |

### NÃO remover (mantém para fallback)

| Arquivo | Motivo |
|---------|--------|
| `Beneficio.java` (entity) | Usado por `FolhaTotalizacaoService` |
| `BeneficioRepository.java` | Usado por `FolhaTotalizacaoService` |
| Tabela `beneficios` (banco) | Dados históricos; sem migration de drop |

---

## Inventory: O que será MOVIDO/ADICIONADO

### Para seção "Cadastros" no menu

| Item | Rota | Ícone sugerido |
|------|------|----------------|
| Tipos de Benefício | `/tipos-beneficio` | `Category` ou `CardGiftcard` |

**Referência de padrão**: Página `Cargos/index.tsx` (CRUD simples com MUI DataGrid/Cards)

### Para tela `/importacao`

| Card | Arquivo aceito | Campos extras |
|------|---------------|---------------|
| Importação de Benefícios Mensais | `.xlsx` | Mês (select), Ano (select) |

**Substitui**: Card "Importação de Benefícios" (CSV) que existe atualmente

---

## Current State Map

```
ANTES (confuso):
┌──────────────────────────────────────────────┐
│ Menu Principal                                │
│   • Benefícios Mensais → /beneficios-mensais │ ← novo módulo (funcional)
│   • Benefícios         → /beneficios         │ ← legado (mock, stub)
│   • Importação         → /importacao         │ ← CSV benefícios + ADP txt
│                                              │
│ Cadastros                                    │
│   • (sem Tipos de Benefício)                 │
└──────────────────────────────────────────────┘

DEPOIS (limpo):
┌──────────────────────────────────────────────┐
│ Menu Principal                                │
│   • Benefícios Mensais → /beneficios-mensais │ ← apenas visualização/consulta
│   • Importação         → /importacao         │ ← xlsx benefícios + ADP txt
│                                              │
│ Cadastros                                    │
│   • Tipos de Benefício → /tipos-beneficio    │ ← CRUD (admin)
│   • Cargos, Centros, Rubricas, ...           │
└──────────────────────────────────────────────┘
```

## Open Questions

Nenhuma — todas as decisões foram confirmadas na sessão de especificação.
