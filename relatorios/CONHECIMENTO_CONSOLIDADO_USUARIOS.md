# 👥 Conhecimento Consolidado - Tela de Usuários

## 📋 Índice

1. [Visão Geral](#-visão-geral)
2. [Arquitetura e Tecnologias](#-arquitetura-e-tecnologias)
3. [Camada Backend](#-camada-backend)
4. [Camada Frontend](#-camada-frontend)
5. [Fluxo de Dados](#-fluxo-de-dados)
6. [Exemplos de Código](#-exemplos-de-código)
7. [Padrões e Boas Práticas](#-padrões-e-boas-práticas)
8. [Melhorias Futuras](#-melhorias-futuras)

---

## 🎯 Visão Geral

A tela de **Usuários** é responsável pelo gerenciamento de usuários do sistema, incluindo **autenticação, autorização e criptografia de senhas**. É uma implementação completa que integra **Spring Security** e **BCrypt**.

### Características Principais

- ✅ **CRUD Completo**: Criar, listar, editar e excluir usuários
- ✅ **Autenticação**: Integração com Spring Security + JWT
- ✅ **Criptografia BCrypt**: Senhas criptografadas com BCrypt
- ✅ **Permissões**: Sistema de permissões múltiplas (ADMIN, GESTOR, OPERADOR, etc.)
- ✅ **Vinculação com Funcionário**: Relacionamento opcional @ManyToOne
- ✅ **Senha Opcional na Edição**: Manter senha atual se não informar nova
- ✅ **Validação de Senha**: Confirmação de senha + mínimo 6 caracteres
- ✅ **Show/Hide Password**: Toggle de visibilidade de senha
- ✅ **Filtros Avançados**: Por nome, login e funcionário
- ✅ **Login Único**: Validação de unicidade de login
- ✅ **UserDetails**: Implementa interface do Spring Security
- ✅ **Soft Delete**: Exclusão lógica preservando histórico

### Funcionalidades Especiais

**Segurança**:
- Senha nunca retorna no DTO (`senha = null`)
- BCryptPasswordEncoder com salt automático
- Implementação completa de `UserDetails`
- Permissões convertidas para `GrantedAuthority`

**UX Avançada**:
- Card de filtros com 3 campos
- Chips coloridos por tipo de permissão
- Checkboxes para seleção de permissões
- Ícones de visibilidade para senhas
- Divisores (Divider) para organização do formulário
- Loading states e mensagens amigáveis

---

## 🏗️ Arquitetura e Tecnologias

### Stack Tecnológica

#### Backend
- **Java 17** com Spring Boot 3.2.3
- **Spring Security** para autenticação
- **Spring Data JPA** para persistência
- **BCryptPasswordEncoder** para criptografia
- **PostgreSQL** como banco de dados
- **Lombok** para redução de boilerplate
- **Jakarta Bean Validation** para validações
- **UserDetails** implementado na entidade

#### Frontend
- **React 19.1** com TypeScript
- **Material-UI (MUI) v7** para componentes avançados
- **React Hook Form** com Controller
- **Promise.all** para carregamento paralelo
- **useNotification** hook customizado
- **Axios** para comunicação HTTP

### Estrutura de Arquivos

```
📁 Backend
├── src/main/java/br/com/techne/sistemafolha/
│   ├── controller/
│   │   └── UsuarioController.java           (89 linhas)
│   ├── service/
│   │   └── UsuarioService.java              (166 linhas)
│   ├── repository/
│   │   └── UsuarioRepository.java           (16 linhas)
│   ├── model/
│   │   └── Usuario.java                     (86 linhas - UserDetails)
│   └── dto/
│       └── UsuarioDTO.java                  (28 linhas - Record)

📁 Frontend
├── src/
│   ├── pages/
│   │   └── Usuarios/
│   │       └── index.tsx                     (644 linhas - rico em features)
│   ├── services/
│   │   └── usuarioService.ts                (84 linhas)
│   └── types/
│       └── index.ts                          (interface Usuario)
```

**Total de Código**: ~1.113 linhas (Backend: 385 linhas | Frontend: 728 linhas)

---

## 🔧 Camada Backend

### 1. Entidade JPA - `Usuario.java`

```java
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
public class Usuario implements UserDetails {  // ← Implementa Spring Security

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String login;

    @NotBlank
    private String senha;  // ← Criptografada com BCrypt

    @NotBlank
    @Size(max = 100)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;  // ← Relacionamento opcional

    @ElementCollection(fetch = FetchType.EAGER)  // ← Permissões em tabela separada
    @CollectionTable(name = "usuario_permissoes", 
                     joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "permissao")
    private List<String> permissoes;

    private boolean ativo = true;

    // ==================== UserDetails Methods ====================
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissoes.stream()
                .map(permissao -> new SimpleGrantedAuthority("ROLE_" + permissao))
                .toList();
    }

    @Override
    public String getPassword() { return senha; }

    @Override
    public String getUsername() { return login; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return ativo; }
}
```

**Características Especiais**:

1. **`implements UserDetails`**: Integração completa com Spring Security
2. **`@ElementCollection`**: Permissões em tabela separada (`usuario_permissoes`)
3. **`@Column(unique = true)`**: Login único a nível de banco
4. **`getAuthorities()`**: Converte permissões em `ROLE_*` para Spring Security
5. **`isEnabled()`**: Retorna `ativo` para controle de acesso
6. **Senha Criptografada**: Nunca armazenada em texto plano

---

### 2. DTO - `UsuarioDTO.java`

```java
public record UsuarioDTO(
    Long id,
    String login,
    String senha,  // ← Sempre null ao retornar
    String nome,
    List<String> permissoes,
    Long funcionarioId,
    String funcionarioNome,  // ← Join manual
    String funcionarioCpf    // ← Join manual
) {
    public static UsuarioDTO fromEntity(Usuario usuario) {
        return new UsuarioDTO(
            usuario.getId(),
            usuario.getLogin(),
            null, // ⚠️ Não retornamos a senha no DTO (segurança)
            usuario.getNome(),
            usuario.getPermissoes(),
            usuario.getFuncionario() != null ? usuario.getFuncionario().getId() : null,
            usuario.getFuncionario() != null ? usuario.getFuncionario().getNome() : null,
            usuario.getFuncionario() != null ? usuario.getFuncionario().getCpf() : null
        );
    }
}
```

**Segurança**: Senha sempre `null` ao retornar para o frontend!

---

### 3. Repository - `UsuarioRepository.java`

```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByAtivoTrue();
    Optional<Usuario> findByLoginAndAtivoTrue(String login);
    boolean existsByLoginAndAtivoTrue(String login);
    Optional<Usuario> findByFuncionarioIdAndAtivoTrue(Long funcionarioId);
}
```

---

### 4. Service - `UsuarioService.java`

```java
@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;  // ← BCrypt

    @Transactional
    public UsuarioDTO cadastrar(UsuarioDTO dto) {
        // Validar login único
        if (usuarioRepository.existsByLoginAndAtivoTrue(dto.login())) {
            throw new IllegalArgumentException("Já existe um usuário ativo com este login");
        }

        Usuario usuario = toEntity(dto);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));  // ← Criptografa
        return toDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.isAtivo())
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        // Validar login único (se mudou)
        if (!usuario.getLogin().equals(dto.login()) && 
            usuarioRepository.existsByLoginAndAtivoTrue(dto.login())) {
            throw new IllegalArgumentException("Já existe um usuário ativo com este login");
        }

        usuario.setLogin(dto.login());
        usuario.setNome(dto.nome());
        usuario.setPermissoes(dto.permissoes());
        
        // ⚠️ Atualizar senha SOMENTE se fornecida
        if (dto.senha() != null && !dto.senha().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }
        
        // Atualizar funcionário
        if (dto.funcionarioId() != null) {
            Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
            usuario.setFuncionario(funcionario);
        } else {
            usuario.setFuncionario(null);
        }
        
        return toDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void alterarSenha(Long id, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verificar senha atual
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        // Atualizar senha
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return UsuarioDTO.fromEntity(usuario);  // ← Senha sempre null
    }
}
```

**Pontos Críticos**:

1. **`passwordEncoder.encode()`**: Criptografa senha com BCrypt
2. **`passwordEncoder.matches()`**: Valida senha sem descriptografar
3. **Senha opcional ao editar**: `if (dto.senha() != null && !dto.senha().isEmpty())`
4. **Validação de login único**: Antes de salvar
5. **Relacionamento opcional**: Funcionário pode ser `null`

---

### 5. Controller - `UsuarioController.java`

```java
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "API para gerenciamento de usuários")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/login/{login}")
    public ResponseEntity<UsuarioDTO> buscarPorLogin(@PathVariable String login) {
        return ResponseEntity.ok(usuarioService.buscarPorLogin(login));
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<UsuarioDTO> buscarPorFuncionario(@PathVariable Long funcionarioId) {
        UsuarioDTO usuario = usuarioService.buscarPorFuncionario(funcionarioId);
        return usuario != null ? ResponseEntity.ok(usuario) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> cadastrar(@Valid @RequestBody UsuarioDTO usuario) {
        return ResponseEntity.ok(usuarioService.cadastrar(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuario) {
        return ResponseEntity.ok(usuarioService.atualizar(id, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/alterar-senha")
    public ResponseEntity<Void> alterarSenha(@PathVariable Long id, 
                                             @RequestParam String senhaAtual, 
                                             @RequestParam String novaSenha) {
        usuarioService.alterarSenha(id, senhaAtual, novaSenha);
        return ResponseEntity.ok().build();
    }
}
```

---

## 🎨 Camada Frontend

### Componente Principal (Resumido)

```tsx
export default function Usuarios() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [funcionarios, setFuncionarios] = useState<{id: number, nome: string, cpf: string}[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedUsuario, setSelectedUsuario] = useState<Usuario | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState('');

  // ========== Carregamento Paralelo ==========
  const carregarDados = async () => {
    const [usuariosData, funcionariosData] = await Promise.all([
      usuarioService.listar(),
      usuarioService.listarFuncionarios()
    ]);
    setUsuarios(usuariosData);
    setFuncionarios(funcionariosData);
  };

  // ========== Validação de Senha ==========
  const onSubmit = async (data: UsuarioForm) => {
    // Validar senhas (novo usuário ou senha informada)
    if (!selectedUsuario || data.senha) {
      if (data.senha !== confirmPassword) {
        showNotification('As senhas não coincidem', 'error');
        return;
      }
      if (!data.senha || data.senha.length < 6) {
        showNotification('A senha deve ter pelo menos 6 caracteres', 'error');
        return;
      }
    }

    // Remover senha vazia (edição)
    if (selectedUsuario && !data.senha) {
      delete data.senha;
    }

    // Salvar
    if (selectedUsuario) {
      await usuarioService.atualizar(selectedUsuario.id, data);
    } else {
      await usuarioService.criar(data);
    }
  };
```

### Features Especiais

#### 1. **Chips de Permissões Coloridas**

```tsx
const getPermissaoColor = (permissao: string) => {
  const colors: Record<string, ChipColor> = {
    'ADMIN': 'error',
    'GESTOR': 'warning',
    'OPERADOR': 'primary',
    'CONSULTA': 'info',
    'FOLHA_PAGAMENTO': 'success',
    'BENEFICIOS': 'secondary',
  };
  return colors[permissao] || 'default';
};

// Renderização
<Chip
  label={permissao}
  size="small"
  color={getPermissaoColor(permissao)}
/>
```

#### 2. **Show/Hide Password**

```tsx
<TextField
  type={showPassword ? 'text' : 'password'}
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
```

#### 3. **Checkboxes de Permissões**

```tsx
<Controller
  name="permissoes"
  control={control}
  rules={{ required: 'Selecione pelo menos uma permissão' }}
  render={({ field }) => (
    <FormGroup>
      {permissoesDisponiveis.map((permissao) => (
        <FormControlLabel
          control={
            <Checkbox
              checked={field.value.includes(permissao)}
              onChange={(e) => {
                if (e.target.checked) {
                  field.onChange([...field.value, permissao]);
                } else {
                  field.onChange(field.value.filter((p) => p !== permissao));
                }
              }}
            />
          }
          label={permissao}
        />
      ))}
    </FormGroup>
  )}
/>
```

#### 4. **Card de Filtros**

```tsx
<Card>
  <CardContent>
    <form onSubmit={handleFilterSubmit(handleFilter)}>
      <Box display="flex" gap={2}>
        <TextField label="Nome" />
        <TextField label="Login" />
        <Select label="Funcionário">
          <MenuItem value="">Todos</MenuItem>
          {funcionarios.map(f => <MenuItem value={f.id}>{f.nome}</MenuItem>)}
        </Select>
        <Button type="submit">Filtrar</Button>
        <Button onClick={handleClearFilter}>Limpar</Button>
      </Box>
    </form>
  </CardContent>
</Card>
```

---

## 🔄 Fluxo de Dados

### Fluxo de Criação de Usuário

```
[1] Usuário preenche formulário
     ↓
[2] Validação Frontend
     - Senha === Confirmar Senha
     - Senha >= 6 caracteres
     - Permissões > 0
     ↓
[3] POST /usuarios
     Body: {
       login: "joao.silva",
       nome: "João Silva",
       senha: "123456",
       funcionarioId: 10,
       permissoes: ["ADMIN", "GESTOR"]
     }
     ↓
[4] Backend: UsuarioService.cadastrar()
     - Valida login único
     - Criptografa senha: BCrypt
     - Associa funcionário
     - Salva usuário
     ↓
[5] INSERT INTO usuarios (login, nome, senha, funcionario_id, ativo)
    INSERT INTO usuario_permissoes (usuario_id, permissao) VALUES (1, 'ADMIN'), (1, 'GESTOR')
     ↓
[6] Retorna UsuarioDTO (senha = null)
     ↓
[7] Frontend atualiza lista
```

### Fluxo de Edição (com senha)

```
[1] Usuário clica em Editar
     ↓
[2] Preenche dados + nova senha
     ↓
[3] PUT /usuarios/{id}
     Body: {
       login: "joao.silva",
       nome: "João Silva Atualizado",
       senha: "novaSenha123",  ← Nova senha
       permissoes: ["ADMIN"]
     }
     ↓
[4] Backend verifica se senha foi informada
     if (dto.senha() != null && !dto.senha().isEmpty()) {
       usuario.setSenha(passwordEncoder.encode(dto.senha()));
     }
     ↓
[5] UPDATE usuarios SET ..., senha = $2a$10$abc...xyz
```

### Fluxo de Edição (SEM senha)

```
[3] PUT /usuarios/{id}
     Body: {
       login: "joao.silva",
       nome: "João Silva Atualizado",
       senha: "",  ← Vazio ou não enviado
       permissoes: ["ADMIN"]
     }
     ↓
[4] Frontend remove senha vazia
     if (selectedUsuario && !data.senha) {
       delete data.senha;
     }
     ↓
[5] Backend ignora senha
     if (dto.senha() != null && !dto.senha().isEmpty()) {
       // NÃO ENTRA AQUI
     }
     ↓
[6] Senha atual permanece inalterada
```

---

## 💻 Exemplos de Código

### Exemplo: Criar Usuário Admin

**Request:**
```http
POST /usuarios
Content-Type: application/json

{
  "login": "admin",
  "nome": "Administrador do Sistema",
  "senha": "admin123",
  "funcionarioId": null,
  "permissoes": ["ADMIN", "CADASTROS", "RELATORIOS"]
}
```

**SQL Executado:**
```sql
-- 1. Validar login único
SELECT COUNT(*) FROM usuarios WHERE login = 'admin' AND ativo = true;

-- 2. Inserir usuário
INSERT INTO usuarios (login, senha, nome, funcionario_id, ativo) 
VALUES ('admin', '$2a$10$AbCdEfGhIjKlMnOpQrStUvWxYz...', 'Administrador do Sistema', NULL, true);

-- 3. Inserir permissões
INSERT INTO usuario_permissoes (usuario_id, permissao) VALUES (1, 'ADMIN');
INSERT INTO usuario_permissoes (usuario_id, permissao) VALUES (1, 'CADASTROS');
INSERT INTO usuario_permissoes (usuario_id, permissao) VALUES (1, 'RELATORIOS');
```

**Response:**
```json
{
  "id": 1,
  "login": "admin",
  "senha": null,
  "nome": "Administrador do Sistema",
  "permissoes": ["ADMIN", "CADASTROS", "RELATORIOS"],
  "funcionarioId": null,
  "funcionarioNome": null,
  "funcionarioCpf": null
}
```

---

## 🎯 Padrões e Boas Práticas

### 1. Segurança de Senha

```java
// ✅ Sempre criptografar
usuario.setSenha(passwordEncoder.encode(dto.senha()));

// ✅ Nunca retornar senha
public static UsuarioDTO fromEntity(Usuario usuario) {
    return new UsuarioDTO(
        usuario.getId(),
        usuario.getLogin(),
        null, // ← Senha sempre null
        ...
    );
}

// ✅ Validar sem descriptografar
passwordEncoder.matches(senhaTexto, usuario.getSenha())
```

### 2. Senha Opcional na Edição

```tsx
// Frontend - Remover senha vazia
if (selectedUsuario && !data.senha) {
  delete data.senha;
}

// Backend - Atualizar apenas se fornecida
if (dto.senha() != null && !dto.senha().isEmpty()) {
  usuario.setSenha(passwordEncoder.encode(dto.senha()));
}
```

### 3. Validação de Confirmação de Senha

```tsx
const onSubmit = async (data: UsuarioForm) => {
  if (data.senha !== confirmPassword) {
    showNotification('As senhas não coincidem', 'error');
    return;
  }
  
  if (data.senha.length < 6) {
    showNotification('A senha deve ter pelo menos 6 caracteres', 'error');
    return;
  }
  
  // Prosseguir...
};
```

### 4. Permissões como Array de Checkboxes

```tsx
<Controller
  name="permissoes"
  render={({ field }) => (
    <Checkbox
      checked={field.value.includes(permissao)}
      onChange={(e) => {
        if (e.target.checked) {
          field.onChange([...field.value, permissao]);
        } else {
          field.onChange(field.value.filter(p => p !== permissao));
        }
      }}
    />
  )}
/>
```

---

## 🚀 Melhorias Futuras

### 1. Força de Senha

```tsx
import { LinearProgress, Typography } from '@mui/material';

const calcularForcaSenha = (senha: string) => {
  let forca = 0;
  if (senha.length >= 8) forca += 25;
  if (/[a-z]/.test(senha)) forca += 25;
  if (/[A-Z]/.test(senha)) forca += 25;
  if (/[0-9]/.test(senha)) forca += 12.5;
  if (/[^a-zA-Z0-9]/.test(senha)) forca += 12.5;
  return Math.min(forca, 100);
};

<Box>
  <LinearProgress 
    variant="determinate" 
    value={forcaSenha} 
    color={forcaSenha < 50 ? 'error' : forcaSenha < 75 ? 'warning' : 'success'}
  />
  <Typography variant="caption">
    Força: {forcaSenha < 50 ? 'Fraca' : forcaSenha < 75 ? 'Média' : 'Forte'}
  </Typography>
</Box>
```

### 2. Auditoria de Acesso

```sql
CREATE TABLE usuario_audit_log (
    id SERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id),
    acao VARCHAR(50), -- LOGIN, LOGOUT, SENHA_ALTERADA, CRIADO, ATUALIZADO
    ip_address VARCHAR(45),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    detalhes JSON
);
```

### 3. Expiração de Senha

```java
@Entity
public class Usuario {
    private LocalDateTime dataUltimaAlteracaoSenha;
    private Integer diasValidadeSenha = 90;
    
    public boolean isSenhaExpirada() {
        if (dataUltimaAlteracaoSenha == null) return false;
        return LocalDateTime.now()
            .isAfter(dataUltimaAlteracaoSenha.plusDays(diasValidadeSenha));
    }
}
```

---

## 📝 Conclusão

A tela de **Usuários** é a mais **crítica** e **completa** do sistema:

✅ **Autenticação**: Spring Security + JWT + BCrypt  
✅ **Autorização**: Sistema de permissões múltiplas  
✅ **Segurança**: Senha criptografada + nunca retornada  
✅ **UX Rica**: Show/hide password + chips coloridas + filtros avançados  
✅ **644 linhas de Frontend**: Componente mais completo do sistema  

Esta implementação serve como **referência definitiva** para:
- Gerenciamento de usuários com segurança
- Criptografia de senhas com BCrypt
- Implementação de `UserDetails` do Spring Security
- Formulários complexos com validações avançadas
- Checkboxes múltiplos com Controller

**Documento gerado em:** 16 de Outubro de 2025  
**Versão do Sistema:** 1.0  
**Total**: ~1.113 linhas de código (Backend: 385 | Frontend: 728)

