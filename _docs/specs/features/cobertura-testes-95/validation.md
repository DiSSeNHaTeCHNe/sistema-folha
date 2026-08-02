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
