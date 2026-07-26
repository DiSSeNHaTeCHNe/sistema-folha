---
name: jpa-performance
description: Uso eficiente de JPA/Hibernate neste backend Java — N+1, fetch, projeção, paginação, batch, transação e índice. Use SEMPRE que a tarefa envolver entidade, repositório, query, relacionamento, listagem, migration, lentidão, timeout, ou criação de endpoint que leia ou grave dados — mesmo que o usuário não mencione performance.
---

# JPA / Hibernate — eficiência

## Diagnóstico primeiro

Antes de otimizar, veja o SQL que está sendo gerado. Em perfil de desenvolvimento:

```yaml
spring.jpa.properties.hibernate.generate_statistics: true
logging.level.org.hibernate.SQL: DEBUG
logging.level.org.hibernate.orm.jdbc.bind: TRACE
```

Não proponha otimização sem saber quantas queries a operação dispara hoje. Se não conseguir medir, diga isso em vez de chutar.

## N+1 — o problema padrão

Sintoma: uma query para a lista, mais uma por item.

```java
// ERRADO — dispara 1 + N
List<Pedido> pedidos = repository.findAll();
pedidos.forEach(p -> p.getItens().size());
```

Correções, em ordem de preferência:

```java
// 1. EntityGraph — declarativo, combina com Pageable
@EntityGraph(attributePaths = {"itens", "cliente"})
List<Pedido> findByStatus(Status status);

// 2. JOIN FETCH — controle fino
@Query("select distinct p from Pedido p join fetch p.itens where p.status = :status")
List<Pedido> buscarComItens(@Param("status") Status status);

// 3. batch fetching — quando o grafo é grande demais para join
@BatchSize(size = 50)   // na coleção
```

**Nunca** resolva N+1 trocando o fetch para `EAGER`. Isso apenas move o problema para todas as outras consultas da entidade.

## Fetch

- Todo `@ManyToOne` e `@OneToOne`: `fetch = LAZY` explícito (o default deles é EAGER).
- `@ManyToMany`: evite. Modele a tabela de junção como entidade própria.
- `open-in-view` desligado:

```yaml
spring.jpa.open-in-view: false
```

Com isso, `LazyInitializationException` aparece em desenvolvimento em vez de virar query silenciosa na renderização. Se aparecer, a correção é buscar o dado na query — não reabrir a sessão.

## Projeção — não carregue o que não vai usar

Listagem quase nunca precisa da entidade inteira.

```java
// interface projection
public interface PedidoResumo {
    Long getId();
    String getNumero();
    BigDecimal getTotal();
}
List<PedidoResumo> findByClienteId(Long id);

// ou DTO direto no JPQL
@Query("select new com.empresa.pedido.dto.PedidoResumo(p.id, p.numero, p.total) from Pedido p")
```

Projeção evita o dirty checking do contexto de persistência e o tráfego de colunas inúteis (BLOB, texto longo).

## Paginação

- Endpoint de listagem **sempre** paginado (`Pageable`). Sem exceção.
- `JOIN FETCH` de coleção + `Pageable` faz o Hibernate paginar em memória (`HHH90003004`). Isso carrega a tabela inteira. Soluções: `@EntityGraph` em vez de `join fetch`, ou duas queries (ids paginados → busca com fetch pelos ids).
- Em tabela grande, `count(*)` do `Page` pode custar mais que a própria página. Use `Slice` quando o total não for necessário na tela.

## Escrita

```java
// batch
spring.jpa.properties.hibernate.jdbc.batch_size: 50
spring.jpa.properties.hibernate.order_inserts: true
spring.jpa.properties.hibernate.order_updates: true
```

- `saveAll` com `flush`/`clear` periódico em lote grande; `save` dentro de laço anula o batch.
- ID: `IDENTITY` desabilita batch de insert no MySQL. Se o volume de escrita importa, use `SEQUENCE` com `allocationSize` (onde o banco suportar) e diga isso ao usuário — é decisão de schema.
- Update em massa: `@Modifying @Query("update ...")` em vez de carregar e salvar N entidades. Lembre de `clearAutomatically = true`.

## Transação

- `@Transactional` na camada de serviço, no menor escopo possível.
- Leitura: `@Transactional(readOnly = true)` — evita dirty checking e permite réplica de leitura.
- **Nunca** chamada HTTP, envio de mensagem ou `sleep` dentro de transação aberta. Conexão presa é o caminho mais rápido para exaurir o pool.
- Chamada interna na mesma classe não passa pelo proxy — `@Transactional` é ignorado. Extraia para outro bean.

## Schema e índice

- Toda alteração por migration Flyway versionada. `ddl-auto: validate` em todo ambiente; `update` nunca.
- Coluna usada em `WHERE`, `JOIN` ou `ORDER BY` de query frequente precisa de índice — crie na mesma migration.
- Índice composto respeita ordem: `(status, data)` serve para filtro por `status` e por `status + data`, não para filtro só por `data`.
- Em tabela grande de produção, `ALTER`/`CREATE INDEX` bloqueia. **Pare e pergunte** antes de gerar essa migration — precisa de janela ou de estratégia online.

## Antes de entregar

- [ ] Sei quantas queries essa operação dispara?
- [ ] Nenhum relacionamento novo em EAGER?
- [ ] Listagem paginada e projetada?
- [ ] Nenhuma chamada externa dentro de `@Transactional`?
- [ ] Coluna de filtro nova tem índice na migration?
