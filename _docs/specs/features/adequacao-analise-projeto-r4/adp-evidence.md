# ADP integration evidence — R4 (T10)

**Date:** 2026-07-29  
**Branch:** `feat/adequacao-analise-projeto`

## Docker precheck

```bash
docker info >/dev/null 2>&1 && echo DOCKER=UP || echo DOCKER=DOWN
# Result: DOCKER=UP (exit 0)
```

## Integration test run

```bash
cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest
# Exit: 0 — Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
```

## Interpretation

- `@EnabledIf("isDockerAvailable")` → Testcontainers `DockerClientFactory.isDockerAvailable()` returned **false** (BadRequest 400 from Docker Desktop socket despite `docker info` OK).
- Test **skipped** — not live PASS; suite remains green.
- **Status: N/A** — Testcontainers unavailable at runtime; not masked as PASS.

_T13 will fold into `validation.md`._
