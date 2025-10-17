# 🔐 Conhecimento Consolidado - Tela de Login e Autenticação JWT

## 📋 Índice

1. [Visão Geral](#-visão-geral)
2. [Arquitetura e Tecnologias](#-arquitetura-e-tecnologias)
3. [Camada Backend](#-camada-backend)
4. [Camada Frontend](#-camada-frontend)
5. [Fluxo de Autenticação](#-fluxo-de-autenticação)
6. [Exemplos de Código](#-exemplos-de-código)
7. [Padrões e Boas Práticas](#-padrões-e-boas-práticas)
8. [Melhorias Futuras](#-melhorias-futuras)

---

## 🎯 Visão Geral

A tela de **Login** e o sistema de **Autenticação JWT** constituem a base de segurança de todo o sistema. Implementa autenticação robusta com **JWT** (JSON Web Token) e **Refresh Token** para sessões persistentes e seguras.

### Características Principais

- ✅ **Autenticação JWT**: Tokens stateless com assinatura HMAC SHA-256
- ✅ **Refresh Token**: Tokens de longa duração (7 dias) armazenados no banco
- ✅ **Refresh Automático**: Interceptor Axios renova tokens automaticamente
- ✅ **Persistência de Sessão**: Tokens salvos em localStorage
- ✅ **BCrypt**: Validação de senha com hash seguro
- ✅ **UserDetails**: Integração completa com Spring Security
- ✅ **Show/Hide Password**: Toggle de visibilidade
- ✅ **Loading States**: Feedback visual durante login
- ✅ **Logout Seguro**: Revogação de refresh token no servidor
- ✅ **Validação de Expiração**: Verificação em tempo real
- ✅ **Auto Logout**: Redirect automático ao expirar sessão

### Arquitetura de Segurança

```
┌─────────────────────────────────────────────────────────────┐
│                  ARQUITETURA DE AUTENTICAÇÃO                 │
└─────────────────────────────────────────────────────────────┘

[1] Login
     ↓
[2] BCrypt valida senha
     ↓
[3] Gera JWT (24h) + Refresh Token (7d)
     ↓
[4] Salva Refresh Token no banco (revogável)
     ↓
[5] Frontend armazena tokens + expirações
     ↓
[6] Requisições incluem JWT no header
     ↓
[7] JWT expira? → Refresh automático (transparente)
     ↓
[8] Refresh Token expirou? → Logout automático
```

---

## 🏗️ Arquitetura e Tecnologias

### Stack Tecnológica

#### Backend
- **Java 17** com Spring Boot 3.2.3
- **Spring Security** para autenticação
- **JJWT 0.12.x** para geração/validação de JWT
- **BCryptPasswordEncoder** para validação de senha
- **PostgreSQL** para armazenamento de refresh tokens
- **HMAC SHA-256** para assinatura de tokens
- **SecureRandom** para geração de refresh tokens

#### Frontend
- **React 19.1** com TypeScript
- **Material-UI (MUI) v7** para UI
- **Axios** com interceptors
- **React Router DOM v7** para navegação
- **React Context API** para estado global
- **localStorage** para persistência de tokens

### Estrutura de Arquivos

```
📁 Backend
├── controller/
│   └── AuthController.java                (42 linhas)
├── security/
│   ├── AuthenticationService.java         (129 linhas)
│   ├── JwtService.java                    (125 linhas)
│   └── JwtAuthenticationFilter.java       (filtro de requisições)
├── service/
│   └── RefreshTokenService.java           (gerenciamento de tokens)
├── model/
│   ├── Usuario.java                       (86 linhas - UserDetails)
│   └── RefreshToken.java                  (53 linhas)
├── dto/
│   ├── LoginDTO.java                      (6 linhas - Record)
│   ├── TokenDTO.java                      (16 linhas - Record)
│   └── RefreshTokenRequest.java           (8 linhas - Record)
└── repository/
    └── RefreshTokenRepository.java         (queries de tokens)

📁 Frontend
├── pages/
│   └── Login/
│       └── index.tsx                       (133 linhas)
├── contexts/
│   └── AuthContext.tsx                     (136 linhas)
├── services/
│   ├── api.ts                              (166 linhas - interceptors)
│   └── tokenService.ts                     (94 linhas)
└── types/
    └── index.ts                             (interfaces)
```

**Total de Código**: ~1.200 linhas (Backend: ~600 | Frontend: ~600)

---

## 🔧 Camada Backend

### 1. DTOs

#### `LoginDTO.java`
```java
public record LoginDTO(
    String login,
    String senha
) {}
```

#### `TokenDTO.java`
```java
public record TokenDTO(
    String login,
    String token,                    // JWT (24h)
    String refreshToken,             // Refresh Token (7d)
    LocalDateTime tokenExpiration,   // Data de expiração do JWT
    LocalDateTime refreshExpiration  // Data de expiração do Refresh Token
) {}
```

#### `RefreshTokenRequest.java`
```java
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token é obrigatório")
    String refreshToken
) {}
```

---

### 2. Entidade - `RefreshToken.java`

```java
@Data
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;  // ← Gerado com SecureRandom (256 bits)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;
    
    @Column(name = "revogado", nullable = false)
    private Boolean revogado = false;  // ← Permite revogação manual
    
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
    
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(dataExpiracao);
    }
    
    public boolean isRevogado() {
        return revogado != null && revogado;
    }
    
    public boolean isValido() {
        return !isExpirado() && !isRevogado();
    }
}
```

**Características**:
- **Armazenado no banco**: Permite revogação em tempo real
- **Único por token**: Evita reutilização
- **Auditoria**: `dataCriacao` para rastreamento
- **Validação**: Métodos helper para verificar validade

---

### 3. JwtService - Geração e Validação de JWT

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;  // ← Mínimo 256 bits

    @Value("${jwt.expiration}")
    private long jwtExpiration;  // ← 24 horas

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshExpiration;  // ← 7 dias

    private final SecureRandom secureRandom = new SecureRandom();

    // ========== Geração de Token JWT ==========
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())  // ← Login do usuário
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtExpiration)))
                .signWith(getSigningKey())  // ← HMAC SHA-256
                .compact();
    }

    // ========== Geração de Refresh Token ==========
    
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];  // ← 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // ========== Validação ==========
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String login = extractLogin(token);
            return (login.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ========== Extração de Claims ==========
    
    public String extractLogin(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token expirado", e);
        }
        // ... outras exceções
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalStateException("A chave secreta deve ter pelo menos 256 bits");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

---

### 4. AuthenticationService - Lógica de Autenticação

```java
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // ========== Login ==========
    
    @Transactional
    public TokenDTO authenticate(LoginDTO loginDTO) {
        // 1. Buscar usuário
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(loginDTO.login())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        // 2. Validar senha com BCrypt
        if (!passwordEncoder.matches(loginDTO.senha(), usuario.getSenha())) {
            throw new UsernameNotFoundException("Senha incorreta");
        }

        // 3. Gerar JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.login());
        String jwtToken = jwtService.generateToken(userDetails);
        
        // 4. Gerar Refresh Token (salvo no banco)
        RefreshToken refreshToken = refreshTokenService.criarRefreshToken(loginDTO.login());
        
        // 5. Calcular expirações
        LocalDateTime tokenExpiration = LocalDateTime.now()
            .plusSeconds(jwtService.getJwtExpirationTime() / 1000);
        LocalDateTime refreshExpiration = refreshToken.getDataExpiracao();
        
        // 6. Retornar tokens
        return new TokenDTO(
            loginDTO.login(), 
            jwtToken, 
            refreshToken.getToken(),
            tokenExpiration,
            refreshExpiration
        );
    }

    // ========== Refresh Token ==========
    
    @Transactional
    public TokenDTO refreshToken(String refreshTokenString) {
        // 1. Buscar refresh token no banco
        RefreshToken refreshToken = refreshTokenService.buscarPorToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Refresh token não encontrado"));
        
        // 2. Validar refresh token
        if (!refreshTokenService.validarRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token inválido ou expirado");
        }
        
        Usuario usuario = refreshToken.getUsuario();
        
        // 3. Gerar novo JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getLogin());
        String newJwtToken = jwtService.generateToken(userDetails);
        
        // 4. Criar novo refresh token
        RefreshToken newRefreshToken = refreshTokenService.criarRefreshToken(usuario.getLogin());
        
        // 5. Retornar novos tokens
        return new TokenDTO(
            usuario.getLogin(), 
            newJwtToken, 
            newRefreshToken.getToken(),
            LocalDateTime.now().plusSeconds(jwtService.getJwtExpirationTime() / 1000),
            newRefreshToken.getDataExpiracao()
        );
    }

    // ========== Logout ==========
    
    @Transactional
    public void logout(String refreshTokenString) {
        if (refreshTokenString != null && !refreshTokenString.isEmpty()) {
            refreshTokenService.revogarToken(refreshTokenString);
        }
    }
}
```

---

### 5. AuthController - API REST

```java
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "APIs de autenticação")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário")
    public ResponseEntity<TokenDTO> login(@RequestBody @Valid LoginDTO loginDTO) {
        return ResponseEntity.ok(authenticationService.authenticate(loginDTO));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token de acesso")
    public ResponseEntity<TokenDTO> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Realiza logout do usuário")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}
```

---

## 🎨 Camada Frontend

### 1. Componente de Login

```tsx
export function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<LoginRequest>({
    login: '',
    senha: '',
  });
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(formData);
      navigate('/');
    } catch (err) {
      setError('Usuário ou senha inválidos');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
          <Typography component="h1" variant="h5">
            Sistema de Folha
          </Typography>
          
          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            
            <TextField
              required
              fullWidth
              label="Login"
              name="login"
              autoComplete="username"
              autoFocus
              value={formData.login}
              onChange={handleChange}
            />
            
            <TextField
              required
              fullWidth
              label="Senha"
              name="senha"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              value={formData.senha}
              onChange={handleChange}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowPassword(!showPassword)}>
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? 'Entrando...' : 'Entrar'}
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
}
```

**Características**:
- Design limpo e profissional
- Show/hide password
- Loading state durante autenticação
- Alert de erro
- AutoComplete para navegadores

---

### 2. TokenService - Gerenciamento de Tokens

```tsx
export class TokenService {
  // ========== Salvar Tokens ==========
  
  static setTokens(tokenData: TokenData): void {
    localStorage.setItem('token', tokenData.token);
    localStorage.setItem('refreshToken', tokenData.refreshToken);
    localStorage.setItem('tokenExpiration', tokenData.tokenExpiration);
    localStorage.setItem('refreshExpiration', tokenData.refreshExpiration);
  }

  // ========== Verificar Expiração ==========
  
  static isTokenExpired(): boolean {
    const expiration = this.getTokenExpiration();
    if (!expiration) return true;
    
    // Considera expirado se resta menos de 5 minutos
    const now = new Date();
    const fiveMinutes = 5 * 60 * 1000;
    return expiration.getTime() - now.getTime() < fiveMinutes;
  }

  static isRefreshTokenExpired(): boolean {
    const expiration = this.getRefreshExpiration();
    if (!expiration) return true;
    
    const now = new Date();
    return expiration.getTime() < now.getTime();
  }

  // ========== Validar Tokens ==========
  
  static hasValidTokens(): boolean {
    const token = this.getToken();
    const refreshToken = this.getRefreshToken();
    
    if (!token || !refreshToken) return false;
    
    // Se o refresh token está expirado, não temos tokens válidos
    if (this.isRefreshTokenExpired()) return false;
    
    return true;
  }

  static needsRefresh(): boolean {
    return this.hasValidTokens() && this.isTokenExpired();
  }

  // ========== Limpar ==========
  
  static clearTokens(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('tokenExpiration');
    localStorage.removeItem('refreshExpiration');
  }
}
```

---

### 3. Auth Context - Estado Global

```tsx
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Usuario | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    initializeAuth();
    
    // Escutar evento de logout automático do interceptor
    const handleAutoLogout = () => {
      console.log('Auto logout triggered');
      handleLogout();
    };
    
    window.addEventListener('auth:logout', handleAutoLogout);
    
    return () => {
      window.removeEventListener('auth:logout', handleAutoLogout);
    };
  }, []);

  const initializeAuth = async () => {
    // Verificar se temos tokens válidos
    if (!TokenService.hasValidTokens()) {
      clearAuth();
      return;
    }
    
    // Carregar usuário armazenado
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    
    setLoading(false);
  };

  const login = async (data: LoginRequest) => {
    const response = await apiLogin(data);
    
    // Salvar tokens
    TokenService.setTokens({
      token: response.token,
      refreshToken: response.refreshToken,
      tokenExpiration: response.tokenExpiration,
      refreshExpiration: response.refreshExpiration,
    });
    
    // Buscar dados completos do usuário
    const userData = await getUserByLogin(response.login);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData as Usuario);
    navigate('/dashboard');
  };

  const logout = async () => {
    await apiLogout();
    clearAuth();
    navigate('/login');
  };

  const isAuthenticated = user !== null && TokenService.hasValidTokens();

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
}
```

---

### 4. Axios Interceptors - Refresh Automático

```tsx
// ========== Request Interceptor ==========

api.interceptors.request.use(
  (config) => {
    const token = TokenService.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ========== Response Interceptor ==========

let isRefreshing = false;
let failedQueue: Array<{resolve: Function, reject: Function}> = [];

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Se o erro é 401 (token expirado)
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      // Se já estamos fazendo refresh, colocar na fila
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }
      
      isRefreshing = true;
      
      try {
        const refreshToken = TokenService.getRefreshToken();
        
        if (!refreshToken || TokenService.isRefreshTokenExpired()) {
          throw new Error('Refresh token inválido');
        }
        
        // Fazer refresh usando fetch nativo (evita loop)
        const refreshResponse = await fetch(`${api.defaults.baseURL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
        
        if (!refreshResponse.ok) {
          throw new Error('Falha ao renovar token');
        }
        
        const refreshData = await refreshResponse.json();
        
        // Salvar novos tokens
        TokenService.setTokens({
          token: refreshData.token,
          refreshToken: refreshData.refreshToken,
          tokenExpiration: refreshData.tokenExpiration,
          refreshExpiration: refreshData.refreshExpiration,
        });
        
        // Processar fila de requisições pendentes
        processQueue(null, refreshData.token);
        
        // Retry da requisição original
        originalRequest.headers.Authorization = `Bearer ${refreshData.token}`;
        return api(originalRequest);
        
      } catch (refreshError) {
        processQueue(refreshError, null);
        
        // Logout automático
        TokenService.clearTokens();
        window.dispatchEvent(new Event('auth:logout'));
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    
    return Promise.reject(error);
  }
);
```

---

## 🔄 Fluxo de Autenticação

### Fluxo Completo de Login

```
┌──────────────────────────────────────────────────────────────┐
│                    FLUXO DE LOGIN                             │
└──────────────────────────────────────────────────────────────┘

[1] Usuário preenche login e senha
     ↓
[2] POST /auth/login { login: "admin", senha: "123456" }
     ↓
[3] Backend busca usuário no banco
     ↓
[4] BCrypt valida senha: passwordEncoder.matches(senha, hash)
     ↓
[5] Gera JWT com roles:
     - Subject: login do usuário
     - Claims: ["ROLE_ADMIN", "ROLE_GESTOR"]
     - Expiration: now + 24h
     - Signature: HMAC-SHA256
     ↓
[6] Gera Refresh Token:
     - SecureRandom (256 bits)
     - Salva no banco com expiração 7 dias
     ↓
[7] Retorna TokenDTO {
       login: "admin",
       token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
       refreshToken: "abcd1234...",
       tokenExpiration: "2025-10-17T10:30:00",
       refreshExpiration: "2025-10-23T10:30:00"
     }
     ↓
[8] Frontend salva no localStorage:
     - token
     - refreshToken
     - tokenExpiration
     - refreshExpiration
     ↓
[9] Frontend busca dados do usuário
     ↓
[10] Salva usuário no localStorage + Context
     ↓
[11] Redirect para /dashboard
```

### Fluxo de Refresh Automático

```
┌──────────────────────────────────────────────────────────────┐
│                 FLUXO DE REFRESH AUTOMÁTICO                   │
└──────────────────────────────────────────────────────────────┘

[1] Requisição para API (GET /funcionarios)
     ↓
[2] Request Interceptor adiciona: Authorization: Bearer <token>
     ↓
[3] Backend valida JWT
     ↓
[4] JWT expirado? → Retorna 401
     ↓
[5] Response Interceptor detecta 401
     ↓
[6] Verifica se refresh token é válido
     ↓
[7] POST /auth/refresh { refreshToken: "abcd1234..." }
     ↓
[8] Backend valida refresh token no banco
     ↓
[9] Gera novo JWT + novo refresh token
     ↓
[10] Frontend salva novos tokens
     ↓
[11] Retry da requisição original com novo token
     ↓
[12] Usuário nem percebe! (transparente)
```

---

## 💻 Exemplos de Código

### Exemplo 1: Login Bem-Sucedido

**Request:**
```http
POST /auth/login
Content-Type: application/json

{
  "login": "admin",
  "senha": "admin123"
}
```

**Response:**
```json
{
  "login": "admin",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNjk3NDY0ODAwLCJleHAiOjE2OTc1NTEyMDB9.abc123...",
  "refreshToken": "Xa4kL9mN2pQ5rS8tU1vW3xY6zA0bC4dE",
  "tokenExpiration": "2025-10-17T10:30:00",
  "refreshExpiration": "2025-10-23T10:30:00"
}
```

**SQL Executado:**
```sql
-- 1. Buscar usuário
SELECT * FROM usuarios WHERE login = 'admin' AND ativo = true;

-- 2. Validar senha (em memória com BCrypt)

-- 3. Salvar refresh token
INSERT INTO refresh_tokens (token, usuario_id, data_expiracao, revogado, data_criacao) 
VALUES ('Xa4kL9mN2pQ5rS8tU1vW3xY6zA0bC4dE', 1, '2025-10-23 10:30:00', false, NOW());
```

---

### Exemplo 2: Refresh Token

**Request:**
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "Xa4kL9mN2pQ5rS8tU1vW3xY6zA0bC4dE"
}
```

**Response:**
```json
{
  "login": "admin",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNjk3NDY1MDAwLCJleHAiOjE2OTc1NTE0MDB9.xyz789...",
  "refreshToken": "Zb5mL0nO3qR6sT9uV2wX4yZ7aB1cD5eF",
  "tokenExpiration": "2025-10-17T11:00:00",
  "refreshExpiration": "2025-10-24T11:00:00"
}
```

---

### Exemplo 3: Logout

**Request:**
```http
POST /auth/logout
Content-Type: application/json

{
  "refreshToken": "Xa4kL9mN2pQ5rS8tU1vW3xY6zA0bC4dE"
}
```

**SQL Executado:**
```sql
UPDATE refresh_tokens 
SET revogado = true 
WHERE token = 'Xa4kL9mN2pQ5rS8tU1vW3xY6zA0bC4dE';
```

---

## 🎯 Padrões e Boas Práticas

### 1. Segurança de Tokens

```java
// ✅ Chave secreta mínima de 256 bits
@Value("${jwt.secret}")
private String secretKey;

private SecretKey getSigningKey() {
    byte[] keyBytes = secretKey.getBytes();
    if (keyBytes.length < 32) {
        throw new IllegalStateException("A chave secreta deve ter pelo menos 256 bits");
    }
    return Keys.hmacShaKeyFor(keyBytes);
}

// ✅ Refresh token com SecureRandom
public String generateRefreshToken() {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}
```

### 2. Refresh Automático Transparente

```tsx
// ✅ Interceptor com fila de requisições
if (isRefreshing) {
  return new Promise((resolve, reject) => {
    failedQueue.push({ resolve, reject });
  }).then(() => api(originalRequest));
}

// ✅ Usar fetch nativo para evitar loop
const refreshResponse = await fetch(`${api.defaults.baseURL}/auth/refresh`, {
  method: 'POST',
  body: JSON.stringify({ refreshToken }),
});
```

### 3. Validação de Expiração

```tsx
// ✅ Considerar expirado se resta menos de 5 minutos
static isTokenExpired(): boolean {
  const expiration = this.getTokenExpiration();
  if (!expiration) return true;
  
  const now = new Date();
  const fiveMinutes = 5 * 60 * 1000;
  return expiration.getTime() - now.getTime() < fiveMinutes;
}
```

### 4. Logout Automático

```tsx
// ✅ Evento customizado para logout
window.addEventListener('auth:logout', handleAutoLogout);

// ✅ Disparar no interceptor
window.dispatchEvent(new Event('auth:logout'));
```

---

## 🚀 Melhorias Futuras

### 1. Two-Factor Authentication (2FA)

```java
@Service
public class TwoFactorService {
    
    public String generateTOTP(String secret) {
        // Implementar TOTP (Time-based One-Time Password)
    }
    
    public boolean validateTOTP(String secret, String code) {
        // Validar código de 6 dígitos
    }
}
```

### 2. Remember Me

```tsx
<FormControlLabel
  control={<Checkbox value="remember" color="primary" />}
  label="Lembrar-me"
  onChange={(e) => setRememberMe(e.target.checked)}
/>

// Aumentar expiração do refresh token se remember me
const refreshExpiration = rememberMe ? 30 dias : 7 dias;
```

### 3. Histórico de Sessões

```sql
CREATE TABLE usuario_sessoes (
    id SERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id),
    refresh_token_id BIGINT REFERENCES refresh_tokens(id),
    ip_address VARCHAR(45),
    user_agent TEXT,
    data_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_logout TIMESTAMP,
    ativo BOOLEAN DEFAULT true
);
```

### 4. Bloqueio por Tentativas Falhadas

```java
@Service
public class LoginAttemptService {
    
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    
    public void loginFailed(String key) {
        int attempts = attemptsCache.getOrDefault(key, 0);
        attempts++;
        attemptsCache.put(key, attempts);
        
        if (attempts >= 5) {
            // Bloquear por 15 minutos
        }
    }
    
    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }
}
```

---

## 📝 Conclusão

A tela de **Login** e o sistema de **Autenticação JWT** implementam um dos sistemas de autenticação mais robustos possíveis:

✅ **JWT + Refresh Token**: Segurança máxima com usabilidade  
✅ **Refresh Automático**: Transparente para o usuário  
✅ **Revogação**: Refresh tokens podem ser revogados  
✅ **Persistência**: Sessão sobrevive a refresh de página  
✅ **HMAC SHA-256**: Assinatura criptograficamente segura  
✅ **BCrypt**: Validação de senha sem descriptografar  

Esta implementação serve como **referência definitiva** para:
- Autenticação JWT completa
- Sistema de refresh token robusto
- Interceptors com retry automático
- Gerenciamento global de autenticação

**Documento gerado em:** 16 de Outubro de 2025  
**Versão do Sistema:** 1.0  
**Total**: ~1.200 linhas de código (Backend: 600 | Frontend: 600)

