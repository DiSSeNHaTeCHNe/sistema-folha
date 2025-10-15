# 📊 Endpoints do Organograma

Esta documentação descreve todos os endpoints disponíveis para gerenciamento do organograma da empresa.

## 🔐 Autenticação

Todos os endpoints requerem autenticação JWT. Use o endpoint de login para obter o token:

```
POST {{baseUrl}}/auth/login
{
    "login": "admin",
    "senha": "admin"
}
```

## 📋 Operações Básicas

### Listar Todos os Nós
```
GET {{baseUrl}}/api/organograma
```
Lista todos os nós do organograma ativos.

### Buscar Nó por ID
```
GET {{baseUrl}}/api/organograma/{id}
```
Busca um nó específico pelo ID.

### Criar Nó
```
POST {{baseUrl}}/api/organograma
{
    "nome": "Diretoria Executiva",
    "descricao": "Diretoria responsável pela gestão executiva da empresa",
    "parentId": null,
    "posicao": null
}
```
Cria um novo nó do organograma. `parentId` e `posicao` são opcionais.

### Atualizar Nó
```
PUT {{baseUrl}}/api/organograma/{id}
{
    "nome": "Diretoria Executiva Atualizada",
    "descricao": "Descrição atualizada",
    "parentId": null,
    "posicao": 0
}
```
Atualiza um nó existente.

### Remover Nó
```
DELETE {{baseUrl}}/api/organograma/{id}
```
Remove um nó (apenas se não tiver filhos).

### Remover Nó com Filhos
```
DELETE {{baseUrl}}/api/organograma/{id}/cascata
```
Remove um nó e todos os seus filhos recursivamente.

## 🌳 Operações Hierárquicas

### Obter Árvore Completa
```
GET {{baseUrl}}/api/organograma/arvore
```
Obtém a árvore completa do organograma ativo.

### Obter Filhos de um Nó
```
GET {{baseUrl}}/api/organograma/filhos?parentId={id}
```
Obtém os nós filhos de um nó pai específico.

### Mover Nó
```
PUT {{baseUrl}}/api/organograma/{id}/mover?novoParentId={id}&novaPosicao={posicao}
```
Move um nó para outra posição na hierarquia.

## 🎯 Gestão do Organograma Ativo

### Obter Organograma Ativo
```
GET {{baseUrl}}/api/organograma/ativo
```
Obtém o organograma atualmente ativo.

### Ativar Organograma
```
PUT {{baseUrl}}/api/organograma/{id}/ativar
```
Ativa um organograma (nó raiz). Apenas um organograma pode estar ativo por vez.

### Desativar Organograma
```
PUT {{baseUrl}}/api/organograma/desativar
```
Desativa o organograma atual.

## 👥 Associações com Funcionários

### Associar Funcionário
```
POST {{baseUrl}}/api/organograma/{noId}/funcionarios/{funcionarioId}
```
Associa um funcionário a um nó do organograma.

### Desassociar Funcionário
```
DELETE {{baseUrl}}/api/organograma/{noId}/funcionarios/{funcionarioId}
```
Desassocia um funcionário de um nó do organograma.

### Listar Funcionários do Nó
```
GET {{baseUrl}}/api/organograma/{noId}/funcionarios
```
Lista funcionários associados a um nó.

## 💰 Associações com Centros de Custo

### Associar Centro de Custo
```
POST {{baseUrl}}/api/organograma/{noId}/centros-custo/{centroCustoId}
```
Associa um centro de custo a um nó do organograma.

### Desassociar Centro de Custo
```
DELETE {{baseUrl}}/api/organograma/{noId}/centros-custo/{centroCustoId}
```
Desassocia um centro de custo de um nó do organograma.

### Listar Centros de Custo do Nó
```
GET {{baseUrl}}/api/organograma/{noId}/centros-custo
```
Lista centros de custo associados a um nó.

## 📝 Exemplos de Uso

### 1. Criar Estrutura Hierárquica

```bash
# 1. Criar nó raiz
POST {{baseUrl}}/api/organograma
{
    "nome": "Presidência",
    "descricao": "Presidência da empresa"
}

# 2. Criar diretorias
POST {{baseUrl}}/api/organograma
{
    "nome": "Diretoria Financeira",
    "descricao": "Diretoria responsável pelas finanças",
    "parentId": 1
}

POST {{baseUrl}}/api/organograma
{
    "nome": "Diretoria de TI",
    "descricao": "Diretoria responsável pela tecnologia",
    "parentId": 1
}

# 3. Criar gerências
POST {{baseUrl}}/api/organograma
{
    "nome": "Gerência de Contabilidade",
    "descricao": "Gerência responsável pela contabilidade",
    "parentId": 2
}
```

### 2. Ativar Organograma

```bash
# Ativar o organograma com a presidência como raiz
PUT {{baseUrl}}/api/organograma/1/ativar
```

### 3. Associar Funcionários

```bash
# Associar funcionário à diretoria financeira
POST {{baseUrl}}/api/organograma/2/funcionarios/1

# Associar funcionário à gerência de contabilidade
POST {{baseUrl}}/api/organograma/4/funcionarios/2
```

### 4. Associar Centros de Custo

```bash
# Associar centro de custo à diretoria financeira
POST {{baseUrl}}/api/organograma/2/centros-custo/1

# Associar centro de custo à gerência de contabilidade
POST {{baseUrl}}/api/organograma/4/centros-custo/2
```

## ⚠️ Regras de Negócio

1. **Organograma Ativo**: Apenas um organograma pode estar ativo por vez
2. **Hierarquia**: Nós filhos herdam o nível do pai + 1
3. **Posição**: Calculada automaticamente se não fornecida
4. **Remoção**: Nós com filhos só podem ser removidos com operação em cascata
5. **Associações**: Um funcionário pode estar associado a apenas um nó
6. **Centros de Custo**: Um centro de custo pode estar associado a múltiplos nós

## 🔧 Códigos de Resposta

- `200 OK`: Operação realizada com sucesso
- `201 Created`: Recurso criado com sucesso
- `204 No Content`: Recurso removido com sucesso
- `400 Bad Request`: Dados inválidos ou regra de negócio violada
- `404 Not Found`: Recurso não encontrado
- `409 Conflict`: Conflito (ex: tentativa de ativar organograma quando já existe um ativo)
- `500 Internal Server Error`: Erro interno do servidor 