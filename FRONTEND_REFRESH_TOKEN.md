# Sistema de Refresh Token - Frontend

## Visão Geral

O frontend foi configurado para usar o sistema de refresh token automático, garantindo que os usuários não precisem fazer login novamente quando o token de acesso expira.

## Estrutura Implementada

### 1. **TokenService** (`src/services/tokenService.ts`)
Serviço responsável por gerenciar tokens localmente:

- **Armazenamento**: Salva tokens no localStorage
- **Validação**: Verifica se tokens estão expirados
- **Limpeza**: Remove tokens inválidos
- **Pré-expiração**: Considera token expirado 5 minutos antes para renovação proativa

### 2. **API Service** (`src/services/api.ts`)
Interceptors do Axios para refresh automático:

- **Request Interceptor**: Adiciona token de autorização em todas as requisições
- **Response Interceptor**: Detecta erros 401 e faz refresh automático
- **Fila de Requisições**: Evita múltiplas tentativas de refresh simultâneas
- **Retry Automático**: Refaz requisições originais após refresh

### 3. **AuthContext** (`src/contexts/AuthContext.tsx`)
Contexto de autenticação atualizado:

- **Inicialização**: Verifica tokens válidos na inicialização
- **Auto-logout**: Escuta eventos de logout automático
- **Gerenciamento**: Usa TokenService para todas as operações

### 4. **TokenStatus** (`src/components/TokenStatus/index.tsx`)
Componente de debug para monitorar tokens:

- **Status Visual**: Mostra estado atual dos tokens
- **Tempo Restante**: Exibe tempo até expiração
- **Informações**: Mostra dados dos tokens (parciais)

## Fluxo de Funcionamento

### 1. **Login**
```typescript
// Usuário faz login
const response = await apiLogin(data);

// Tokens são salvos automaticamente
TokenService.setTokens({
  token: response.token,
  refreshToken: response.refreshToken,
  tokenExpiration: response.tokenExpiration,
  refreshExpiration: response.refreshExpiration,
});
```

### 2. **Requisições Automáticas**
```typescript
// Toda requisição automaticamente inclui o token
api.interceptors.request.use((config) => {
  const token = TokenService.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 3. **Refresh Automático**
```typescript
// Ao receber 401, tenta refresh automaticamente
if (error.response?.status === 401 && !originalRequest._retry) {
  // Faz refresh do token
  const refreshResponse = await fetch('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
  
  // Salva novos tokens
  TokenService.setTokens(newTokenData);
  
  // Refaz requisição original
  return api(originalRequest);
}
```

### 4. **Logout Automático**
```typescript
// Se refresh falhar, dispara logout automático
window.dispatchEvent(new CustomEvent('auth:logout'));

// AuthContext escuta e executa logout
window.addEventListener('auth:logout', handleAutoLogout);
```

## Configuração de Tokens

### Tempos de Expiração
- **Access Token**: 24 horas
- **Refresh Token**: 7 dias
- **Pré-expiração**: 5 minutos antes (para renovação proativa)

### Armazenamento
```typescript
// localStorage keys
const TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const TOKEN_EXPIRATION_KEY = 'tokenExpiration';
const REFRESH_EXPIRATION_KEY = 'refreshExpiration';
```

## Tratamento de Erros

### 1. **Token Expirado**
- Tenta refresh automaticamente
- Se refresh funcionar, continua normalmente
- Se refresh falhar, faz logout automático

### 2. **Refresh Token Expirado**
- Limpa todos os tokens
- Redireciona para login
- Mostra mensagem de sessão expirada

### 3. **Erro de Rede**
- Mantém tentativas de refresh
- Falha graciosamente se não conseguir renovar

## Uso no Código

### Hook de Autenticação
```typescript
const { user, isAuthenticated, login, logout } = useAuth();

// Verificar se está autenticado
if (!isAuthenticated) {
  return <Login />;
}
```

### Verificação Manual de Tokens
```typescript
import { TokenService } from '../services/tokenService';

// Verificar se tem tokens válidos
const hasValidTokens = TokenService.hasValidTokens();

// Verificar se precisa renovar
const needsRefresh = TokenService.needsRefresh();

// Obter dados do token
const tokenData = TokenService.getTokenData();
```

## Componente de Debug

Para desenvolvimento, use o componente `TokenStatus`:

```typescript
import { TokenStatus } from '../components/TokenStatus';

// Mostra informações dos tokens em tempo real
<TokenStatus />
```

## Segurança

### Características de Segurança
- **Tokens únicos**: Cada refresh gera novos tokens
- **Revogação**: Tokens antigos são automaticamente revogados
- **Tempo limitado**: Tokens têm expiração definida
- **Armazenamento local**: Tokens ficam apenas no localStorage

### Boas Práticas
- Tokens são limpos no logout
- Refresh é feito apenas quando necessário
- Falhas de refresh resultam em logout automático
- Não exposição de tokens em logs

## Testando o Sistema

### 1. **Login Normal**
- Faça login e observe os tokens no TokenStatus
- Navegue pela aplicação normalmente

### 2. **Refresh Automático**
- Aguarde o token expirar (ou simule alterando a data)
- Faça uma requisição qualquer
- Observe o refresh automático acontecer

### 3. **Logout Automático**
- Simule refresh token expirado
- Observe o logout automático e redirecionamento

## Troubleshooting

### Problemas Comuns

1. **Loop de Refresh**
   - Verificar se o backend está retornando tokens válidos
   - Verificar se as datas estão corretas

2. **Logout Constante**
   - Verificar se o refresh token está sendo salvo corretamente
   - Verificar se o backend aceita o refresh token

3. **Tokens Não Salvos**
   - Verificar se o localStorage está funcionando
   - Verificar se não há erros na resposta do login

### Debug
Use o componente `TokenStatus` para monitorar:
- Estado atual dos tokens
- Tempo restante para expiração
- Valores dos tokens (parciais)

O sistema está totalmente funcional e pronto para uso em produção! 🚀 