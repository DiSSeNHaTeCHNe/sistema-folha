# Sistema de Refresh Token

## Visão Geral

O sistema de refresh token foi implementado para melhorar a segurança da autenticação, permitindo que os usuários renovem seus tokens de acesso sem precisar fazer login novamente.

## Características

- **Access Token**: JWT com duração de 24 horas
- **Refresh Token**: String aleatória segura com duração de 7 dias
- **Armazenamento**: Refresh tokens são armazenados no banco de dados
- **Limpeza Automática**: Tokens expirados são removidos automaticamente a cada 6 horas
- **Revogação**: Tokens podem ser revogados no logout

## Endpoints

### 1. Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "login": "admin",
  "senha": "senha123"
}
```

**Resposta:**
```json
{
  "login": "admin",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "abc123def456ghi789jkl012mno345pqr678",
  "tokenExpiration": "2025-07-11T19:46:48",
  "refreshExpiration": "2025-07-17T19:46:48"
}
```

### 2. Refresh Token
```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "abc123def456ghi789jkl012mno345pqr678"
}
```

**Resposta:**
```json
{
  "login": "admin",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "xyz789uvw456rst123opq890lmn567hij234",
  "tokenExpiration": "2025-07-11T20:46:48",
  "refreshExpiration": "2025-07-17T20:46:48"
}
```

### 3. Logout
```bash
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "abc123def456ghi789jkl012mno345pqr678"
}
```

**Resposta:** HTTP 200 OK

## Fluxo de Uso

1. **Login**: O usuário faz login e recebe um access token e refresh token
2. **Requisições**: Usa o access token no header `Authorization: Bearer <token>`
3. **Refresh**: Quando o access token expira, usa o refresh token para obter um novo
4. **Logout**: Revoga o refresh token ao fazer logout

## Configuração

No `application.yml`:

```yaml
jwt:
  secret: sua_chave_secreta_aqui
  expiration: 86400000 # 24 horas em milissegundos
  refresh:
    expiration: 604800000 # 7 dias em milissegundos
```

## Segurança

- Refresh tokens são únicos e criptograficamente seguros
- Tokens antigos são automaticamente revogados ao gerar novos
- Limpeza automática remove tokens expirados
- Tokens podem ser revogados manualmente no logout

## Modelo de Dados

### Tabela refresh_tokens
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    data_expiracao TIMESTAMP NOT NULL,
    revogado BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Tratamento de Erros

- **Token não encontrado**: HTTP 400 - "Refresh token não encontrado"
- **Token expirado**: HTTP 400 - "Refresh token inválido ou expirado"
- **Token revogado**: HTTP 400 - "Refresh token inválido ou expirado"
- **Usuário não encontrado**: HTTP 400 - "Usuário não encontrado" 