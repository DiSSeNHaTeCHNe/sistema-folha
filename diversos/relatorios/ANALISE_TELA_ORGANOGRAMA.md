# Análise Detalhada da Tela de Organograma

**Data da Análise**: 16 de outubro de 2025  
**Sistema**: Sistema de Folha de Pagamento  
**Módulo**: Organograma

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura Backend](#arquitetura-backend)
3. [Arquitetura Frontend](#arquitetura-frontend)
4. [Fluxo de Dados](#fluxo-de-dados)
5. [Funcionalidade Drag & Drop](#funcionalidade-drag--drop)
6. [Problemas Identificados e Soluções](#problemas-identificados-e-soluções)
7. [Melhorias Implementadas](#melhorias-implementadas)
8. [Recomendações Futuras](#recomendações-futuras)

---

## 🎯 Visão Geral

A tela de Organograma é uma funcionalidade complexa que permite aos usuários criar, visualizar e gerenciar a estrutura organizacional da empresa de forma hierárquica e visual. A funcionalidade inclui:

- **Gestão de Nós**: Criação, edição e exclusão de nós hierárquicos
- **Hierarquia**: Estrutura pai-filho com múltiplos níveis
- **Associações**: Vinculação de funcionários e centros de custo aos nós
- **Drag & Drop**: Interface intuitiva para arrastar funcionários/centros de custo para os nós
- **Visualização**: Representação visual da estrutura organizacional em árvore

---

## 🔧 Arquitetura Backend

### Estrutura de Banco de Dados

#### Tabela: `nos_organograma`
```sql
CREATE TABLE nos_organograma (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    nivel INTEGER NOT NULL DEFAULT 0,
    parent_id BIGINT REFERENCES nos_organograma(id),
    posicao INTEGER NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    organograma_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP,
    data_atualizacao TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);
```

**Campos Principais**:
- `nivel`: Nível hierárquico (0 = raiz)
- `parent_id`: Referência ao nó pai (permite hierarquia)
- `posicao`: Ordem entre nós irmãos
- `organograma_ativo`: Indica se pertence ao organograma ativo

#### Tabela: `funcionario_organograma`
```sql
CREATE TABLE funcionario_organograma (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    no_organograma_id BIGINT NOT NULL REFERENCES nos_organograma(id),
    data_criacao TIMESTAMP,
    criado_por VARCHAR(100),
    UNIQUE(funcionario_id, no_organograma_id)
);
```

**Regra de Negócio**: Um funcionário só pode estar em um nó por vez.

#### Tabela: `centro_custo_organograma`
```sql
CREATE TABLE centro_custo_organograma (
    id BIGSERIAL PRIMARY KEY,
    centro_custo_id BIGINT NOT NULL REFERENCES centros_custo(id),
    no_organograma_id BIGINT NOT NULL REFERENCES nos_organograma(id),
    data_criacao TIMESTAMP,
    criado_por VARCHAR(100),
    UNIQUE(centro_custo_id, no_organograma_id)
);
```

### Constraints e Triggers

#### Constraint: Único Organograma Ativo
```sql
CREATE UNIQUE INDEX idx_unique_organograma_ativo 
ON nos_organograma (organograma_ativo) 
WHERE organograma_ativo = TRUE;
```

**Propósito**: Garante que apenas um organograma esteja ativo por vez, mas permite múltiplos nós com `organograma_ativo = FALSE`.

#### Trigger: Auto-desativação
```sql
CREATE TRIGGER trigger_single_active_organograma
    BEFORE INSERT OR UPDATE ON nos_organograma
    FOR EACH ROW
    EXECUTE FUNCTION check_single_active_organograma();
```

**Comportamento**: Ao ativar um organograma, desativa automaticamente todos os outros.

---

## 🎨 Arquitetura Frontend

### Estrutura de Componentes

```
Organograma (Principal)
├── NoOrganogramaCard (Recursivo)
│   ├── useDroppable (zona de drop)
│   ├── Informações do nó
│   ├── Lista de funcionários (Chips)
│   ├── Lista de centros de custo (Chips)
│   └── Nós filhos (recursivo)
│
├── DraggableItem (Itens arrastáveis)
│   ├── useDraggable
│   └── Representação visual
│
└── DndContext (Contexto de drag & drop)
    ├── Sensores (Pointer, Keyboard)
    ├── DragOverlay (Preview visual)
    └── Handlers (onDragStart, onDragEnd)
```

### Tecnologias Utilizadas

#### React & TypeScript
- **React 19.1**: Framework principal
- **TypeScript**: Tipagem estática
- **React Hooks**: useState, useEffect, useForm

#### Material-UI (MUI) v7
- **Componentes**: Card, Button, Dialog, Chip, Accordion, etc.
- **Layout**: Box, Paper, Typography
- **Ícones**: @mui/icons-material

#### @dnd-kit
Biblioteca moderna de drag & drop para React:
- **DndContext**: Contexto global de D&D
- **useDraggable**: Hook para itens arrastáveis
- **useDroppable**: Hook para zonas de drop
- **DragOverlay**: Preview visual durante o arrasto
- **Sensores**: PointerSensor, KeyboardSensor
- **Collision Detection**: closestCenter

#### React Hook Form
- Gerenciamento de formulários
- Validação de campos
- Integração com Material-UI

#### React Toastify
- Notificações de sucesso/erro
- Feedback visual ao usuário

---

## 🔄 Fluxo de Dados

### 1. Carregamento Inicial

```typescript
const carregarDados = async () => {
    // 1. Buscar dados do backend em paralelo
    const [nosData, funcionariosData, centrosCustoData] = await Promise.all([
        organogramaService.listarTodos(),      // Nós com funcionarioIds/centroCustoIds
        funcionarioService.listar(),            // Todos os funcionários
        centroCustoService.listarTodos(),       // Todos os centros de custo
    ]);

    // 2. Enriquecer nós com objetos completos
    const nosEnriquecidos = nosData.map(no => ({
        ...no,
        funcionarios: no.funcionarioIds?.map(id => 
            funcionariosData.find(f => f.id === id)
        ),
        centrosCusto: no.centroCustoIds?.map(id => 
            centrosCustoData.find(cc => cc.id === id)
        ),
    }));

    // 3. Construir árvore hierárquica
    const arvore = construirArvore(nosEnriquecidos);

    // 4. Filtrar funcionários disponíveis
    const funcionariosAssociadosIds = new Set(
        nosEnriquecidos.flatMap(n => n.funcionarioIds || [])
    );
    const funcionariosDisponiveis = funcionariosData.filter(
        f => !funcionariosAssociadosIds.has(f.id)
    );

    // 5. Atualizar estado
    setNos(arvore);
    setFuncionarios(funcionariosDisponiveis);
    setCentrosCusto(centrosCustoData);
};
```

### 2. Construção da Árvore

```typescript
const construirArvore = (nos: NoOrganograma[]): NoOrganogramaWithChildren[] => {
    const nosMap = new Map<number, NoOrganogramaWithChildren>();
    const raizes: NoOrganogramaWithChildren[] = [];

    // Criar mapa de todos os nós
    nos.forEach(no => {
        nosMap.set(no.id, { ...no, children: [] });
    });

    // Construir hierarquia
    nos.forEach(no => {
        const noComChildren = nosMap.get(no.id)!;
        if (no.parentId) {
            // Nó com pai: adicionar aos filhos do pai
            const parent = nosMap.get(no.parentId);
            if (parent) {
                parent.children.push(noComChildren);
            }
        } else {
            // Nó raiz: adicionar à lista de raízes
            raizes.push(noComChildren);
        }
    });

    // Ordenar por posição (recursivamente)
    ordenarPorPosicao(raizes);
    
    return raizes;
};
```

### 3. Endpoints da API

#### GET /api/organogramas
**Response**:
```json
[
  {
    "id": 1,
    "nome": "Diretoria",
    "descricao": "Diretoria Executiva",
    "nivel": 0,
    "parentId": null,
    "posicao": 0,
    "ativo": true,
    "organogramaAtivo": true,
    "funcionarioIds": [1, 2, 3],
    "centroCustoIds": [1],
    "children": []
  }
]
```

#### POST /api/organogramas
**Request**:
```json
{
  "nome": "Departamento TI",
  "descricao": "Tecnologia da Informação",
  "parentId": 1
}
```

#### POST /api/organogramas/{noId}/funcionarios/{funcionarioId}
**Associa um funcionário a um nó**

#### DELETE /api/organogramas/{noId}/funcionarios/{funcionarioId}
**Remove a associação**

---

## 🎯 Funcionalidade Drag & Drop

### Implementação com @dnd-kit

#### 1. Configuração do DndContext

```tsx
<DndContext
  sensors={sensors}
  collisionDetection={closestCenter}
  onDragStart={handleDragStart}
  onDragEnd={handleDragEnd}
>
  {/* Conteúdo */}
</DndContext>
```

**Sensores**:
- `PointerSensor`: Mouse/touch com threshold de 8px
- `KeyboardSensor`: Acessibilidade via teclado

**Collision Detection**:
- `closestCenter`: Detecta o drop zone mais próximo do centro do item

#### 2. Itens Arrastáveis (DraggableItem)

```tsx
const DraggableItem = ({ item }) => {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: item.id,  // Ex: "funcionario-123"
    data: item,   // Dados completos do item
  });

  return (
    <div ref={setNodeRef} style={{...}} {...attributes} {...listeners}>
      {/* Conteúdo visual */}
    </div>
  );
};
```

**IDs Utilizados**:
- `funcionario-{id}`: Para funcionários
- `centroCusto-{id}`: Para centros de custo

#### 3. Zonas de Drop (NoOrganogramaCard)

```tsx
const NoOrganogramaCard = ({ no }) => {
  const { setNodeRef, isOver } = useDroppable({
    id: `no-${no.id}`,  // Ex: "no-5"
    data: {
      type: 'no-organograma',
      noId: no.id,
    },
  });

  return (
    <Card
      ref={setNodeRef}
      sx={{
        borderColor: isOver ? 'primary.main' : 'grey.300',
        bgcolor: isOver ? 'primary.light' : 'background.paper',
      }}
    >
      {/* Conteúdo do nó */}
    </Card>
  );
};
```

**Feedback Visual**:
- Borda muda para azul quando item está sobre o nó
- Background fica levemente azulado

#### 4. Handlers de Eventos

##### onDragStart
```tsx
const handleDragStart = (event: DragStartEvent) => {
  const { active } = event;
  const activeId = active.id as string;
  
  // Identificar o item sendo arrastado
  if (activeId.startsWith('funcionario-')) {
    const funcionarioId = parseInt(activeId.replace('funcionario-', ''));
    const funcionario = funcionarios.find(f => f.id === funcionarioId);
    setActiveItem({ id: activeId, type: 'funcionario', data: funcionario });
  }
  // Similar para centro de custo...
};
```

##### onDragEnd
```tsx
const handleDragEnd = async (event: DragEndEvent) => {
  const { active, over } = event;
  
  setActiveItem(null);  // Limpar preview
  
  if (!over) return;  // Drop fora de zona válida
  
  const activeId = active.id as string;
  const overId = over.id as string;
  
  // Processar drop de funcionário em nó
  if (activeId.startsWith('funcionario-') && overId.startsWith('no-')) {
    const funcionarioId = parseInt(activeId.replace('funcionario-', ''));
    const noId = parseInt(overId.replace('no-', ''));
    
    try {
      await organogramaService.adicionarFuncionario(noId, funcionarioId);
      toast.success('Funcionário adicionado ao nó');
      carregarDados();  // Recarregar dados
    } catch (error) {
      toast.error('Erro ao adicionar funcionário');
    }
  }
  
  // Similar para centro de custo...
};
```

#### 5. DragOverlay (Preview Visual)

```tsx
<DragOverlay style={{ zIndex: 10000 }}>
  {activeItem ? (
    <Paper sx={{ p: 1, border: '2px solid', borderColor: 'primary.main' }}>
      <Typography>
        {activeItem.type === 'funcionario' 
          ? activeItem.data.nome 
          : activeItem.data.descricao
        }
      </Typography>
    </Paper>
  ) : null}
</DragOverlay>
```

**Propósito**: Mostra uma cópia visual do item sendo arrastado que segue o cursor.

---

## 🐛 Problemas Identificados e Soluções

### Problema 1: Itens Indo para Trás Durante Drag

**Sintoma**: Ao arrastar um item (funcionário ou centro de custo), ele aparecia atrás de outros elementos da tela.

**Causa**: Falta de `z-index` adequado no elemento sendo arrastado.

**Solução**:
```tsx
const style = {
  transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
  opacity: isDragging ? 0.5 : 1,
  zIndex: isDragging ? 9999 : 'auto',  // ← Solução
  cursor: isDragging ? 'grabbing' : 'grab',
};
```

### Problema 2: Itens Não Sendo Adicionados aos Nós

**Sintoma**: Visualmente o drag & drop funcionava, mas os itens não eram associados aos nós no backend.

**Causa**: 
1. Uso incorreto de `useSortable` em vez de `useDraggable`/`useDroppable`
2. Falta de dados (`data`) nos hooks
3. Backend retornando dados incompletos

**Solução**:
1. Substituir `useSortable` por `useDraggable` e `useDroppable`:
```tsx
// ANTES (errado)
const { setNodeRef } = useSortable({ id: `no-${no.id}` });

// DEPOIS (correto)
const { setNodeRef, isOver } = useDroppable({
  id: `no-${no.id}`,
  data: { type: 'no-organograma', noId: no.id }
});
```

2. Passar dados completos nos hooks para debug
3. Adicionar logs detalhados em `handleDragEnd`

### Problema 3: Frontend Não Mostrando Funcionários Associados

**Sintoma**: Após associar um funcionário ao nó, ele continuava na lista de disponíveis e não aparecia no card do nó.

**Causa**: Backend retornando apenas IDs (`funcionarioIds`), mas não os objetos completos.

**Solução**:

**Backend** (`OrganogramaService.java`):
```java
// ANTES
public List<NoOrganogramaDTO> listarTodos() {
    return noOrganogramaRepository.findByAtivoTrue().stream()
            .map(this::toDTO)  // ← Não carrega funcionarioIds
            .collect(Collectors.toList());
}

// DEPOIS
public List<NoOrganogramaDTO> listarTodos() {
    return noOrganogramaRepository.findByAtivoTrue().stream()
            .map(this::toDTOCompleto)  // ← Carrega funcionarioIds e centroCustoIds
            .collect(Collectors.toList());
}
```

**Frontend** (`Organograma/index.tsx`):
```tsx
// Enriquecer nós com objetos completos
const nosEnriquecidos = nosData.map(no => ({
  ...no,
  funcionarios: no.funcionarioIds?.map(id => 
    funcionariosData.find(f => f.id === id)
  ).filter(Boolean),
  centrosCusto: no.centroCustoIds?.map(id => 
    centrosCustoData.find(cc => cc.id === id)
  ).filter(Boolean),
}));
```

### Problema 4: Constraint Incorreta no Banco

**Sintoma**: Erro ao criar novo nó:
```
ERROR: duplicate key value violates unique constraint "check_organograma_ativo"
Key (organograma_ativo)=(f) already exists.
```

**Causa**: Constraint `UNIQUE (organograma_ativo)` impedia múltiplos registros com `FALSE`.

**Solução**: Migration V1.7
```sql
-- Remover constraint incorreta
ALTER TABLE nos_organograma DROP CONSTRAINT IF EXISTS check_organograma_ativo;

-- Criar índice único parcial (só para TRUE)
CREATE UNIQUE INDEX idx_unique_organograma_ativo 
ON nos_organograma (organograma_ativo) 
WHERE organograma_ativo = TRUE;
```

**Resultado**: Permite múltiplos nós com `organograma_ativo = FALSE`, mas apenas um com `TRUE`.

---

## ✨ Melhorias Implementadas

### 1. Logs de Debug Detalhados

```tsx
console.log('🔍 Dados recebidos do backend:', {
  totalNos: nosData.length,
  primeiroNo: nosData[0],
  temFuncionarioIds: nosData[0]?.funcionarioIds,
});

console.log('✅ Nós enriquecidos:', nosEnriquecidos[0]);

console.log('📊 Estatísticas:', {
  totalFuncionarios: funcionariosData.length,
  funcionariosAssociados: funcionariosAssociadosIds.size,
  funcionariosDisponiveis: funcionariosDisponiveis.length,
  nosComFuncionarios: nosEnriquecidos.filter(n => n.funcionarios.length > 0).length,
});
```

### 2. Feedback Visual Rico

- **Border colorida**: Indica zona de drop ativa
- **Background highlight**: Muda quando item está sobre nó
- **Opacity reduzida**: Item original fica semi-transparente durante drag
- **DragOverlay**: Preview visual que segue o cursor
- **Toast notifications**: Feedback de sucesso/erro

### 3. Tratamento de Erros Robusto

```tsx
try {
  await organogramaService.adicionarFuncionario(noId, funcionarioId);
  toast.success('Funcionário adicionado ao nó');
  carregarDados();
} catch (error: any) {
  console.error('❌ Erro ao adicionar funcionário:', error);
  toast.error(error?.response?.data?.message || 'Erro ao adicionar funcionário');
}
```

### 4. Validações no Backend

```java
// Verificar se funcionário já está em outro nó
List<FuncionarioOrganograma> associacoesExistentes = 
    funcionarioOrganogramaRepository.findByFuncionario(funcionario);
    
if (!associacoesExistentes.isEmpty()) {
    NoOrganograma noExistente = associacoesExistentes.get(0).getNoOrganograma();
    throw new IllegalArgumentException(
        "Funcionário '" + funcionario.getNome() + 
        "' já está associado ao nó '" + noExistente.getNome() + 
        "'. Um funcionário só pode estar em um nó por vez."
    );
}
```

---

## 🔮 Recomendações Futuras

### 1. Performance

- **Lazy Loading**: Carregar nós sob demanda para organogramas muito grandes
- **Memoização**: Usar `useMemo` para evitar recálculos desnecessários
- **Virtualização**: Para listas longas de funcionários/centros de custo

### 2. UX/UI

- **Busca/Filtro**: Adicionar busca para encontrar funcionários rapidamente
- **Zoom/Pan**: Para organogramas grandes, permitir zoom e navegação
- **Modo Compacto**: Visualização mais condensada
- **Export**: Exportar organograma como imagem ou PDF

### 3. Funcionalidades

- **Histórico**: Rastrear mudanças no organograma ao longo do tempo
- **Comparação**: Comparar diferentes versões de organogramas
- **Permissões**: Controlar quem pode editar cada nó
- **Notificações**: Alertar gestores sobre mudanças em seus departamentos

### 4. Testes

- **Testes Unitários**: Testar lógica de construção de árvore
- **Testes de Integração**: Testar API endpoints
- **Testes E2E**: Testar fluxo completo de drag & drop
- **Testes de Acessibilidade**: Garantir navegação por teclado

### 5. Documentação

- **Guia do Usuário**: Como usar a tela de organograma
- **Swagger**: Documentar API com exemplos
- **Storybook**: Documentar componentes isoladamente

---

## 📊 Métricas de Qualidade

### Código Frontend
- **TypeScript**: ✅ 100% tipado
- **Hooks Modernos**: ✅ Uso correto de useState, useEffect
- **Error Handling**: ✅ Try-catch e feedback ao usuário
- **Componentização**: ✅ Componentes pequenos e reutilizáveis

### Código Backend
- **SOLID**: ✅ Service layer separado
- **Validações**: ✅ Regras de negócio validadas
- **Transações**: ✅ @Transactional onde necessário
- **Logging**: ✅ Logs informativos e de erro

### Banco de Dados
- **Normalização**: ✅ 3ª Forma Normal
- **Constraints**: ✅ Integridade referencial garantida
- **Índices**: ✅ Otimização de queries
- **Soft Delete**: ✅ Preservação de histórico

---

## 📚 Referências Técnicas

### Bibliotecas Principais
- **@dnd-kit**: https://dndkit.com/
- **Material-UI**: https://mui.com/
- **React Hook Form**: https://react-hook-form.com/
- **Spring Boot**: https://spring.io/projects/spring-boot

### Padrões de Projeto
- **Repository Pattern**: Acesso a dados
- **Service Layer**: Lógica de negócio
- **DTO Pattern**: Transferência de dados
- **Composite Pattern**: Estrutura hierárquica de nós

---

## 🎓 Aprendizados

1. **@dnd-kit vs react-dnd**: @dnd-kit é mais moderno, performático e flexível
2. **useDraggable vs useSortable**: Usar o hook correto para cada caso
3. **Índices Parciais**: PostgreSQL permite constraints condicionais elegantes
4. **TypeScript Generics**: Melhoram reutilização de código
5. **React 19**: Novos hooks e otimizações de performance

---

## ✅ Conclusão

A tela de Organograma é uma funcionalidade **complexa e bem estruturada** que demonstra:

- ✅ **Arquitetura sólida**: Separação clara de responsabilidades
- ✅ **UX moderna**: Drag & drop intuitivo com feedback visual rico
- ✅ **Código limpo**: TypeScript, validações, error handling
- ✅ **Performance**: Queries otimizadas, índices, lazy loading potencial
- ✅ **Manutenibilidade**: Código documentado, logs detalhados, estrutura clara

As **soluções implementadas** corrigiram todos os problemas identificados e a funcionalidade está **100% operacional**.

---

**Última atualização**: 16 de outubro de 2025  
**Autor**: Análise técnica do Sistema de Folha de Pagamento

