# Qualidade — 7 CRITICAL + S8688 Design

**Spec**: `_docs/specs/features/qualidade-criticos-sonar/spec.md`
**Status**: Amended (QUAL-12 emenda B2 — 2026-08-03)

---

## Architecture Overview

Refactor de qualidade, sem mudança de comportamento. Duas frentes independentes:

1. **Tempo testável (S8688):** introduzir um único `Clock` bean e injetá-lo nas **camadas de serviço** (beans Spring); entidades JPA usam forma explícita `Clock.systemDefaultZone()` no `@PrePersist` (Opção A confirmada).
2. **7 CRITICAL:** extrações locais (métodos auxiliares para S3776/S2004; constantes para S1192), preservando o comportamento observável.

```mermaid
graph TD
    Cfg[TimeConfig: @Bean Clock] --> Svc[Serviços/Adapters<br/>now(clock)]
    Svc --> Val[Validação auth<br/>now(clock).isAfter(expiração)]
    Svc --> Test[Testes com Clock.fixed]
    Ent[Entidades JPA @PrePersist<br/>now systemDefaultZone] -.Opção A.-> DB[(timestamps idênticos)]
    Ent -.isExpirado legado<br/>não usado em auth.-> Val
    Crit[7 CRITICAL] --> R1[S3776: extrair helpers CC<=15]
    Crit --> R2[S2004: extrair funções aninhadas <=4]
    Crit --> R3[S1192: extrair constantes]
```

**Emenda B2 (QUAL-12):** Opção B **parcial** — apenas comparação de expiração na validação auth move para serviço com `Clock` injetado. `@PrePersist` e métodos `isExpirado()`/`isExpirada()` nas entidades permanecem (Opção A); auth **deixa de delegar** a eles.

**Approach exploration:** Opção B completa (timestamps em serviço) continua rejeitada. QUAL-12 fecha split-brain sem invadir entidades JPA.

**Conformidade com decisões ativas (STATE.md):** AD-002 (código em `backend/`/`frontend/`), AD-007/AD-008 (monólito modular, pacote `{dominio}.{camada}`). O `Clock` bean vai em `config` (cross-cutting técnico existente) — não cria domínio novo, respeita as fronteiras. Nenhuma AD é superada.

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| Convenção `Clock.systemDefaultZone()` | `dashboard/…DashboardService`, `beneficios/…BeneficioMensalService`, `folha/…FolhaImportacaoAdapter`, `cadastros/domain/FuncionarioRubricaFixa` | Mesmo padrão já usado; entidades seguem-no (Opção A) |
| Pacote `config` | `br.com.techne.sistemafolha.config` | Casa do novo `TimeConfig` (`@Bean Clock`) |
| `slotProps`/helpers React | `pages/BeneficiosMensais`, `components/*` | Padrão de extração de handlers nomeados no FE |
| Testes de serviço existentes | `backend/src/test/java/**/*Test.java` | Floor de estilo; estender com `Clock.fixed` |

### Integration Points

| System | Integration Method |
| --- | --- |
| Spring DI | `Clock` injetado via construtor (`@RequiredArgsConstructor`/construtor manual) nos serviços |
| SonarQube | Verificação final via `sonar-analyze.sh` (CRITICAL=0, S8688=0) |

---

## Components

### TimeConfig (novo)
- **Purpose**: expor um `Clock` único e injetável.
- **Location**: `backend/src/main/java/br/com/techne/sistemafolha/config/TimeConfig.java`
- **Interfaces**: `@Bean Clock clock()` → `Clock.systemDefaultZone()`
- **Dependencies**: nenhuma
- **Reuses**: zona default já usada no projeto (comportamento idêntico)

### Serviços com `Clock` injetado (S8688 + QUAL-12)
- **Purpose**: substituir `.now()` por `.now(clock)`; **validação de expiração auth** usa o mesmo `clock`.
- **Location / alvos**: `security/JwtService.java`, `auth/application/RefreshTokenService.java`, `auth/application/AuthenticationService.java`, `importacao/application/ImportacaoFolhaAdpService.java`, `auth/application/ApiKeyService.java`.
- **QUAL-12 change**:
  - `RefreshTokenService.validarRefreshToken`: checar `revogado` + `LocalDateTime.now(clock).isAfter(dataExpiracao)` inline — **não** chamar `refreshToken.isValido()`.
  - `ApiKeyService.autenticarPorChave`: checar `revogado` + expiração via `clock` antes do hash — **não** chamar `apiKey.isValida()` para expiração.
- **Semântica**: preservar `isAfter` (válido no instante exato de `dataExpiracao`).
- **Interfaces**: construtor passa a receber `Clock clock`.
- **Reuses**: `TimeConfig`.

### Entidades JPA (Opção A)
- **Purpose**: satisfazer S8688 sem injeção.
- **Location / alvos**: `auth/domain/RefreshToken.java`, `cadastros/domain/Funcionario.java`, `organograma/domain/{FuncionarioOrganograma,NoOrganograma,CentroCustoOrganograma}.java`, `beneficios/domain/{BeneficioMensal,TipoBeneficio}.java` (ApiKey já ajustada no quick-task 010).
- **Interfaces**: `@PrePersist`/`@PreUpdate` usam `LocalDateTime.now(Clock.systemDefaultZone())`.
- **Reuses**: convenção existente.

### Refactors dos 7 CRITICAL
- `beneficios/application/ImportacaoBeneficioMensalService.importar` — extrair sub-métodos (parse/validação/persistência) → CC ≤ 15 (S3776).
- `cadastros/application/RubricaService` — constante para "Rubrica não encontrada com ID: " (S1192).
- `importacao/application/ImportacaoFolhaAdpService` — constante para "Filial 0065 TECHNE - EDUCACAO" em `inicializarMapaEmpresas` (S1192).
- `frontend/src/pages/Organograma/index.tsx` — extrair função (l.408, S3776) e desaninhar callbacks (l.465,470, S2004).
- `frontend/src/pages/Usuarios/index.tsx` — desaninhar callback (l.621, S2004).

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| Refactor altera caminho de erro | Testes existentes de erro devem continuar verdes; exceções/mensagens idênticas | Nenhum (comportamento preservado) |
| Borda de expiração no instante exato | Manter semântica `isAfter`/`isBefore` original | Nenhum |

---

## Risks & Concerns

| Concern | Location | Impact | Mitigation |
| --- | --- | --- | --- |
| Entidades JPA seguem não-testáveis no tempo (Opção A) | domínios `@PrePersist` | Timestamps de persistência não usam Clock fixo | QUAL-12 move **validação auth** para serviço; QUAL-10 testa via clock injetado |
| `ImportacaoFolhaAdpService` é frágil (CC 71) e tocado 2× aqui (clock + constante) | `importacao/application/ImportacaoFolhaAdpService.java` | Risco de regressão | Mudanças mínimas e cirúrgicas; suíte de 61 testes do arquivo cobre; **não** refatorar o God method (fora de escopo) |
| Refactor FE pode quebrar render | `Organograma/index.tsx`, `Usuarios/index.tsx` | Regressão visual/comportamental | Testes RTL existentes devem permanecer verdes; extração pura (sem mudar JSX de saída) |

---

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Fonte de tempo | `Clock` bean único (`systemDefaultZone`) injetado em serviços | Testabilidade sem mudar zona; convenção já presente |
| Validação auth expiração | Opção B parcial — `now(clock).isAfter` no serviço | Fecha split-brain; QUAL-10 literal; entidades intactas |
| Entidades JPA | Opção A (explicit zone, sem injeção) | Confirmado; `@PrePersist` inalterado |
| ApiKeyService | Migrar band-aid do quick-task 010 → clock injetado | Consistência + habilita QUAL-10 |

> Decisão de projeto (candidata a AD): "Fonte de tempo do backend = `Clock` bean injetável; entidades JPA usam zona explícita." Registrar como próximo `AD-NNN` no STATE.md **ao concluir o Execute** (não agora, na fase Design apenas proposta).
