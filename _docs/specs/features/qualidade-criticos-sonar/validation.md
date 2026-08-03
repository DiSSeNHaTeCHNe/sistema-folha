# Qualidade — 7 CRITICAL + S8688 Validation

## Status atual
**Veredito**: PASS ✅
**Spec vigente**: qualidade-criticos-sonar (emenda B2)
**HEAD**: fe2f993
**Gaps abertos**: none (1 ressalva informativa — boundary `isAfter` no instante exato sem assert dedicado)

---

## Execução: qualidade-criticos-sonar — 2026-08-02 — f961a7b..c9b1317

### Veredito: FAIL ❌

Gates (backend 1046, frontend 444, Sonar CRITICAL=0, S8688=0, QG=OK, new_violations=0, JaCoCo ≥95%) passam. Falha no discrimination sensor: mutante M3 (revert constante RubricaService com mensagem alterada) sobreviveu — testes não detectam regressão de mensagem.

### Evidência por AC (QUAL-01..QUAL-11)

| Req | Criterion | file:line + assertion | Spec outcome | Result |
| --- | --- | --- | --- | --- |
| QUAL-01 | Sonar 0 CRITICAL + S3776 ImportacaoBeneficioMensalService CC≤15 | Sonar API: HIGH/CRITICAL open=0; `ImportacaoBeneficioMensalServiceTest.java:100` — `assertEquals(1, resultado.getTotalProcessados())` (importar verde) | 0 CRITICAL; CC≤15; comportamento idêntico | ✅ PASS (CC inferido via Sonar — ⚠️ sem assert CC no teste) |
| QUAL-02 | S3776 Organograma FE CC≤15 | Sonar: 0 HIGH issues em `Organograma/index.tsx`; `Organograma.test.tsx:149` — `expect(screen.getByRole('heading', { name: /Organograma/i })).toBeInTheDocument()` | CC≤15; render/comportamento idêntico | ✅ PASS (CC via Sonar — ⚠️ spec-precision gap) |
| QUAL-03 | S2004 Organograma aninhamento≤4 | Sonar: 0 HIGH/S2004; `Organograma.test.tsx:186` — `fireEvent.click(screen.getByRole('button', { name: 'Gráfico' }))` (interação pós-refactor) | aninhamento≤4; comportamento idêntico | ✅ PASS (aninhamento via Sonar) |
| QUAL-04 | S2004 Usuarios aninhamento≤4 | Sonar: 0 HIGH/S2004; `Usuarios.test.tsx` — testes RTL verdes (32 files / suite completa) | aninhamento≤4; comportamento idêntico | ✅ PASS (aninhamento via Sonar) |
| QUAL-05 | S1192 RubricaService literal extraído | `RubricaService.java:22` — `RUBRICA_NAO_ENCONTRADA_COM_ID`; `RubricaServiceTest.java:196` — `assertThrows(RubricaNotFoundException.class, () -> rubricaService.buscarPorId(99L))` | sem duplicação ≥3×; mensagem byte-a-byte igual | ⚠️ GAP — constante OK; teste não asserta mensagem (sensor M3 survived) |
| QUAL-06 | S1192 ImportacaoFolhaAdpService literal extraído | `ImportacaoFolhaAdpService.java:46,81-83` — `EMPRESA_FILIAL_0065_TECHNE_EDUCACAO` (3 usos, 0 duplicação literal) | sem duplicação ≥3× do literal | ✅ PASS (grep + código; ⚠️ sem assert direto no teste) |
| QUAL-07 | Clock bean + injeção serviços | `TimeConfig.java:12` — `return Clock.systemDefaultZone()`; `RefreshTokenService.java:31` — `private final Clock clock`; `JwtService.java:40` — `public JwtService(Clock clock)` | Clock em config; injetado em serviços/adapters | ✅ PASS |
| QUAL-08 | 0 `.now()` sem arg backend | `rg '\.now\(\)' backend/src/main/java --glob '*.java'` → 0 matches; Sonar S8688=0 | 0 S8688; 0 bare `.now()` em serviços | ✅ PASS |
| QUAL-09 | Entidades JPA zona explícita | `RefreshToken.java:40,44` — `LocalDateTime.now(Clock.systemDefaultZone())`; demais entidades idem (grep 7 arquivos) | forma explícita; instante gravado idêntico | ✅ PASS (comportamento via convenção Opção A — ⚠️ sem assert instante) |
| QUAL-10 | Testes Clock.fixed expiração | `RefreshTokenServiceTest.java:76` — `assertTrue(expiracao.isAfter(agoraFixo))`; `:79` — `assertTrue(LocalDateTime.now(clockAposExpiracao).isAfter(expiracao))`; `ApiKeyServiceTest.java:83,86` — mesma forma | instante controlado; válido antes / expirado depois | ✅ PASS (asserta timestamps; ⚠️ não chama `validarRefreshToken`/`autenticarPorChave` com Clock fixo) |
| QUAL-11 | Gate QG + cobertura ≥95% | Sonar QG API: status=OK, new_violations=0; `check-coverage-95.sh`: BE LINE 96.53%, BE BRANCH 95.32%, FE Lines 97.52%, FE Branches 95.15% | new_violations=0; QG OK; cobertura ≥95% | ✅ PASS |

### Discrimination sensor

| Mutation | File | Killed? |
| --- | --- | --- |
| M1: flip `plusSeconds` → `minusSeconds` em expiração refresh token | `RefreshTokenService.java:44-45` | ✅ Killed — `RefreshTokenServiceTest.java:76` `assertTrue(expiracao.isAfter(agoraFixo))` falhou |
| M2: `Instant.now(clock)` → `Instant.now(clock).minusSeconds(86400)` | `JwtService.java:62` | ✅ Killed — `JwtServiceTest.java:39` `extractLogin(token)` → Token expirado |
| M3: revert constante — mensagem `"Rubrica ERRADA com ID: "` | `RubricaService.java` (buscarPorId) | ❌ Survived — `RubricaServiceTest.java:196` só verifica tipo da exceção |

**Sensor depth**: lightweight (3 mutations)  
**Result**: 2/3 killed — FAIL ❌

### Gate

- Backend: **1046** run, **1045** passed, **0** failed, **1** skipped (Docker/Testcontainers ADP integration)
- Frontend: **444** passed (32 files), **0** failed; build OK
- Sonar: CRITICAL/HIGH=**0**, S8688=**0**, QG=**OK**, new_violations=**0**, coverage (JaCoCo gate): BE LINE **96.53%**, BE BRANCH **95.32%** (Sonar aggregate coverage **92.4%** — gate local ≥95% passa via `check-coverage-95.sh`)

### Gaps encontrados

1. **(Major — sensor)** `RubricaServiceTest` não asserta mensagem byte-a-byte de `RubricaNotFoundException` — mutante M3 sobreviveu; viola edge case spec (literal em exceção) e enfraquece QUAL-05.
2. **(Minor — spec-precision)** QUAL-01/02/03/04: CC/aninhamento verificados via Sonar, não por assert em teste.
3. **(Minor — spec-precision)** QUAL-10: testes Clock.fixed assertam timestamps de expiração, não retorno explícito válido/expirado de `validarRefreshToken`/`autenticarPorChave` com clock injetado no caminho de validação.

---

## Execução: qualidade-criticos-sonar-fix1 — 2026-08-02 — c9b1317..6f19ead

### Veredito: PASS ✅

Re-verificação pós fix cycle 1 (commit `6f19ead`: assert de mensagem em `RubricaServiceTest`). Sensor M3 morto; QUAL-05 verificado; gate `RubricaServiceTest` verde.

### Evidência fix cycle 1

| Req | Criterion | file:line + assertion | Spec outcome | Result |
| --- | --- | --- | --- | --- |
| QUAL-05 | S1192 RubricaService literal extraído + mensagem preservada | `RubricaService.java:22` — `RUBRICA_NAO_ENCONTRADA_COM_ID`; `RubricaServiceTest.java:198` — `assertEquals("Rubrica não encontrada com ID: 99", ex.getMessage())` | sem duplicação ≥3×; mensagem byte-a-byte igual | ✅ PASS |

### Discrimination sensor (M3 re-run)

| Mutation | File | Killed? |
| --- | --- | --- |
| M3: revert constante — mensagem `"Rubrica ERRADA com ID: "` | `RubricaService.java:22` (scratch mutant → `rub-m3-mutant.java`) | ✅ Killed — `RubricaServiceTest.java:198` `assertEquals` falhou: expected `<Rubrica não encontrada com ID: 99>` but was `<Rubrica ERRADA com ID: 99>` |

**Sensor depth (fix1 scope)**: M3 re-run only (M1/M2 unchanged from prior exec — both killed)  
**Overall sensor (feature)**: 3/3 killed — PASS ✅

### Gate

- Command: `cd backend && mvn test -Dtest=RubricaServiceTest`
- Result: **37** run, **37** passed, **0** failed, **0** skipped

### Gaps encontrados (fix1)

Nenhum gap bloqueante. Gaps informativos de spec-precision herdados da execução anterior (QUAL-01..04 via Sonar; QUAL-10 sem assert de retorno válido/expirado) permanecem como ressalvas menores, não bloqueiam veredito.

---

## Execução: qualidade-criticos-sonar-fix2 — 2026-08-03 — 6f19ead..HEAD

### Veredito: PASS ✅

Fix cycle 2 — QUAL-10: testes `Clock.fixed` passam a assertar retorno válido/expirado dos métodos de serviço (`validarRefreshToken`, `autenticarPorChave`), não só timestamps de criação.

### Evidência QUAL-10

| Criterion | file:line + assertion | Spec outcome | Result |
| --- | --- | --- | --- |
| Clock fixo → válido antes / expirado depois (refresh token) | `RefreshTokenServiceTest.java:76-79` — `assertTrue(service.validarRefreshToken(result))`; `:78-79` — `setDataExpiracao(past)` + `assertFalse(service.validarRefreshToken(result))` | serviço retorna válido/expirado em instante controlado | ✅ PASS |
| Clock fixo → válido antes / expirado depois (API key) | `ApiKeyServiceTest.java:87-90` — `assertTrue(service.autenticarPorChave(...).isPresent())`; `:89-90` — `setDataExpiracao(past)` + `assertTrue(...isEmpty())` | serviço retorna usuário/vazio conforme expiração | ✅ PASS |

**Nota Opção A:** criação usa `Clock` injetado; validação de expiração na entidade usa `Clock.systemDefaultZone()`. Testes fixam `dataExpiracao` relativa ao relógio do sistema para simular expirado — comportamento de produção idêntico.

### Gate

- `cd backend && mvn test -Dtest=RefreshTokenServiceTest,ApiKeyServiceTest`: **45** passed, **0** failed

### Gaps encontrados

Nenhum.

---

## Execução: qualidade-criticos-sonar-b2 — 2026-08-03 — defb9c5..fe2f993

### Veredito: PASS ✅

Emenda B2 (T13–T14): validação de expiração auth usa `Clock` injetado inline (`isAfter`); testes avançam `Clock.fixed` entre instâncias de serviço sem mutar `dataExpiracao`. Gates verdes.

### Evidência QUAL-12 + QUAL-10

| Req | Criterion | file:line + assertion | Spec outcome | Result |
| --- | --- | --- | --- | --- |
| QUAL-12 AC1 | `validarRefreshToken` usa `now(clock).isAfter`, não `isValido()` | `RefreshTokenService.java:90` — `LocalDateTime.now(clock).isAfter(refreshToken.getDataExpiracao())`; grep serviço: 0× `isValido`/`isExpirado` | comparação via Clock injetado | ✅ PASS |
| QUAL-12 AC2 | `autenticarPorChave` usa `now(clock).isAfter`, não `isValida()` | `ApiKeyService.java:121` — `LocalDateTime.now(clock).isAfter(apiKey.getDataExpiracao())`; grep serviço: 0× `isValida`/`isExpirada` | comparação via Clock injetado | ✅ PASS |
| QUAL-12 AC3 | bean default preserva semântica `isAfter` | `TimeConfig.java:12` — `Clock.systemDefaultZone()`; mesma expressão `isAfter` que entidade (`RefreshToken.java:44`) | comportamento idêntico em produção | ✅ PASS (código; ⚠️ sem assert produção) |
| QUAL-12 AC4 | testes clock após TTL sem mutar entidade | `RefreshTokenServiceTest.java:75-79` — `serviceB` clock `base.plusSeconds(86401)` + `assertFalse(serviceB.validarRefreshToken(token))` sem `setDataExpiracao`; `ApiKeyServiceTest.java:89-92` — idem `base.plus(8, DAYS)` + `assertTrue(...isEmpty())` | inválido/vazio sem mutação | ✅ PASS |
| QUAL-10 AC2 | avançar Clock.fixed entre instâncias, sem `setDataExpiracao` | `RefreshTokenServiceTest.java:61-79` — `serviceA`/`serviceB` com clocks distintos; `:73` `assertTrue(serviceA.validarRefreshToken(token))`; `:79` `assertFalse(serviceB.validarRefreshToken(token))`; `ApiKeyServiceTest.java:67-92` — `:87` `assertTrue(...isPresent())`; `:92` `assertTrue(...isEmpty())` | válido antes / expirado depois via clock | ✅ PASS |
| QUAL-10 AC3 | `isAfter` no instante exato → ainda válido | `RefreshTokenService.java:90` — `isAfter` (não `!isBefore`/`>=`); `ApiKeyService.java:121` — idem | válido no instante exato; expirado somente após | ⚠️ Spec-precision — implementação correta; sem assert dedicado no instante exato (`base+86400`) |

### Discrimination sensor

| Mutation | File:line | Description | Killed? |
| --- | --- | --- | --- |
| M1 | `RefreshTokenService.java:90` | `isAfter` → `isBefore` | ✅ Killed — `RefreshTokenServiceTest.java:73` `assertTrue` falhou (expected true, was false) |
| M2 | `ApiKeyService.java:121` | `isAfter` → `isBefore` | ✅ Killed — `ApiKeyServiceTest.java:87` `assertTrue` falhou (expected true, was false) |
| M3 | `RefreshTokenService.java:90` | revert delegação `!refreshToken.isValido()` (entidade usa `systemDefaultZone`) | ✅ Killed — `RefreshTokenServiceTest.java:73` `assertTrue` falhou |

**Sensor depth**: lightweight (3 mutations, scratch `.scratch/cov95-sensor-b2/`)  
**Result**: 3/3 killed — PASS ✅

### Gate

- `cd backend && mvn test -Dtest=RefreshTokenServiceTest,ApiKeyServiceTest`: **45** run, **45** passed, **0** failed
- `cd backend && mvn test`: **1046** run, **1045** passed, **0** failed, **1** skipped (Testcontainers ADP — Docker)

### Gaps encontrados

1. **(Minor — spec-precision)** QUAL-10 AC3 / edge case: semântica `isAfter` no instante exato garantida pelo código (`:90`, `:121`), mas nenhum teste fixa clock em `dataExpiracao` exato e asserta válido — ressalva informativa, não bloqueia veredito.
