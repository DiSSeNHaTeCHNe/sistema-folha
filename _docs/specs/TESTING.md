# Testing Infrastructure

**Analyzed:** 2026-07-29 (post-R3 harness — sync R4 T1)

## Test Frameworks

| Layer | Framework | Notes |
| ----- | --------- | ----- |
| Backend unit | JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) | Services, auth, domain — mocks, no DB |
| Backend integration | Spring Boot Test + Testcontainers | ADP import — Docker-gated `@EnabledIf` |
| Backend coverage | JaCoCo (`jacoco-maven-plugin`) | Report: `backend/target/site/jacoco/jacoco.xml` |
| Frontend unit | Vitest 4 + Testing Library + jsdom | **184+** test cases across page/service tests |
| Frontend HTTP mocks | MSW 2 (`msw/node`) | Isolated per test file via `createAuthMswServer()` — **not** global `setup.ts` |
| Frontend coverage | `@vitest/coverage-v8` | `npm run test:coverage` → `frontend/coverage/lcov.info` |
| E2E | Playwright (`@playwright/test`) | `npm run test:e2e` — login smoke with `page.route()` mock (no backend) |

## Test Counts (baseline post-R3 @ `088a438`)

| Suite | Count | Command |
| ----- | ----- | ------- |
| Backend | **474** (0 failures; 1 skip when Docker absent) | `cd backend && mvn test` |
| Frontend Vitest | **184** (27 files) | `cd frontend && npm test` |

## Test Organization

### Backend

**Location:** `backend/src/test/java/br/com/techne/sistemafolha/`  
**Naming:** `*Test.java` mirroring production packages (`service/`, `auth/application/`, `importacao/application/`, `arch/`)  
**Structure:** Unit tests with `@Mock` repositories/services; integration tests with `@SpringBootTest` + Testcontainers where needed.

**Notable classes:**

- `AuthenticationServiceTest` — auth domain (login, refresh, logout)
- `ImportacaoFolhaAdpIntegrationTest` — ADP XLSX import with PostgreSQL Testcontainer (`@EnabledIf("isDockerAvailable")`)
- `ModularArchitectureTest` — ArchUnit AD-010 boundary checks

### Frontend

**Location:** `frontend/src/**/*.test.tsx`, `frontend/src/services/api.test.ts`  
**Naming:** co-located with source (`Login.test.tsx`, `api.test.ts`)  
**MSW harness:** `frontend/src/test/mswServer.ts` + `frontend/src/test/handlers/authHandlers.ts`  
**Shared API URL:** `frontend/src/lib/apiBaseUrl.ts` (`getApiBaseUrl()`)

Page tests use `vi.mock` for services; HTTP-layer tests (`api.test.ts`) use MSW with explicit lifecycle (`beforeAll`/`afterEach`/`afterAll`).

## Testing Patterns

### Backend unit

**Approach:** Isolate service; mock repositories/ports; assert business rules with `assertThrows` and Portuguese messages.  
**Data:** Entities/DTOs built inline — no `application-test.yml` for unit tests.

### Backend integration (Testcontainers)

**Approach:** `@SpringBootTest` + `@Testcontainers` + `@Transactional` rollback.  
**Docker gate:** `ImportacaoFolhaAdpIntegrationTest` uses `@EnabledIf("isDockerAvailable")` — skipped when Docker daemon is down; full suite stays green.

### Frontend unit (Vitest + MSW)

**Approach:** Testing Library queries by role/label; MSW intercepts axios/fetch in Node.  
**Auth client:** `api.test.ts` exercises 401→refresh→retry, logout on refresh failure, concurrent queue.  
**Page tests:** mock service modules; no global MSW in `setup.ts`.

### E2E (Playwright)

**Live:** `cd frontend && npm run test:e2e` — `e2e/login.spec.ts` mocks `POST */auth/login` and `GET */usuarios/login/:login` via `page.route()`; Vite preview started by `playwright.config.ts` (no backend).  
**Prerequisite:** `npx playwright install chromium` (first run or CI image without browsers).

## Test Execution

| Action | Command |
| ------ | ------- |
| All backend tests | `cd backend && mvn test` |
| Single backend class | `cd backend && mvn test -Dtest=AuthenticationServiceTest` |
| ADP integration (Docker) | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| ArchUnit boundaries | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| JaCoCo report | `cd backend && mvn test` → `target/site/jacoco/index.html` |
| JaCoCo thresholds | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| All frontend tests | `cd frontend && npm test` |
| Single FE test file | `cd frontend && npm test -- api.test` |
| FE coverage | `cd frontend && npm run test:coverage` |
| Lint frontend | `cd frontend && npm run lint` |
| Build frontend | `cd frontend && npm run build` |
| Sonar local scan | `./diversos/scripts/sonar-analyze.sh` |
| E2E | `cd frontend && npm run test:e2e` |
| Gate local (R4 P3) | `./diversos/scripts/gate-r4-local.sh` (optional `--docker`, `--e2e`, `--sonar`) |

**Configuration:** Backend integration tests use `@ActiveProfiles("test")` + Testcontainers dynamic datasource. Frontend Vitest config in `frontend/vite.config.ts`.

## Coverage Targets

| Metric | Target | Enforcement |
| ------ | ------ | ----------- |
| JaCoCo global (backend) | ≥ 75% | `check-jacoco-thresholds.sh` |
| JaCoCo importacao package | ≥ 75% | same script |
| Sonar `new_coverage` (leak period) | ≥ 80% QG floor; **85%** R4 internal meta | `sonar-analyze.sh` |
| Sonar `new_violations` | 0 | Quality Gate |
| Vitest floor | ≥ 184 cases | manual count in gate |

## Test Coverage Matrix

| Code Layer | Required Test Type | Location Pattern | Run Command |
| ---------- | ------------------ | ---------------- | ----------- |
| Backend services | unit (Mockito) | `backend/src/test/java/**/**/*Test.java` | `cd backend && mvn test` |
| Backend auth refresh | unit | `auth/application/AuthenticationServiceTest.java` | `mvn test -Dtest=AuthenticationServiceTest` |
| Backend ADP import | integration (Testcontainers) | `importacao/application/ImportacaoFolhaAdpIntegrationTest.java` | `mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| ArchUnit modular | unit | `arch/ModularArchitectureTest.java` | `mvn test -Dtest=ModularArchitectureTest` |
| FE auth client | unit (Vitest + MSW) | `frontend/src/services/api.test.ts` | `cd frontend && npm test -- api.test` |
| FE pages | unit (Vitest + TL) | `frontend/src/pages/**/*.test.tsx` | `cd frontend && npm test` |
| FE MSW handlers | compile-only | `frontend/src/test/handlers/authHandlers.ts` | via `npm test` |
| E2E login smoke | e2e (Playwright) | `frontend/e2e/*.spec.ts` (T7–T8) | `npm run test:e2e` |
| Gate scripts | none | `diversos/scripts/*.sh` | per script |

## Parallelism Assessment

| Test Type | Parallel-Safe? | Isolation Model | Evidence |
| --------- | -------------- | --------------- | -------- |
| Backend unit (Mockito) | Yes | No shared DB; mocks per test instance | `*ServiceTest.java` |
| Backend integration (Testcontainers) | Yes (per class) | Dedicated PostgreSQL container | `ImportacaoFolhaAdpIntegrationTest` |
| Frontend Vitest | Yes | jsdom + isolated MSW server per file | `createAuthMswServer()` lifecycle |
| Playwright E2E | Yes | Single spec; mocked HTTP via `page.route()` | `e2e/login.spec.ts` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick FE | FE unit/MSW change | `cd frontend && npm test` |
| FE Focused | Single test file | `cd frontend && npm test -- <pattern>` |
| FE Coverage | Before Sonar | `cd frontend && npm run test:coverage` |
| Quick BE | Backend unit | `cd backend && mvn test -Dtest=<ClassTest>` |
| ADP Integration | Docker available | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| Arch | Backend structural | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| JaCoCo | Phase 3 gates | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| Full BE | Phase 3 / Verifier | `cd backend && mvn test` (≥474, 0 failures) |
| Full FE | Phase 3 / Verifier | `cd frontend && npm test` (≥184 cases) |
| Build FE | Playwright / config | `cd frontend && npm run lint && npm run build` |
| Sonar | Checkpoint + final | `./diversos/scripts/sonar-analyze.sh` |
| E2E | Playwright login smoke | `cd frontend && npm run test:e2e` |
| Gate local P3 | Pre-merge | `./diversos/scripts/gate-r4-local.sh` |

> **Brownfield note:** Frontend TARGET (AD-004: `src/features/`, TanStack Query everywhere) is unchanged — current `src/pages/` + `vi.mock` pattern is documented in `frontend/AGENTS.md`. MSW is isolated to HTTP test files, not global setup.
