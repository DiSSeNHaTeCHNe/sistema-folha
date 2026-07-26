# Benefícios Mensais — Context

## Decisions Log

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Formato de importação: apenas .xlsx | Padrão operacional do RH; planilha Excel é o artefato de trabalho | 2026-06-20 |
| 2 | Tipos de benefício: CRUD restrito a ADMIN | Evitar proliferação de tipos inconsistentes; operador só lança valores | 2026-06-20 |
| 3 | Coexistência com tabela `beneficios` legada | Não migrar retroativamente; `FolhaTotalizacaoService` prioriza `beneficio_mensal` | 2026-06-20 |
| 4 | Resumo calculado (não materializado) | Volume esperado < 1k rows/mês; GROUP BY é suficiente | 2026-06-20 |
| 5 | Mesma representação de competência da folha (date range) | Consistência; permite cruzamento direto entre folha e benefícios | 2026-06-20 |
| 6 | Acesso por organograma (mesmo padrão folha) | Requisito explícito do usuário; reusar `OrganogramaAcessoService` | 2026-06-20 |

## Source Artifact

Planilha de referência: `planilha_beneficios_custo_empresa.xlsx`

**Aba "Resumo"** — Estrutura de totalização por tipo:
| Código | Descrição | Total | Qtd. Lançamentos |
|--------|-----------|-------|-------------------|
| SEGUROS | Seguros - Custo Empresa | (soma) | (count) |
| VALE_REFEICAO | Vale Refeição - Custo Empresa | ... | ... |
| VALE_TRANSPORTE | Vale Transporte - Custo Empresa | ... | ... |
| VALE_ALIMENTACAO | Vale Alimentação - Custo Empresa | ... | ... |
| AM_UNIMED_CE | Assistência Médica - Unimed CE - Custo Empresa | ... | ... |
| AM_GNDI_INTERMEDICA | Assistência Médica - GNDI Intermédica - Custo Empresa | ... | ... |
| AM_OMINT | Assistência Médica - Omint - Custo Empresa | ... | ... |
| AM_UNIMED_SALVADOR | Assistência Médica - Unimed Salvador - Custo Empresa | ... | ... |

**Aba "Lancamentos"** — Layout de entrada:
| Col A | Col B | Col C | Col D | Col E |
|-------|-------|-------|-------|-------|
| CPF | Nome | Descrição (dropdown) | Código (auto) | Valor |

## Open Questions

Nenhuma — todas as questões foram resolvidas na fase de especificação.
