# relatorios-executivos-fix1 — Jobs órfãos e geração travada Specification

**Parent:** `_docs/specs/features/relatorios-executivos/` (MVP Execute complete; validation PASS com ressalvas)  
**Related:** REL-01 (async), REL-04–06 (API), REL-24 (PENDENTE UI), parent implicit **State-transition integrity**; evidência operacional Docker 2026-08-04 (3× `PENDENTE` órfãos → UI “Gerando…” + HTTP 429)  
**Complexity:** Medium  
**Spec status:** Draft — Tasks complete; aguardando aprovação antes de Execute

> **Nota:** fix1 fecha lacunas de **ciclo de vida do job** expostas em produção local/Docker. **Não** reimplementa PDFs, hub ou ACL do MVP. Branch de execução: `feat/relatorios-executivos` (commits prefixados `fix1:`).

## Problem Statement

Após o MVP, operadores ficam **presos** na tela de Relatórios: ao abrir, a competência corrente (ex.: agosto) exibe botões **“Gerando…”** indefinidamente; tentativas em outras competências retornam erro genérico no frontend. Evidência: registros `relatorio` com `status=PENDENTE`, sem linha em `relatorio_arquivo`, criados minutos/horas atrás — o worker nunca concluiu ou saiu sem atualizar o status. O limite de **3 jobs PENDENTE por usuário** passa a bloquear **todas** as novas gerações (HTTP 429), amplificando o deadlock. Contribuintes confirmados: timeout axios **10s** no bundle Docker vs **60s** no backend; worker que retorna silenciosamente quando o registro não é encontrado; frontend que impede retry enquanto `PENDENTE`; contagem de pendentes inclui jobs **stale/órfãos**.

## Goals

- [ ] Garantir que **nenhum** job permaneça `PENDENTE` indefinidamente — transição obrigatória para `PROCESSADO` ou `ERRO` dentro de janela bounded
- [ ] Recuperar ou encerrar jobs **órfãos/stale** existentes sem intervenção manual no banco
- [ ] Impedir que jobs stale consumam o **limite de 3 pendentes** por usuário
- [ ] Alinhar timeout frontend ↔ backend e expor erros HTTP **acionáveis** (429, 403, timeout) na UI
- [ ] Permitir **retry** seguro quando geração estiver travada (UI + API)
- [ ] Evidência automatizada (unit/WebMvc/Vitest) para transições de estado e recovery

## Out of Scope

| Feature | Reason |
| --- | --- |
| Redesign PDF, KPIs, ACL scoped PDF, thumbnail REL-25 | MVP; fora do fix1 |
| Purge 12 meses (REL-33 P3) | Lifecycle futuro |
| CSV export (REL-28/29 P2) | Feature separada |
| Tenant-wide list vs filtro por usuário (REL-03) | Comportamento deliberado no MVP; fix1 só garante card usa registro **do usuário logado** para status/ações |
| API Key POST gerar relatório | Continua bloqueado (AD-013) |
| Rebuild completo do modelo async (filas externas, SQS) | Brownfield; recovery in-process suficiente |
| Migration Flyway nova versão de schema | Fix comportamental; sem colunas novas salvo decisão explícita em Design |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| IDs de requisito | Prefixo `FIX1-NN` nesta spec; refinam REL-01, REL-04–06, REL-24 e **State-transition integrity** do parent | Padrão fix2 auth-api-keys | n |
| **Stale threshold** | Job `PENDENTE` é **stale** quando `now - data_criacao > timeoutSegundos + 120s` (grace 2 min sobre `relatorios.geracao.timeout-segundos`, default 60+120=**180s**) | Cobre timeout HTTP + margem worker; configurável via `relatorios.geracao.stale-grace-segundos` | n |
| Recovery de stale | Ao detectar stale: **reenfileirar worker uma vez**; se após nova tentativa ainda stale → `status=ERRO`, `erro="Tempo esgotado na geração"` | Preferir conclusão; ERRO desbloqueia UI e limite 429 | n |
| Quando rodar recovery | (a) **Lazy:** em `listar*`, `gerar*` e no **polling** FE antes de responder; (b) worker nunca deixa `PENDENTE` ao sair sem processar | Sem scheduler novo no fix1; lazy é suficiente para MVP operacional | n |
| Worker `findById` null | SHALL marcar registro `ERRO` com mensagem genérica (se ID conhecido) ou log + skip; **never** leave `PENDENTE` | Fecha buraco linhas 47–50 `RelatorioGeracaoWorker` | n |
| Limite 429 | `countByUsuarioIdAndStatus(PENDENTE)` SHALL **excluir** jobs stale **ou** stale são promovidos a ERRO antes da contagem | Evita deadlock por órfãos | n |
| Re-gerar com `PENDENTE` ativo (não stale) | POST SHALL **reenfileirar** worker (já parcialmente suportado no backend); FE SHALL permitir **“Tentar novamente”** quando stale, não bloquear com early-return cego | REL-24 + recovery | n |
| Timeout axios POST | `relatorioService` SHALL usar **65000ms** (≥ backend 60s) | Evidência timeout 10s | n |
| Erros FE | Toast SHALL distinguir: **429** limite, **403** ACL, **timeout** rede, **409** download, genérico fallback | UX acionável | n |
| Card status por competência | Hub SHALL resolver relatório por `(tipo, mes, ano, usuarioId)` do usuário logado — não o primeiro da lista tenant-wide | Evita “Gerando” de outro usuário | n |
| `@Lob` BYTEA vs OID (dev) | Mapear `pdf_bytes` como `byte[]` + `@Column(columnDefinition = "bytea")` ou equivalente; **no** alter para OID em dev | Evita falha silenciosa ao persistir PDF | n |
| Dados órfãos existentes | Recovery lazy trata registros já gravados na primeira listagem/geração pós-deploy | Sem script DML manual obrigatório | n |

**Open questions:** none blocking — defaults acima registrados para confirmação implícita na aprovação da spec.

**Implicit-requirement dimensions (Medium — relevant only):**

| Dimension | Resolution |
| --- | --- |
| Failure / partial-failure | Stale → requeue once → ERRO; worker always terminal state |
| Idempotency / retry | Re-POST mesma tupla reenfileira; retry UI em stale/ERRO |
| Concurrency / ordering | Limite 3 pendentes **ativos** (non-stale) only |
| State-transition integrity | `PENDENTE` → `PROCESSADO` \| `ERRO` mandatory; no infinite `PENDENTE` |
| Observability | Log INFO/WARN em recovery stale, worker skip, requeue (`relatorioId`, login) |
| Remaining dimensions | N/A for this fix scope (auth, external deps, purge) |

---

## User Stories

### P1: Integridade de transição do worker ⭐ MVP

**User Story**: Como operador, quero que todo job iniciado termine em `PROCESSADO` ou `ERRO`, para nunca ver “Gerando…” forever por falha silenciosa do worker.

**Why P1**: Causa raiz dos órfãos — worker retorna sem atualizar status.

**Acceptance Criteria**:

1. (FIX1-01) WHEN `RelatorioGeracaoWorker.processar(relatorioId)` concluir renderização com sucesso THEN registro SHALL ter `status=PROCESSADO`, `dataProcessamento` preenchido e blob em `relatorio_arquivo`
2. (FIX1-02) WHEN renderização ou persistência falhar THEN registro SHALL ter `status=ERRO`, `erro` mensagem genérica truncada (≤500 chars), `dataProcessamento` preenchido
3. (FIX1-03) WHEN `relatorioId` não existir ou registro `ativo=false` **após** commit THEN worker SHALL **NOT** deixar nenhum registro `PENDENTE` referente a esse id; SHALL logar WARN (registro já ausente)
4. (FIX1-04) WHEN worker iniciar processamento THEN SHALL existir log estruturado domain `relatorios` com início; WHEN terminar THEN log de sucesso ou ERRO

**Independent Test**: `RelatorioGeracaoWorkerTest` — sucesso, falha render, id inexistente; assert status terminal.

---

### P1: Detecção e recovery de jobs stale ⭐ MVP

**User Story**: Como operador, quero que jobs travados há muito tempo sejam recuperados ou encerrados automaticamente, para destravar a fila sem acesso ao banco.

**Why P1**: Evidência Docker — 3× PENDENTE >3 min sem PDF.

**Acceptance Criteria**:

1. (FIX1-05) WHEN job `PENDENTE` tiver `now - data_criacao > timeoutSegundos + staleGraceSegundos` AND **sem** blob THEN sistema SHALL tratá-lo como **stale**
2. (FIX1-06) WHEN job stale for detectado em `listarFolha`/`listarBeneficio` ou no início de `gerarFolha`/`gerarBeneficio` THEN sistema SHALL **reenfileirar** worker **uma vez** (flag/metadata `recoveryAttempted` ou equivalente in-process/DB)
3. (FIX1-07) WHEN após reenqueue o job continuar stale na próxima detecção THEN sistema SHALL atualizar para `status=ERRO`, `erro="Tempo esgotado na geração"` (texto exato ou prefixo spec)
4. (FIX1-08) WHEN job stale for promovido a `ERRO` THEN **não** SHALL contar para limite de 3 pendentes

**Independent Test**: `RelatorioGeracaoServiceTest` — insert PENDENTE antigo mock clock → listar → requeue; avançar clock → ERRO.

---

### P1: Limite 429 e re-geração ⭐ MVP

**User Story**: Como operador, quero gerar novos relatórios mesmo após falhas passadas, desde que não haja 3 jobs **realmente** em andamento.

**Why P1**: 429 bloqueou competências novas com 3 órfãos.

**Acceptance Criteria**:

1. (FIX1-09) WHEN usuário tiver **<3** jobs `PENDENTE` **non-stale** THEN POST gerar SHALL retornar **200** ou **202/PENDENTE** conforme fluxo existente — **NOT** 429
2. (FIX1-10) WHEN usuário tiver **≥3** jobs `PENDENTE` non-stale THEN POST SHALL retornar **429** com mensagem indicando aguardar conclusão
3. (FIX1-11) WHEN POST gerar para tupla `(tipo, mes, ano, usuarioId)` com job **stale** THEN sistema SHALL executar recovery (FIX1-06) **antes** de aplicar limite 429
4. (FIX1-12) WHEN POST gerar para tupla com job `PENDENTE` **non-stale** THEN sistema SHALL **reenfileirar** worker (substituir fila anterior) e retornar DTO com `status` atualizado após wait/timeout — paridade REL-01

**Independent Test**: WebMvc + service — 3 stale não disparam 429; 3 active disparam 429; re-POST reenfileira.

---

### P1: Frontend — timeout, erros e retry ⭐ MVP

**User Story**: Como usuário da tela `/relatorios`, quero feedback claro e poder tentar de novo quando a geração travar, sem toast genérico enganoso.

**Why P1**: Timeout 10s e catch genérico mascararam 429 e falhas de rede.

**Acceptance Criteria**:

1. (FIX1-13) WHEN `POST /relatorios/{folha|beneficio}` THEN axios SHALL usar timeout **≥65000ms**
2. (FIX1-14) WHEN API retornar **429** THEN UI SHALL exibir mensagem explícita de limite de gerações simultâneas (não “Erro ao gerar relatório” genérico)
3. (FIX1-15) WHEN API retornar **403** (ACL) THEN UI SHALL exibir mensagem de acesso negado
4. (FIX1-16) WHEN request abortar por timeout cliente THEN UI SHALL exibir mensagem de tempo esgotado e sugerir aguardar/polling (job pode continuar no servidor)
5. (FIX1-17) WHEN card estiver `PENDENTE` **stale** (FE: inferir por tempo desde última resposta ou status ERRO após recovery) THEN SHALL exibir botão **“Tentar novamente”** habilitado — **NOT** “Gerando…” bloqueado forever
6. (FIX1-18) WHEN card estiver `PENDENTE` **non-stale** (< grace window) THEN SHALL manter indicador de progresso e botão desabilitado (REL-24)

**Independent Test**: Vitest `Relatorios.test.tsx` — mock 429/403/timeout messages; stale retry button.

---

### P1: Card usa relatório do usuário logado ⭐ MVP

**User Story**: Como usuário em ambiente multi-usuário, quero ver o status **do meu** relatório na competência selecionada, não o de outro operador.

**Why P1**: Lista tenant-wide + `find()` no primeiro match pode mostrar PENDENTE alheio.

**Acceptance Criteria**:

1. (FIX1-19) WHEN `GET /relatorios/{tipo}` retornar lista THEN backend SHALL incluir identificador do gerador (`usuarioId` ou `login`) em cada DTO **OR** expor endpoint filtrado por usuário — decisão em Design; FE SHALL match por usuário logado
2. (FIX1-20) WHEN hub resolver relatório para competência `(mes, ano)` THEN SHALL usar registro do **usuário autenticado** only para status, totais e ações do card
3. (FIX1-21) WHEN usuário não tiver relatório na competência THEN card SHALL exibir estado “sem relatório” (botão Gerar) — mesmo que exista relatório de outro usuário na lista

**Independent Test**: Vitest com lista multi-usuário mock; card mostra status correto.

---

### P2: Persistência BYTEA confiável em dev ⭐ Should-have

**User Story**: Como desenvolvedor, quero que salvar PDF no PostgreSQL funcione em perfil `dev` sem conflito OID/BYTEA, para o worker não falhar ao persistir.

**Why P2**: Log startup Hibernate `cannot cast bytea to oid`; risco em `persistirArquivo`.

**Acceptance Criteria**:

1. (FIX1-22) WHEN perfil `dev` (`ddl-auto: update`) estiver ativo THEN entity `RelatorioArquivo.pdfBytes` SHALL mapear para coluna **BYTEA** sem DDL alter para OID
2. (FIX1-23) WHEN worker persistir PDF THEN insert em `relatorio_arquivo` SHALL succeed em teste de integração ou unit com repositório mock + schema H2/Postgres compatível

**Independent Test**: Worker test com persist; opcional smoke `@DataJpaTest`.

---

## Edge Cases

- WHEN usuário clicar “Gerar” durante recovery lazy THEN SHALL NOT criar duplicata `(usuarioId, tipo, mes, ano)` — upsert existente
- WHEN recovery reenqueue disparar enquanto worker anterior ainda roda THEN SHALL serializar por `(tipo, mes, ano, usuarioId)` — último comando prevalece (paridade parent re-geração)
- WHEN job passar a `ERRO` por stale THEN download SHALL retornar **409** (paridade REL-05)
- WHEN todos jobs stale forem promovidos a ERRO THEN usuário SHALL poder gerar imediatamente sem 429
- WHEN container reiniciar com jobs PENDENTE non-stale recentes THEN recovery lazy na primeira API call SHALL reenfileirar worker
- WHEN `data_criacao` null (legado) THEN tratar como stale imediato

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FIX1-01 | P1: Worker integridade | Tasks | T2 ✅ |
| FIX1-02 | P1: Worker integridade | Tasks | T2 ✅ |
| FIX1-03 | P1: Worker integridade | Tasks | T2 ✅ |
| FIX1-04 | P1: Worker integridade | Tasks | T2 ✅ |
| FIX1-05 | P1: Stale detection | Tasks | T1 ✅ |
| FIX1-06 | P1: Stale recovery | Tasks | T3 ✅ |
| FIX1-07 | P1: Stale recovery | Tasks | T3 ✅ |
| FIX1-08 | P1: Stale recovery | Tasks | T3 ✅ |
| FIX1-09 | P1: Limite 429 | Tasks | T4 ✅ |
| FIX1-10 | P1: Limite 429 | Tasks | T4 ✅ |
| FIX1-11 | P1: Limite 429 | Tasks | T4 ✅ |
| FIX1-12 | P1: Re-geração | Tasks | T4 ✅ |
| FIX1-13 | P1: FE timeout | Tasks | T5 ✅ |
| FIX1-14 | P1: FE erros | Tasks | T5 ✅ |
| FIX1-15 | P1: FE erros | Tasks | T5 ✅ |
| FIX1-16 | P1: FE erros | Tasks | T5 ✅ |
| FIX1-17 | P1: FE retry | Tasks | T5 ✅ |
| FIX1-18 | P1: FE PENDENTE | Tasks | T5 ✅ |
| FIX1-19 | P1: Card usuário | Tasks | T4 ✅ |
| FIX1-20 | P1: Card usuário | Tasks | T4 ✅ |
| FIX1-21 | P1: Card usuário | Tasks | T4, T5 ✅ |
| FIX1-22 | P2: BYTEA dev | Tasks | T6 ✅ |
| FIX1-23 | P2: BYTEA dev | Tasks | T6 ✅ |

**Parent refinement map:**

| FIX1 | Refines |
| --- | --- |
| FIX1-01…04, 12 | REL-01 |
| FIX1-09…11 | REL-01 edge + parent rate limit |
| FIX1-13…18 | REL-24, REL-26 |
| FIX1-19…21 | REL-03 (card scope) |
| FIX1-05…08 | Parent State-transition integrity |

**Coverage:** 23 total, 23 mapped to tasks (T1–T6), 0 unmapped

---

## Success Criteria

- [ ] Zero registros `PENDENTE` com idade > stale threshold após listar/gerar em ambiente com órfãos reproduzidos
- [ ] Operador destrava geração em nova competência após fix sem DELETE manual em `relatorio`
- [ ] UI não exibe “Gerando…” > stale threshold sem opção de retry ou transição para ERRO
- [ ] POST gerar com axios não aborta antes de 60s em competência média
- [ ] Testes automatizados cobrem stale recovery, 429 com/non-stale, worker terminal states

---

## Auto-Size Assessment

| Attribute | Value |
| --- | --- |
| **Scope** | **Medium** — backend lifecycle + FE error/timeout/retry; sem novo domínio |
| **Design** | **Required** — stale recovery mechanism, DTO usuarioId, BYTEA mapping |
| **Tasks** | **Required** — ~6–8 tasks estimadas |
| **Discuss** | Não — defaults registrados em Assumptions |

**Próximo passo sugerido:** Aprovar tasks → Execute inline T1–T6 (`feat/relatorios-executivos`, prefixo `fix1:`).
