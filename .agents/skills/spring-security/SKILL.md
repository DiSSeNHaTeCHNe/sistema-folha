---
name: spring-security
description: Configuração segura de Spring Boot / Spring Security 6 neste backend Java. Use SEMPRE que a tarefa envolver autenticação, autorização, JWT, token, CORS, CSRF, senha, segredo, header de segurança, exposição de endpoint, log de dado sensível, ou criação/alteração de qualquer endpoint público — mesmo que o usuário não use a palavra "segurança".
---

# Segurança — Spring Boot 3 / Spring Security 6

## Regra zero

Mudança em `SecurityFilterChain`, em `permitAll`, ou em qualquer coisa que altere **quem consegue chamar o quê**: descreva o impacto e **peça confirmação antes de aplicar**. Nunca afrouxe uma restrição existente para fazer um teste passar.

## Filter chain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                                  // ver nota abaixo
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .cors(cors -> cors.configurationSource(corsSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())                                   // default deny
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

- **DSL lambda apenas.** `WebSecurityConfigurerAdapter` e os métodos `and()` encadeados foram removidos no Spring Security 6.
- **Termine sempre com `anyRequest().denyAll()` ou `.authenticated()`.** Endpoint novo nasce fechado; abrir é decisão explícita.
- Ordem importa: o primeiro matcher que casa vence. Regra específica antes da genérica.

### CSRF

Desabilitar só é aceitável em API **stateless com token em header** (`Authorization: Bearer`). Se a autenticação usar cookie de sessão em qualquer ponto, CSRF fica **ligado**. Na dúvida sobre o modelo de autenticação do projeto, pergunte.

### CORS

Nunca `allowedOrigins("*")` junto com `allowCredentials(true)` — combinação inválida e insegura. Liste as origens explicitamente, vindas de configuração:

```java
config.setAllowedOrigins(props.origins());     // de @ConfigurationProperties
config.setAllowedMethods(List.of("GET","POST","PUT","DELETE"));
config.setAllowCredentials(true);
```

## Autorização

- Prefira `@PreAuthorize("hasAuthority('PAGAMENTO_ESCREVER')")` na **camada de serviço**, não no controller. Autoridade granular por operação, não papel genérico (`ROLE_ADMIN` para tudo é cheiro de problema).
- Autorização por **dono do recurso** (o usuário só vê os próprios dados) é regra de negócio: implemente no `WHERE` da query, não filtrando a lista em memória depois.
- Nunca confie em `id` de usuário vindo do corpo ou da query string. Use o `Authentication` do contexto.

## Segredos e configuração

- Segredo em `application.yml` versionado: **proibido**. Variável de ambiente ou cofre.
- Se precisar de um segredo novo, adicione o **placeholder** (`${JWT_SECRET}`) e avise o usuário — não gere valor.
- Senha: `BCryptPasswordEncoder` (ou Argon2). Nunca MD5, SHA-1, SHA-256 puro, nem hash caseiro.
- Não logue: token, senha, CPF/CNPJ completo, número de cartão, chave Pix, cabeçalho `Authorization`, corpo de request de autenticação. Mascare (`***1234`) quando o dado for necessário para rastreio.

## Resposta de erro

- `401` para não autenticado, `403` para autenticado sem permissão, `404` quando revelar existência já é vazamento.
- Erro de autenticação é genérico: nunca "usuário não existe" vs. "senha incorreta".
- `ProblemDetail` sem stacktrace, sem mensagem de exceção interna, sem nome de tabela ou classe.

## Entrada

- Bean Validation em todo DTO de entrada (`@Valid` + constraints no record).
- Limite tamanho de payload e de coleção (`@Size`) — proteção contra abuso.
- Query dinâmica: Specification ou parâmetro nomeado. **Nunca** concatenação de string em JPQL/SQL.
- Upload: valide tipo real, tamanho e nome do arquivo; nunca use o nome enviado como caminho no disco.

## Actuator

Exponha apenas `health` e `info` publicamente. `env`, `beans`, `heapdump`, `loggers`, `mappings` ficam autenticados ou desligados.

## Antes de entregar

- [ ] Endpoint novo tem regra de autorização explícita?
- [ ] Nenhum `permitAll` novo sem eu ter perguntado?
- [ ] Nenhum segredo em arquivo versionado?
- [ ] Nenhum dado sensível em log ou em mensagem de erro?
- [ ] A validação de entrada cobre tipo, tamanho e formato?
