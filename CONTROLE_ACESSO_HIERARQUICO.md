# Controle de Acesso Hierárquico Baseado no Organograma

**Data de Implementação**: 20 de outubro de 2025  
**Versão**: 1.0

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Regras de Negócio](#regras-de-negócio)
3. [Arquitetura da Solução](#arquitetura-da-solução)
4. [Implementação Backend](#implementação-backend)
5. [Implementação Frontend](#implementação-frontend)
6. [Fluxo de Funcionamento](#fluxo-de-funcionamento)
7. [Exemplos de Uso](#exemplos-de-uso)
8. [Testes e Validação](#testes-e-validação)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 Visão Geral

O sistema implementa um **controle de acesso hierárquico** que restringe a visualização de dados da folha de pagamento baseado na posição do usuário no organograma da empresa. 

### Conceito Principal

Um usuário com funcionário vinculado a um nó do organograma tem acesso aos dados de:
- Seu próprio nó
- Todos os nós descendentes (filhos, netos, bisnetos, etc.)
- Todos os centros de custo vinculados a esses nós

**A travessia da árvore não é bloqueada por nós sem centro de custo** - o sistema continua coletando centros dos níveis inferiores.

---

## 📜 Regras de Negócio

### 1. Vínculo Funcionário-Nó
- ✅ Um funcionário só pode estar vinculado a **1 nó** do organograma
- ✅ Constraint `UNIQUE` na tabela `funcionario_organograma` garante isso no banco

### 2. Usuário Sem Funcionário
- ❌ **Sem acesso aos dados**
- O sistema retorna lista vazia para todas as consultas

### 3. Funcionário Sem Nó no Organograma
- ✅ **Acesso total (sem restrições)**
- Pode visualizar todos os centros de custo e folhas
- Útil para administradores ou usuários de sistema

### 4. Filtro "Todos"
- ✅ Traz **apenas os centros de custo acessíveis**
- Não mostra dados de outros centros
- Totalmente transparente para o usuário

### 5. Travessia de Nós Sem Centro de Custo
- ✅ Sistema **continua percorrendo** os níveis abaixo
- Coleta centros de custo de todos os descendentes
- Nós intermediários sem centros não bloqueiam o acesso aos níveis inferiores

---

## 🏗️ Arquitetura da Solução

### Componentes Backend

```
OrganogramaAcessoService (NEW)
  ├─ obterNoDoUsuario(usuarioId)
  ├─ obterCentrosCustoAcessiveis(usuarioId)
  ├─ usuarioPodeAcessarCentroCusto(usuarioId, centroCustoId)
  ├─ usuarioTemAcessoTotal(usuarioId)
  └─ obterInformacoesAcesso(usuarioId)

AcessoUsuarioDTO (NEW)
  ├─ noOrganogramaId
  ├─ noOrganogramaNome
  ├─ nivel
  ├─ centrosCustoAcessiveis (Set<Long>)
  ├─ acessoTotal (Boolean)
  └─ quantidadeCentrosAcessiveis

TokenDTO (MODIFICADO)
  └─+ acessoUsuario (AcessoUsuarioDTO)

AuthenticationService (MODIFICADO)
  └─ Adiciona acessoUsuario no login/refresh

AuthController (MODIFICADO)
  └─+ GET /auth/acesso

FolhaPagamentoController (MODIFICADO)
  ├─ obterCentrosAcessiveis(authentication)
  └─ aplicarFiltroAcesso(folha, centrosAcessiveis)
```

### Componentes Frontend

```
types/index.ts (MODIFICADO)
  └─+ AcessoUsuario interface

AuthContext (MODIFICADO)
  ├─+ acessoUsuario state
  ├─+ podeAcessarCentroCusto(centroCustoId)
  └─ Armazena acessoUsuario no login
```

---

## 🔧 Implementação Backend

### 1. OrganogramaAcessoService

Serviço principal que gerencia o controle de acesso hierárquico.

#### Método Principal: `obterCentrosCustoAcessiveis(Long usuarioId)`

```java
@Transactional(readOnly = true)
public Set<Long> obterCentrosCustoAcessiveis(Long usuarioId) {
    Optional<NoOrganograma> noOpt = obterNoDoUsuario(usuarioId);
    
    // Se não tem nó, significa acesso total (regra 3)
    if (noOpt.isEmpty()) {
        return Collections.emptySet(); // Empty = sem filtro
    }
    
    NoOrganograma noInicial = noOpt.get();
    Set<Long> centrosAcessiveis = new HashSet<>();
    
    // Coletar centros de custo recursivamente
    coletarCentrosCustoRecursivo(noInicial, centrosAcessiveis);
    
    return centrosAcessiveis;
}
```

#### Método Recursivo

```java
private void coletarCentrosCustoRecursivo(NoOrganograma no, Set<Long> centrosAcessiveis) {
    // 1. Coletar centros de custo deste nó
    List<CentroCustoOrganograma> centros = 
        centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no);
    
    for (CentroCustoOrganograma centro : centros) {
        centrosAcessiveis.add(centro.getCentroCusto().getId());
    }
    
    // 2. Buscar filhos ativos e processar recursivamente
    List<NoOrganograma> filhos = 
        noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no);
    
    for (NoOrganograma filho : filhos) {
        coletarCentrosCustoRecursivo(filho, centrosAcessiveis);
    }
}
```

### 2. FolhaPagamentoController - Aplicação do Filtro

Todos os endpoints de consulta de folha aplicam o filtro automaticamente:

```java
private Set<Long> obterCentrosAcessiveis(Authentication authentication) {
    String login = authentication.getName();
    Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(login)
        .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    
    return organogramaAcessoService.obterCentrosCustoAcessiveis(usuario.getId());
}

private boolean aplicarFiltroAcesso(FolhaPagamento folha, Set<Long> centrosAcessiveis) {
    // Empty set = acesso total, sem filtro
    if (centrosAcessiveis.isEmpty()) {
        return true;
    }
    
    // Verificar se o centro de custo do funcionário está nos centros acessíveis
    if (folha.getFuncionario() != null && 
        folha.getFuncionario().getCentroCusto() != null) {
        return centrosAcessiveis.contains(
            folha.getFuncionario().getCentroCusto().getId()
        );
    }
    
    // Se não tem centro de custo, não permitir acesso (por segurança)
    return false;
}
```

### 3. Autenticação - Inclusão de Acesso no Token

```java
private AcessoUsuarioDTO obterAcessoUsuario(Long usuarioId) {
    Optional<NoOrganograma> noOpt = organogramaAcessoService.obterNoDoUsuario(usuarioId);
    Set<Long> centrosAcessiveis = organogramaAcessoService.obterCentrosCustoAcessiveis(usuarioId);
    boolean acessoTotal = organogramaAcessoService.usuarioTemAcessoTotal(usuarioId);
    
    AcessoUsuarioDTO.AcessoUsuarioDTOBuilder builder = AcessoUsuarioDTO.builder()
        .centrosCustoAcessiveis(centrosAcessiveis)
        .acessoTotal(acessoTotal)
        .quantidadeCentrosAcessiveis(centrosAcessiveis.size());
    
    if (noOpt.isPresent()) {
        NoOrganograma no = noOpt.get();
        builder.noOrganogramaId(no.getId())
               .noOrganogramaNome(no.getNome())
               .nivel(no.getNivel());
    }
    
    return builder.build();
}
```

---

## 💻 Implementação Frontend

### 1. AuthContext - Armazenamento de Acesso

```typescript
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Usuario | null>(null);
  const [acessoUsuario, setAcessoUsuario] = useState<AcessoUsuario | null>(null);
  
  // Login salva as informações de acesso
  const login = async (data: LoginRequest) => {
    const response = await apiLogin(data);
    
    if (response.acessoUsuario) {
      localStorage.setItem('acessoUsuario', JSON.stringify(response.acessoUsuario));
      setAcessoUsuario(response.acessoUsuario);
    }
    
    // ... resto do código
  };
  
  // Função helper para verificar acesso
  const podeAcessarCentroCusto = (centroCustoId: number): boolean => {
    if (!acessoUsuario) return true;
    if (acessoUsuario.acessoTotal) return true;
    return acessoUsuario.centrosCustoAcessiveis.includes(centroCustoId);
  };
}
```

### 2. Uso no Componente

```typescript
import { useAuth } from '../contexts/AuthContext';

function MeuComponente() {
  const { acessoUsuario, podeAcessarCentroCusto } = useAuth();
  
  // Verificar se tem acesso total
  if (acessoUsuario?.acessoTotal) {
    console.log('Usuário tem acesso total!');
  }
  
  // Verificar acesso a centro específico
  const temAcesso = podeAcessarCentroCusto(123);
  
  // Ver informações de acesso
  console.log('Nó do usuário:', acessoUsuario?.noOrganogramaNome);
  console.log('Centros acessíveis:', acessoUsuario?.centrosCustoAcessiveis);
}
```

---

## 🔄 Fluxo de Funcionamento

### 1. Login do Usuário

```
1. Usuário faz login
   ↓
2. AuthenticationService.authenticate()
   ↓
3. OrganogramaAcessoService.obterAcessoUsuario(usuarioId)
   ↓
4. TokenDTO retorna com acessoUsuario incluído
   ↓
5. Frontend armazena acessoUsuario no localStorage e AuthContext
```

### 2. Consulta de Folha de Pagamento

```
1. Frontend chama GET /folha-pagamento?dataInicio=...&dataFim=...
   ↓
2. FolhaPagamentoController.consultarPorPeriodo()
   ↓
3. obterCentrosAcessiveis(authentication)
   │  └─> Busca Set<Long> de centros acessíveis pelo usuário
   ↓
4. Busca registros de folha do banco
   ↓
5. aplicarFiltroAcesso() para cada registro
   │  ├─ Se centrosAcessiveis.isEmpty() → true (acesso total)
   │  └─ Senão → verifica se centro do funcionário está na lista
   ↓
6. Retorna apenas registros permitidos
```

### 3. Coleta Recursiva de Centros

```
Exemplo de Organograma:

        [Diretoria] (sem centro)
             |
    ┌────────┴─────────┐
    |                  |
[Depto A]          [Depto B]
(Centro 1)         (Centro 2)
    |                  |
[Setor A1]         [Setor B1]
(Centro 3)         (sem centro)
                       |
                   [Equipe B1.1]
                   (Centro 4)

Usuário vinculado em [Diretoria]:
  → Centros acessíveis: {1, 2, 3, 4}
  → Percorre toda a árvore, mesmo passando por nós sem centro

Usuário vinculado em [Depto B]:
  → Centros acessíveis: {2, 4}
  → Não tem acesso aos centros do Depto A

Usuário vinculado em [Setor B1]:
  → Centros acessíveis: {} (sem centros)
  → Não tem acesso a nenhum centro (lista vazia, mas não é acesso total)
  
Usuário sem nó no organograma:
  → centrosAcessiveis: {} (empty set)
  → aplicarFiltroAcesso() retorna true sempre = ACESSO TOTAL
```

---

## 📝 Exemplos de Uso

### Exemplo 1: Usuário com Acesso Restrito

```
Usuário: João Silva
Funcionário: ID 10
Nó do Organograma: "Gerência de TI" (ID 5)
Centros de Custo Acessíveis: [10, 11, 12]

GET /folha-pagamento?dataInicio=2025-01-01&dataFim=2025-01-31
→ Retorna apenas registros de funcionários dos centros 10, 11, 12
```

### Exemplo 2: Usuário com Acesso Total

```
Usuário: Admin
Funcionário: ID 1
Nó do Organograma: Nenhum
Centros de Custo Acessíveis: [] (empty = acesso total)

GET /folha-pagamento?dataInicio=2025-01-01&dataFim=2025-01-31
→ Retorna TODOS os registros do período
```

### Exemplo 3: Usuário Sem Funcionário

```
Usuário: Sistema
Funcionário: Nenhum
Centros de Custo Acessíveis: null

GET /folha-pagamento?dataInicio=2025-01-01&dataFim=2025-01-31
→ Retorna lista vazia (sem acesso)
```

---

## ✅ Testes e Validação

### 1. Testar Controle de Acesso Básico

```bash
# 1. Login com usuário com nó do organograma
POST /auth/login
{
  "login": "joao.silva",
  "senha": "senha123"
}

# Verificar resposta - deve conter acessoUsuario
{
  "token": "...",
  "acessoUsuario": {
    "noOrganogramaId": 5,
    "noOrganogramaNome": "Gerência de TI",
    "nivel": 2,
    "centrosCustoAcessiveis": [10, 11, 12],
    "acessoTotal": false,
    "quantidadeCentrosAcessiveis": 3
  }
}
```

### 2. Testar Consulta com Filtro

```bash
# Buscar folha - deve retornar apenas centros acessíveis
GET /folha-pagamento?dataInicio=2025-01-01&dataFim=2025-01-31
Authorization: Bearer {token}

# Verificar logs do backend
# Deve mostrar: "Usuário joao.silva consultou folha com X registros (após filtro de acesso)"
```

### 3. Testar Acesso Total

```bash
# Login com admin (sem nó)
POST /auth/login
{
  "login": "admin",
  "senha": "admin123"
}

# Resposta deve ter acessoTotal: true
{
  "acessoUsuario": {
    "centrosCustoAcessiveis": [],
    "acessoTotal": true,
    "quantidadeCentrosAcessiveis": 0
  }
}
```

### 4. Endpoint de Informações de Acesso

```bash
# Consultar informações de acesso
GET /auth/acesso
Authorization: Bearer {token}

# Resposta
{
  "temFuncionario": true,
  "acessoTotal": false,
  "quantidadeCentrosAcessiveis": 3,
  "noOrganogramaId": 5,
  "noOrganogramaNome": "Gerência de TI",
  "nivel": 2,
  "centrosCustoIds": [10, 11, 12]
}
```

---

## 🔍 Troubleshooting

### Problema: Usuário não vê nenhum dado

**Possíveis Causas:**
1. Usuário não tem funcionário vinculado
2. Funcionário está vinculado a um nó sem centros de custo (e sem descendentes)
3. Centros de custo do nó estão inativos

**Solução:**
```sql
-- Verificar se usuário tem funcionário
SELECT u.id, u.login, u.funcionario_id, f.nome
FROM usuarios u
LEFT JOIN funcionarios f ON f.id = u.funcionario_id
WHERE u.login = 'usuario_teste';

-- Verificar se funcionário está em algum nó
SELECT fo.id, fo.funcionario_id, fo.no_organograma_id, n.nome
FROM funcionario_organograma fo
JOIN nos_organograma n ON n.id = fo.no_organograma_id
WHERE fo.funcionario_id = ?;

-- Verificar centros de custo do nó
SELECT cco.id, cc.id as centro_id, cc.descricao, cc.ativo
FROM centro_custo_organograma cco
JOIN centros_custo cc ON cc.id = cco.centro_custo_id
WHERE cco.no_organograma_id = ?;
```

### Problema: Usuário vê dados que não deveria

**Possíveis Causas:**
1. Usuário tem acesso total (não está em nenhum nó)
2. Centro de custo está duplicado em nós diferentes

**Solução:**
```bash
# Verificar logs do backend
# Procurar por: "Usuário X tem acesso total (sem restrições)"

# Verificar no frontend
console.log(acessoUsuario);
// Se acessoTotal: true → comportamento esperado
```

### Problema: Performance lenta

**Possíveis Causas:**
1. Organograma muito profundo (muitos níveis)
2. Muitos centros de custo
3. Consulta recursiva sem cache

**Solução:**
- Implementar cache dos centros acessíveis (Spring Cache)
- Usar CTE recursiva no PostgreSQL para queries complexas
- Adicionar índices nas tabelas de associação

---

## 📊 Monitoramento e Logs

### Logs Importantes

```java
// OrganogramaAcessoService
logger.info("Usuário ID {} está no nó '{}' (ID: {})", usuarioId, no.getNome(), no.getId());
logger.info("Usuário ID {} tem acesso a {} centros de custo", usuarioId, centrosAcessiveis.size());

// FolhaPagamentoController
logger.info("Usuário {} consultou folha com {} registros (após filtro de acesso)", 
           authentication.getName(), folha.size());
logger.warn("Usuário {} tentou acessar centro de custo {} sem permissão", 
           authentication.getName(), centroCustoId);
```

### Métricas Recomendadas

- Quantidade média de centros acessíveis por usuário
- Tempo de resposta das consultas de folha
- Tentativas de acesso negadas
- Usuários com acesso total vs. restrito

---

## 🚀 Melhorias Futuras

### Curto Prazo
1. ✅ Implementar cache de centros acessíveis
2. ✅ Adicionar auditoria de acessos
3. ✅ Interface de administração de permissões

### Médio Prazo
1. Permitir exceções manuais (acesso a centros específicos)
2. Histórico de mudanças de posição no organograma
3. Relatório de acessos por usuário

### Longo Prazo
1. Controle de acesso granular por tipo de dado (proventos vs. descontos)
2. Permissões temporárias (acesso por período)
3. Delegação de acesso (usuário A autoriza usuário B temporariamente)

---

## 📚 Referências

- **Padrão de Segurança**: RBAC (Role-Based Access Control) + HBAC (Hierarchy-Based Access Control)
- **Estrutura de Dados**: Árvore N-ária com travessia DFS (Depth-First Search)
- **Performance**: O(n) onde n = número de nós descendentes

---

**Autor**: Sistema desenvolvido para controle de acesso baseado em organograma  
**Contato**: Documentação técnica do sistema de folha de pagamento  
**Última Atualização**: 20 de outubro de 2025

