# Code Conventions

**Analyzed:** 2026-07-25

## Naming Conventions

**Files (backend):**

- PascalCase Java: `FuncionarioService.java`, `BeneficioMensalController.java`
- Flyway: `V1.13__create_beneficio_mensal.sql`

**Files (frontend):**

- Pastas de página/componente PascalCase + `index.tsx`: `pages/Cargos/index.tsx`, `components/MoneyField/index.tsx`
- Services camelCase + sufixo: `cargoService.ts`, `beneficioMensalService.ts`
- Rotas kebab-case: `/folha-pagamento`, `/tipos-beneficio`

**Functions/Methods:**

- Backend: camelCase português de domínio — `cadastrar`, `softDelete`, `calcularTotaisPorFuncionario`
- Frontend services: português — `listarTodos`, `cadastrar`, `atualizar`, `remover` / `criar`
- Testes: snake_case descritivo — `cadastrar_rejeita_cpf_ativo_duplicado`

**Variables:**

- camelCase; IDs `Long` no backend, `number` no frontend
- Competência frequentemente `competenciaInicio` / `competenciaFim` (`LocalDate`)

**Constants:**

- `private static final` UPPER_SNAKE no Java (`SCALE`, `ROUNDING` em `FolhaTotalizacaoService`)
- Strings de role: `ADMIN` → `ROLE_ADMIN` via `Usuario.getAuthorities()`

## Code Organization

**Import/Dependency Declaration (backend):**

- Pacote → imports Java/Jakarta → Spring → projeto; Lombok annotations na classe
- DI: `@RequiredArgsConstructor` + `private final` (evitar `@Autowired` em campo)

**Import (frontend):**

- React/MUI → libs → paths relativos (`../../services/...`)
- Types: `import type { ... }` quando possível (`api.ts`)

**File Structure:**

- Controller: mapeamento HTTP + delegação
- Service: regras + `toDTO`/`fromDTO`
- Entity: campos + `@PrePersist`/`@PreUpdate` quando há auditoria
- Page CRUD: estado lista + dialog + `useForm` + tabela MUI

**Example DTO record:**

```java
// dto/FuncionarioDTO.java
public record FuncionarioDTO(
    Long id,
    @NotBlank(message = "O nome é obrigatório") String nome,
    ...
) {}
```

**Example service object (frontend):**

```ts
// services/cargoService.ts
const cargoService = {
  listarTodos: async (): Promise<Cargo[]> => { ... },
  cadastrar: async (data: CargoFormData): Promise<Cargo> => { ... },
};
export { cargoService };
```

## Type Safety/Documentation

**Approach:**

- Backend: tipagem Java forte; OpenAPI via `@Schema` / `@Tag` / `@Operation`
- Frontend: TypeScript strict; interfaces em `types/index.ts`; DTOs de create/update nomeados `*CreateDTO` / `*FormData`
- Mensagens de validação e erros em **português**

## Error Handling

**Pattern:**

- Exceções de domínio em `exception/` (`*NotFoundException` → 404, `*DuplicadaException` → 409)
- `GlobalExceptionHandler` (`@RestControllerAdvice`) → `ErrorResponse(status, message)`
- Parte das exceções ainda tratada ad hoc no controller (ex.: importação ADP)
- Frontend: `try/catch` + `toast.error` / estado `error` na página; interceptor 401 faz refresh/logout

## Comments/Documentation

**Style:**

- Javadoc em serviços com regras de negócio não óbvias (`OrganogramaAcessoService`)
- Poucos TODOs no código de produção
- README raiz com changelog e setup; `backend/AGENTS.md` / `frontend` README para agentes e devs

## Observed Variations

- Exports de páginas misturam `export default` e named export
- Services misturam `export const` e `export { objeto }`
- `SecurityConfig` mistura matchers com e sem prefixo `/api` (context-path)
- Dependência React Query instalada sem uso; fetching manual com `useEffect`

> **TARGET:** skills FE (`api-client`, `forms-validation`, `component-architecture`, `routing-perf`, `testing-a11y`) são destino, não gate — ver `STRUCTURE.md` (Current vs TARGET).
