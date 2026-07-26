---
name: spring-boot-new-endpoint
description: >-
  Adiciona endpoint REST em projeto Spring Boot: DTO, service, repository JPA,
  controller, exceção de domínio e teste unitário. Detecta convenções do repo
  antes de implementar. Use ao criar recurso CRUD, nova rota ou expandir API existente.
---

# Spring Boot — New Endpoint

Workflow genérico para expor funcionalidade via REST em Spring Boot 3.x + JPA.

---

## Antes de codar — calibrar no projeto

1. Ler `AGENTS.md`, `README` ou doc equivalente do módulo backend.
2. Inspecionar **um endpoint similar já existente** (controller + service + dto + test).
3. Copiar convenções locais: pacotes, nomenclatura, mapper (MapStruct/manual), estilo de DTO (record/class), tratamento de erro, segurança, idioma das mensagens.
4. Identificar build tool e comandos exatos:

| Build | Compilar | Teste unitário | Subir app |
|-------|----------|----------------|-----------|
| Maven | `mvn clean package -DskipTests` | `mvn test -Dtest=ClassName` | `mvn spring-boot:run` |
| Gradle | `./gradlew build -x test` | `./gradlew test --tests ClassName` | `./gradlew bootRun` |

---

## Checklist

```text
- [ ] Calibrar no código existente (passo acima)
- [ ] DTO de request/response
- [ ] Entity + migration Flyway (se recurso novo)
- [ ] Repository
- [ ] Service (regra de negócio + mapeamento)
- [ ] Controller fino
- [ ] Exceção de domínio + @ControllerAdvice (se necessário)
- [ ] Regra de segurança (se projeto usa Spring Security)
- [ ] Teste unitário do service
- [ ] Build + teste passando
```

---

## Camadas (adaptar nomes de pacote ao projeto)

```text
controller/   → HTTP, @Valid, status codes
service/      → regras de negócio, @Transactional em writes
repository/   → Spring Data JPA
model|entity/ → @Entity
dto/          → contrato da API
exception/    → exceções + handler global
```

Fluxo: **Controller → Service → Repository → Entity**. Controller não acessa repository diretamente.

---

## Templates mínimos

### DTO

```java
public record ItemRequest(
    @NotBlank String name
) {}

public record ItemResponse(
    Long id,
    String name
) {}
```

Use validação Jakarta (`jakarta.validation.*`). Adicione anotações OpenAPI se o projeto já usa springdoc.

### Repository

```java
public interface ItemRepository extends JpaRepository<Item, Long> {
    // preferir derived queries; @Query só quando necessário
}
```

### Service

```java
@Service
@RequiredArgsConstructor  // ou constructor explícito — siga o projeto
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public ItemResponse findById(Long id) {
        return itemRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        Item saved = itemRepository.save(toEntity(request));
        return toResponse(saved);
    }

    // toResponse / toEntity: inline, MapStruct ou mapper do projeto
}
```

Regras universais:
- `@Transactional` em **writes**; `readOnly = true` em reads que tocam lazy loading.
- Validação de negócio no service, não no controller.
- Exceções de domínio sobem até o handler global — evite try/catch no controller.

### Controller

```java
@RestController
@RequestMapping("/items")  // respeitar context-path do projeto
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(body));
    }
}
```

Controller **fino**: sem lógica de negócio, sem persistência direta.

### Exceção + handler

```java
public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(Long id) {
        super("Item not found: " + id);
    }
}

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(ItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()));
    }
}
```

Use o formato de erro já adotado no projeto (`ProblemDetail`, DTO customizado, etc.).

### Teste unitário (preferido para lógica de service)

```java
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    @Mock ItemRepository itemRepository;
    @InjectMocks ItemService itemService;

    @Test
    void findById_throwsWhenMissing() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ItemNotFoundException.class, () -> itemService.findById(1L));
    }
}
```

Prefira teste unitário com mocks. Use `@SpringBootTest` / `@WebMvcTest` só se o projeto já adota integração para esse tipo de fluxo.

---

## Segurança

Se o projeto usa Spring Security, registre a rota nova no filter chain / resource server config existente.  
Verifique se matchers incluem ou excluem o `context-path` — isso varia por projeto.

---

## Schema novo

Recurso novo com tabela → usar skill `flyway-migration-writer` **antes** ou **no mesmo PR** que a entity.

---

## Verificação

```bash
# Maven
mvn clean package -DskipTests && mvn test -Dtest=ItemServiceTest

# Gradle
./gradlew build -x test && ./gradlew test --tests ItemServiceTest
```

Confirme boot da aplicação se a mudança toca config, security ou schema.
