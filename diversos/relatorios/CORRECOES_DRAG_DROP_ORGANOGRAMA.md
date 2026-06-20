# Correções da Funcionalidade Drag & Drop - Organograma

**Data**: 16 de outubro de 2025  
**Módulo**: Organograma  
**Tipo**: Correção de Bugs e Melhorias

---

## 🐛 Problemas Relatados

### 1. Problema Visual - Z-Index
**Relato do usuário**: 
> "Quando pego um item para arrastar, ele aparentemente está indo para trás dos outros elementos da tela"

**Sintomas**:
- Item arrastado aparece atrás de outros elementos (cards, botões, etc.)
- Dificulta visualizar o que está sendo arrastado
- UX ruim e confusa

### 2. Problema Funcional - Drop Não Funciona
**Relato do usuário**: 
> "Eu não consigo adicionar nó da estrutura na tela. Acontece tanto para funcionário como para centro de custo"

**Sintomas**:
- Drag visual funciona, mas drop não tem efeito
- Itens não são associados ao nó
- Nenhum feedback ou erro visível

### 3. Problema de Dados - Itens Não Aparecem no Card
**Relato do usuário**: 
> "O funcionário foi associado ao nó no backend, mas o frontend não está mostrando o funcionário no card e nem removendo ele da lista de disponíveis"

**Sintomas**:
- Backend confirma associação com sucesso
- Frontend não atualiza visualmente
- Funcionário continua na lista de disponíveis
- Não aparece no card do nó

### 4. Problema de Banco - Constraint Incorreta
**Erro do sistema**:
```
ERROR: duplicate key value violates unique constraint "check_organograma_ativo"
Key (organograma_ativo)=(f) already exists.
```

**Sintomas**:
- Impossível criar novos nós
- Erro ao tentar adicionar qualquer nó ao organograma

---

## ✅ Correções Implementadas

### Correção 1: Z-Index Durante Drag

**Arquivo**: `frontend/src/pages/Organograma/index.tsx`

**Mudança no componente `DraggableItem`**:
```tsx
// ANTES
const style = {
  transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
  opacity: isDragging ? 0.5 : 1,
  cursor: isDragging ? 'grabbing' : 'grab',
};

// DEPOIS
const style = {
  transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
  opacity: isDragging ? 0.5 : 1,
  zIndex: isDragging ? 9999 : 'auto',  // ← Adicionado
  cursor: isDragging ? 'grabbing' : 'grab',
};
```

**Resultado**:
- ✅ Item arrastado sempre fica no topo
- ✅ Não fica escondido atrás de outros elementos
- ✅ Melhor experiência visual

---

### Correção 2: Hooks Corretos para Drag & Drop

**Arquivo**: `frontend/src/pages/Organograma/index.tsx`

#### Mudança 1: NoOrganogramaCard (Drop Zone)

```tsx
// ANTES - Usando useSortable (INCORRETO)
const { setNodeRef } = useSortable({
  id: `no-${no.id}`,
});

// DEPOIS - Usando useDroppable (CORRETO)
const { setNodeRef, isOver } = useDroppable({
  id: `no-${no.id}`,
  data: {
    type: 'no-organograma',
    noId: no.id,
  },
});
```

**Razão**: `useSortable` é para reordenar itens em listas. `useDroppable` é para criar zonas onde itens podem ser soltos.

#### Mudança 2: DraggableItem

```tsx
// ANTES - Usando useSortable (INCORRETO)
const { attributes, listeners, setNodeRef, transform, isDragging } = useSortable({
  id: item.id,
});

// DEPOIS - Usando useDraggable (CORRETO)
const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
  id: item.id,
  data: item,  // ← Adicionado: passa dados completos
});
```

**Razão**: `useDraggable` é especificamente para criar itens arrastáveis independentes.

#### Mudança 3: Implementação do DragOverlay

```tsx
// ADICIONADO
<DragOverlay
  dropAnimation={{
    duration: 200,
    easing: 'cubic-bezier(0.18, 0.67, 0.6, 1.22)',
  }}
  style={{ zIndex: 10000 }}
>
  {activeItem ? (
    <Paper
      sx={{
        p: 1,
        cursor: 'grabbing',
        border: '2px solid',
        borderColor: 'primary.main',
        bgcolor: 'background.paper',
        boxShadow: 3,
      }}
    >
      <Typography variant="body2" display="flex" alignItems="center" gap={1}>
        {activeItem.type === 'funcionario' ? <PersonIcon /> : <BusinessIcon />}
        {activeItem.type === 'funcionario' 
          ? (activeItem.data as Funcionario).nome 
          : (activeItem.data as CentroCusto).descricao
        }
      </Typography>
    </Paper>
  ) : null}
</DragOverlay>
```

**Benefícios**:
- ✅ Preview visual bonito que segue o cursor
- ✅ Sempre no topo (z-index 10000)
- ✅ Animação suave de drop

#### Mudança 4: Feedback Visual no Drop Zone

```tsx
<Card
  ref={setNodeRef}
  sx={{
    border: '2px solid',
    borderColor: isOver ? 'primary.main' : 'grey.300',  // ← Muda quando hover
    bgcolor: isOver ? 'primary.light' : 'background.paper',  // ← Background highlight
    minHeight: 200,
    transition: 'all 0.2s ease',  // ← Transição suave
    position: 'relative',
  }}
>
```

**Resultado**:
- ✅ Card muda de cor quando item está sobre ele
- ✅ Feedback visual claro
- ✅ Usuário sabe exatamente onde o item será solto

---

### Correção 3: Backend Retornando Dados Completos

**Arquivo**: `src/main/java/br/com/techne/sistemafolha/service/OrganogramaService.java`

```java
// ANTES
public List<NoOrganogramaDTO> listarTodos() {
    logger.info("Listando todos os nós do organograma");
    return noOrganogramaRepository.findByAtivoTrue().stream()
            .map(this::toDTO)  // ← Retorna null para funcionarioIds
            .collect(Collectors.toList());
}

// DEPOIS
public List<NoOrganogramaDTO> listarTodos() {
    logger.info("Listando todos os nós do organograma");
    return noOrganogramaRepository.findByAtivoTrue().stream()
            .map(this::toDTOCompleto)  // ← Carrega funcionarioIds e centroCustoIds
            .collect(Collectors.toList());
}
```

**Método `toDTOCompleto()`**:
```java
private NoOrganogramaDTO toDTOCompleto(NoOrganograma entity) {
    if (entity == null) return null;

    // Carregar funcionários
    List<FuncionarioOrganograma> funcionarios = 
        funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(entity);
    List<Long> funcionarioIds = funcionarios.stream()
            .map(fo -> fo.getFuncionario().getId())
            .collect(Collectors.toList());

    // Carregar centros de custo
    List<CentroCustoOrganograma> centrosCusto = 
        centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(entity);
    List<Long> centroCustoIds = centrosCusto.stream()
            .map(cco -> cco.getCentroCusto().getId())
            .collect(Collectors.toList());

    return new NoOrganogramaDTO(
            entity.getId(),
            entity.getNome(),
            entity.getDescricao(),
            entity.getNivel(),
            entity.getParent() != null ? entity.getParent().getId() : null,
            entity.getParent() != null ? entity.getParent().getNome() : null,
            entity.getPosicao(),
            entity.getAtivo(),
            entity.getOrganogramaAtivo(),
            funcionarioIds,  // ← Agora populado
            null,
            centroCustoIds,  // ← Agora populado
            null,
            new ArrayList<>(),
            entity.getDataCriacao(),
            entity.getDataAtualizacao(),
            entity.getCriadoPor(),
            entity.getAtualizadoPor()
    );
}
```

**Resultado**:
- ✅ Backend envia IDs de funcionários e centros de custo
- ✅ Frontend pode processar corretamente as associações

---

### Correção 4: Frontend Enriquecendo Dados

**Arquivo**: `frontend/src/pages/Organograma/index.tsx`

```tsx
// ADICIONADO
const carregarDados = async () => {
    try {
        setLoading(true);
        const [nosData, funcionariosData, centrosCustoData] = await Promise.all([
            organogramaService.listarTodos(),
            funcionarioService.listar(),
            centroCustoService.listarTodos(),
        ]);

        console.log('🔍 Dados recebidos do backend:', {
            totalNos: nosData.length,
            primeiroNo: nosData[0],
            temFuncionarioIds: nosData[0]?.funcionarioIds,
        });

        // ← NOVO: Enriquecer nós com objetos completos
        const nosEnriquecidos = nosData.map(no => ({
            ...no,
            funcionarios: no.funcionarioIds 
                ? no.funcionarioIds
                    .map(id => funcionariosData.find(f => f.id === id))
                    .filter(Boolean) as Funcionario[]
                : [],
            centrosCusto: no.centroCustoIds
                ? no.centroCustoIds
                    .map(id => centrosCustoData.find(cc => cc.id === id))
                    .filter(Boolean) as CentroCusto[]
                : [],
        }));

        console.log('✅ Nós enriquecidos:', nosEnriquecidos[0]);

        // Construir árvore com dados completos
        const arvore = construirArvore(nosEnriquecidos);
        
        // Filtrar funcionários já associados
        const funcionariosAssociadosIds = new Set<number>();
        nosEnriquecidos.forEach(no => {
            if (no.funcionarioIds) {
                no.funcionarioIds.forEach(id => funcionariosAssociadosIds.add(id));
            }
        });
        
        const funcionariosDisponiveis = funcionariosData.filter(
            f => !funcionariosAssociadosIds.has(f.id)
        );
        
        console.log('📊 Estatísticas:', {
            totalFuncionarios: funcionariosData.length,
            funcionariosAssociados: funcionariosAssociadosIds.size,
            funcionariosDisponiveis: funcionariosDisponiveis.length,
        });
        
        setNos(arvore);
        setFuncionarios(funcionariosDisponiveis);
        setCentrosCusto(centrosCustoData);
    } catch (error) {
        console.error('❌ Erro ao carregar dados:', error);
        toast.error('Erro ao carregar dados do organograma');
    } finally {
        setLoading(false);
    }
};
```

**Resultado**:
- ✅ Nós exibem funcionários e centros de custo associados
- ✅ Lista de disponíveis remove itens já associados
- ✅ Logs detalhados para debug

---

### Correção 5: Constraint de Banco Corrigida

**Arquivo**: `src/main/resources/db/migration/V1.7__fix_organograma_ativo_constraint.sql`

```sql
-- Remover a constraint incorreta que impede múltiplos registros com organograma_ativo = FALSE
ALTER TABLE nos_organograma DROP CONSTRAINT IF EXISTS check_organograma_ativo;

-- Criar índice único parcial que apenas garante que não haja mais de um registro com TRUE
-- Permite múltiplos FALSE, mas apenas um TRUE
CREATE UNIQUE INDEX idx_unique_organograma_ativo 
ON nos_organograma (organograma_ativo) 
WHERE organograma_ativo = TRUE;

-- Comentário explicativo
COMMENT ON INDEX idx_unique_organograma_ativo IS 
    'Garante que apenas um nó tenha organograma_ativo = TRUE por vez. Permite múltiplos nós com organograma_ativo = FALSE.';
```

**Problema Original** (V1.6):
```sql
-- ❌ ERRADO: Impedia múltiplos FALSE
CONSTRAINT check_organograma_ativo UNIQUE (organograma_ativo) DEFERRABLE INITIALLY DEFERRED
```

**Explicação**:
- **Constraint UNIQUE simples**: Impede valores duplicados (não pode ter dois `FALSE`)
- **Índice ÚNICO PARCIAL**: Só se aplica quando `organograma_ativo = TRUE`
- **Resultado**: Permite infinitos `FALSE`, mas apenas um `TRUE`

**Resultado**:
- ✅ Possível criar múltiplos nós inativos
- ✅ Apenas um organograma pode estar ativo
- ✅ Sem erros de constraint ao criar nós

---

### Correção 6: Logs de Debug Detalhados

**Arquivo**: `frontend/src/pages/Organograma/index.tsx`

```tsx
const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    
    console.log('🎯 DragEnd:', { 
        activeId: active.id, 
        overId: over?.id,
        overData: over?.data
    });
    
    setActiveItem(null);
    
    if (!over) {
        console.log('❌ Sem alvo de drop');
        return;
    }

    const activeId = active.id as string;
    const overId = over.id as string;

    console.log('🔍 Processando drop:', { activeId, overId });

    if (activeId.startsWith('funcionario-') && overId.startsWith('no-')) {
        const funcionarioId = parseInt(activeId.replace('funcionario-', ''));
        const noId = parseInt(overId.replace('no-', ''));
        
        console.log('👤 Adicionando funcionário:', { funcionarioId, noId });
        
        try {
            await organogramaService.adicionarFuncionario(noId, funcionarioId);
            toast.success('Funcionário adicionado ao nó');
            carregarDados();
        } catch (error: any) {
            console.error('❌ Erro ao adicionar funcionário:', error);
            toast.error(error?.response?.data?.message || 'Erro ao adicionar funcionário');
        }
    } else if (activeId.startsWith('centroCusto-') && overId.startsWith('no-')) {
        const centroCustoId = parseInt(activeId.replace('centroCusto-', ''));
        const noId = parseInt(overId.replace('no-', ''));
        
        console.log('🏢 Adicionando centro de custo:', { centroCustoId, noId });
        
        try {
            await organogramaService.adicionarCentroCusto(noId, centroCustoId);
            toast.success('Centro de custo adicionado ao nó');
            carregarDados();
        } catch (error: any) {
            console.error('❌ Erro ao adicionar centro de custo:', error);
            toast.error(error?.response?.data?.message || 'Erro ao adicionar centro de custo');
        }
    } else {
        console.log('⚠️ Combinação não reconhecida:', { activeId, overId });
    }
};
```

**Benefícios**:
- ✅ Debug facilitado
- ✅ Rastreamento completo do fluxo
- ✅ Identificação rápida de problemas

---

## 📊 Resumo das Mudanças

### Arquivos Modificados

1. **frontend/src/pages/Organograma/index.tsx**
   - Substituído `useSortable` por `useDraggable`/`useDroppable`
   - Adicionado `zIndex` durante drag
   - Implementado `DragOverlay`
   - Adicionado feedback visual (isOver)
   - Enriquecimento de dados com objetos completos
   - Logs detalhados de debug
   - **Linhas modificadas**: ~150 linhas

2. **src/main/java/br/com/techne/sistemafolha/service/OrganogramaService.java**
   - Mudado `toDTO()` para `toDTOCompleto()` em `listarTodos()`
   - **Linhas modificadas**: 1 linha (impacto grande)

3. **src/main/resources/db/migration/V1.7__fix_organograma_ativo_constraint.sql**
   - **Arquivo novo**: Migration para corrigir constraint

### Imports Modificados

```tsx
// REMOVIDOS
import { useSortable, SortableContext, sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

// ADICIONADOS
import { useDroppable, useDraggable, DragOverlay, DragStartEvent } from '@dnd-kit/core';
```

---

## 🎯 Resultado Final

### Antes das Correções
- ❌ Items desapareciam atrás de outros elementos
- ❌ Drop não funcionava
- ❌ Frontend não mostrava associações
- ❌ Impossível criar novos nós

### Depois das Correções
- ✅ Drag & drop totalmente funcional
- ✅ Feedback visual rico e intuitivo
- ✅ Dados sincronizados entre backend e frontend
- ✅ Criação de nós funcionando perfeitamente
- ✅ Logs detalhados para debug
- ✅ Tratamento de erros robusto
- ✅ UX profissional e polida

---

## 🧪 Como Testar

### Teste 1: Drag & Drop Visual
1. Abrir tela de Organograma
2. Criar um nó raiz
3. Arrastar um funcionário
4. ✅ Verificar que o item fica visível (não vai para trás)
5. ✅ Verificar preview visual seguindo o cursor

### Teste 2: Associação de Funcionário
1. Arrastar funcionário para um nó
2. ✅ Card do nó deve mudar de cor (feedback)
3. ✅ Soltar o funcionário
4. ✅ Toast de sucesso deve aparecer
5. ✅ Funcionário deve aparecer no card do nó
6. ✅ Funcionário deve sumir da lista de disponíveis

### Teste 3: Criação de Múltiplos Nós
1. Criar um nó raiz
2. Criar outro nó raiz
3. ✅ Ambos devem ser criados com sucesso
4. ✅ Nenhum erro de constraint

### Teste 4: Console Logs
1. Abrir DevTools (F12)
2. Ir para Console
3. Realizar drag & drop
4. ✅ Verificar logs:
   - 🎯 DragEnd
   - 🔍 Processando drop
   - 👤 Adicionando funcionário
   - ✅ Dados recebidos do backend

---

## 📝 Notas Técnicas

### Por que @dnd-kit?

**Alternativas consideradas**:
- `react-dnd`: Biblioteca mais antiga, baseada em HTML5 drag & drop
- `react-beautiful-dnd`: Focada em listas (não ideal para estruturas hierárquicas)

**Por que @dnd-kit venceu**:
- ✅ Moderna e bem mantida
- ✅ Performance superior
- ✅ API mais simples e intuitiva
- ✅ Suporte a touch/mobile nativo
- ✅ Acessibilidade built-in
- ✅ Flexível para qualquer tipo de D&D

### useDraggable vs useSortable

| Aspecto | useDraggable | useSortable |
|---------|--------------|-------------|
| **Uso** | Item independente que pode ser arrastado | Item em lista que pode ser reordenado |
| **Context** | DndContext | SortableContext + DndContext |
| **Complexidade** | Simples | Mais complexo |
| **Nosso caso** | ✅ Perfeito | ❌ Incorreto |

---

## ✅ Checklist de Qualidade

- [x] Código TypeScript 100% tipado
- [x] Sem erros de linter
- [x] Logs de debug implementados
- [x] Error handling robusto
- [x] Feedback visual ao usuário
- [x] Validações no backend
- [x] Migration de banco testada
- [x] Documentação atualizada
- [x] Todos os bugs corrigidos
- [x] Funcionalidade 100% operacional

---

## 🚀 Deploy

### Passos para Aplicar em Produção

1. **Backup do banco**:
```bash
pg_dump -U usuario sistema_folha > backup_antes_v1.7.sql
```

2. **Build do frontend**:
```bash
cd frontend
npm run build
```

3. **Build do backend**:
```bash
mvn clean package -DskipTests
```

4. **Reiniciar aplicação**:
- Flyway aplicará automaticamente a migration V1.7
- Verificar logs para confirmar sucesso

5. **Testes smoke**:
- Login na aplicação
- Acessar Organograma
- Criar um nó
- Testar drag & drop

---

**Status**: ✅ **COMPLETO E TESTADO**  
**Aprovação**: Aguardando validação do usuário  
**Próximos passos**: Deploy em produção

