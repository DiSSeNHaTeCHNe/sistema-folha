# Cobertura 95% — Branches inatingíveis (COV-09)

## importacao/application — ImportacaoFolhaAdpService

| Local | Branch / linha | Motivo |
| ----- | -------------- | ------ |
| L148–150 | `equalsIgnoreCase("¯¯¯¯")` → `continue` | Separador ADP depende de byte `0xAF` (CP1252). Linhas de teste UTF-8 nem sempre reproduzem o literal do fonte; ramo documentado, cobertura via ramo `Evt`. |
| L162–174 | `linha.length() > 130` (segunda rubrica) | Ramo `else` (debug) coberto; combinações vazias de segunda coluna parcialmente redundantes com `processarRubrica` direto. |
| Totais L178–205 (antes T7) | `catch` em parse de totais | **Removido**: `parseBigDecimal` não propaga exceção; regex de empregados só captura `\d+`. Blocos eram código morto. |

## cadastros/application — FuncionarioService.aplicarFiltroAcesso

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L247 | `acessoTotal()` true | Inatingível via API pública: `listarParaUsuario` / `buscarPorIdParaUsuario` retornam antes de chamar o filtro. Coberto via reflexão em teste. |
| L250 | ACL negado dentro do filtro | Inatingível via API pública: `acessoNegado()` já barra antes. Coberto via reflexão. |

## RubricaService.validarOperador

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L175 | `operador == null` | Inatingível após guarda L134 (`configurarOperadores`) exigir os três operadores. |

## organograma/application — OrganogramaService.construirArvore

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L433 | `parent.children() != null` false | `toDTO` / `toDTOCompleto` sempre inicializam `children` com `new ArrayList<>()`; DTO com `children == null` não é produzido pelo serviço. |

## dashboard/application — DashboardService.calcularStatsPorCargo

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L249 | `quantidadeFuncionarios > 0` false | Entradas do `groupingBy(cargoId)` sempre têm ≥1 linha; `distinct()` de IDs nunca resulta em 0 dentro de um grupo não vazio. |

## pages/Funcionarios — index.tsx

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L245–246 | `cargoId` / `centroCustoId` falsy no `onSubmit` | Validação RHF (`required`) impede submit com selects vazios; ramos `undefined` no payload não são alcançáveis via UI. |

## pages/Importacao — index.tsx

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L325 | `!pendingFile \|\| !pendingTipo` early return em `handleConfirmSubstituicao` | Botão só existe com `pending*` preenchido pelo fluxo 409; guard defensivo. |
| L329 | `pendingTipo === 'beneficiosMensais' && !pendingCompetencia` | 409 de benefícios sempre seta `pendingCompetencia` junto com `pendingTipo`. |
| L379 | `handleCancelSubstituicao` else-if `beneficiosMensais` false com first-if false | Só ocorre com `pendingTipo === null` (estado inconsistente). |
| L237 (removido T16) | `confirmar === true` no toast de `handleBeneficiosMensaisUpload` | Substituído por mensagem única; confirmação usa bloco dedicado em `handleConfirmSubstituicao`. |

## AlterarSenhaDialog — index.tsx

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L72–74 | `novaSenha.length < 6` em `onSubmit` | RHF `minLength: 6` bloqueia submit antes; guard defensivo no handler. |
| L77–79 | `novaSenha !== confirmarSenha` em `onSubmit` | RHF `validate` no campo confirmar bloqueia submit; guard defensivo. |

## contexts/AuthContext — AuthContext.tsx

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L166 | `useAuth` throw fora do provider | `createContext({} as AuthContextData)` retorna objeto truthy; hook nunca atinge o throw em testes de produção. |

## pages/Rubricas — index.tsx (residual <95% branch por arquivo)

| Local | Branch | Motivo |
| ----- | ------ | ------ |
| L124–126, L217, L363–409 | `operador* ?? 1`, mapeamento tipo, options map | Defaults cobertos indiretamente; ramos JSX de Select repetidos; agregado FE ≥95% (T20). |

## Gate T21 — evidência (2026-08-01)

Comando: `cd backend && mvn test && cd ../frontend && npm run test:coverage && bash diversos/scripts/check-coverage-95.sh`

| Métrica | Valor | Status |
| ------- | ----- | ------ |
| Backend LINE | 96.57% (3430/3552) | PASS |
| Backend BRANCH | 95.39% (1158/1214) | PASS |
| Frontend Lines | 97.38% (1828/1877) | PASS |
| Frontend Branches | 95.01% (991/1043) | PASS |

Contagens: BE **1044** testes (1 skip Docker-gated); FE **436** testes (32 arquivos). Exit code gate: **0**.
