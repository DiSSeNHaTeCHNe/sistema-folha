# Sistema de Controle de Folha de Pagamento

## Descrição
Sistema completo para controle de folha de pagamento, desenvolvido com Spring Boot no backend e React no frontend.

## 📋 Changelog

### Versão 1.1 (17/10/2025)
✅ **Nova Funcionalidade - Visualização em Gráfico do Organograma**
- Adicionado modo de visualização tipo mapa mental com zoom/pan
- Toggle para alternar entre modo Lista (tradicional) e Gráfico (novo)
- Biblioteca ReactFlow integrada para navegação intuitiva
- MiniMap para overview da estrutura completa
- Layout automático hierárquico com centralização inteligente
- Todas funcionalidades de edição mantidas no modo gráfico
- Performance otimizada para hierarquias complexas
- 📄 Documentação detalhada: `relatorios/MELHORIA_VISUALIZACAO_GRAFICO_ORGANOGRAMA.md`

**Dependências adicionadas:**
- `reactflow: ^11.x` - Visualização de grafos interativos

**Arquivos modificados:**
- `frontend/src/pages/Organograma/index.tsx` - Integração do toggle
- `frontend/src/components/OrganogramaGrafico/index.tsx` - Novo componente
- `frontend/package.json` - Nova dependência

## Tecnologias Utilizadas

### Backend
- Java 17
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway (migrações)
- JWT (autenticação)
- OpenAPI/Swagger
- Lombok
- Maven

### Frontend
- React 18
- TypeScript
- Material-UI v7
- ReactFlow (visualização de grafos)
- Vite
- Axios
- React Router
- React Hook Form
- @dnd-kit (drag & drop)
- ESLint
- Prettier

## Funcionalidades Implementadas

### 1. Autenticação e Autorização
- Login com JWT
- Controle de permissões
- Refresh token
- Proteção de rotas

### 2. Gestão de Usuários
- CRUD completo de usuários
- Controle de permissões
- Soft delete
- Validações de dados

### 3. Gestão de Funcionários
- CRUD completo de funcionários
- Campos específicos:
  - Nome
  - Cargo
  - Centro de Custo
  - Linha de Negócio
  - ID Externo
  - Data de Admissão
  - Sexo
  - Tipo de Salário
  - Função
  - Dependentes (IRRF e Salário Família)
  - Vínculo
- Soft delete
- Validações de dados

### 4. Folha de Pagamento
- CRUD completo de registros
- Campos:
  - Funcionário
  - Rubrica
  - Data Início/Fim
  - Valor
  - Quantidade
  - Base de Cálculo
- Soft delete
- Validações de dados

### 5. Benefícios
- CRUD completo de benefícios
- Campos:
  - Funcionário
  - Descrição
  - Valor
  - Data Início/Fim
  - Observação
- Soft delete
- Validações de dados

### 6. Relatórios
- Geração de relatórios em PDF
- Tipos de relatórios:
  - Folha de Pagamento
  - Benefícios
- Campos dos relatórios:
  - Mês/Ano
  - Total de Funcionários
  - Total da Folha
  - Total de Benefícios
  - Status
  - Data de Processamento
- Download de relatórios

### 7. Interface do Usuário
- Design responsivo com Material-UI
- Componentes reutilizáveis:
  - Formulários
  - Tabelas
  - Notificações
  - Diálogos de confirmação
  - Campos de data
  - Campos monetários
  - Campos de seleção
  - Campos de texto
  - Campos numéricos
  - Campos de área de texto
  - Checkboxes
  - Radio buttons
  - Switches
  - Autocomplete
  - Campos de data/hora
  - Campos de hora

### 8. Testes
- Testes unitários
- Testes de integração
- Testes de API com Postman
- Script de teste automatizado

### 9. Documentação
- API documentada com Swagger/OpenAPI
- Documentação de código
- Coleção Postman para testes

### 10. Segurança
- Autenticação JWT
- Controle de permissões
- Validação de dados
- Proteção contra CSRF
- Headers de segurança
- Sanitização de inputs

### 11. Banco de Dados
- PostgreSQL
- Migrações com Flyway
- Soft delete em todas as entidades
- Índices para performance
- Relacionamentos otimizados

### 12. DevOps
- Scripts de build
- Scripts de teste
- Configuração de ambiente
- Documentação de deploy

## Estrutura do Projeto

```text
sistema-folha/
├── frontend/              # SPA React
├── backend/               # API Spring Boot (pom.xml + src/)
├── diversos/              # Scripts, Postman, utilitários, relatórios
│   ├── scripts/
│   ├── postman/
│   ├── bcrypt-generator/
│   ├── db/
│   └── relatorios/
├── _docs/                 # PRD, specs, documentação canônica
├── docker-compose.yml
└── Dockerfile
```

### Backend (`backend/`)

```text
backend/
├── pom.xml
└── src/
    ├── main/java/br/com/techne/sistemafolha/
    │   ├── config/
    │   ├── controller/
    │   ├── dto/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── SistemaFolhaApplication.java
    └── main/resources/
        ├── db/migration/
        └── application.yml
```

### Frontend (`frontend/`)

```text
frontend/
├── src/
│   ├── components/
│   ├── contexts/
│   ├── hooks/
│   ├── pages/
│   ├── services/
│   ├── types/
│   └── App.tsx
├── public/
└── package.json
```

## Como Executar

### Pré-requisitos
- JDK 17
- Maven 3.8+
- Node.js 18+
- PostgreSQL 12+
- NPM ou Yarn

### Backend
1. Configure o banco de dados em `backend/src/main/resources/application.yml`:
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/sistema_folha
spring.datasource.username: seu_usuario
spring.datasource.password: sua_senha
```

2. Execute as migrações:
```bash
cd backend
mvn flyway:migrate
```

3. Inicie o servidor:
```bash
cd backend
mvn spring-boot:run
```

### Frontend
1. Instale as dependências:
```bash
cd frontend
npm install
```

2. Inicie o servidor de desenvolvimento:
```bash
cd frontend
npm run dev
```

## Testes

### API
1. Importe a coleção do Postman:
```
diversos/postman/sistema-folha.postman_collection.json
```

2. Importe o ambiente:
```
diversos/postman/sistema-folha.postman_environment.json
```

3. Execute os testes:
```bash
./diversos/scripts/test-api.sh
```

4. Consulte a documentação das collections:
```
diversos/postman/README.md
```

## Próximos Passos
1. Implementar testes unitários no frontend
2. Adicionar mais relatórios
3. Implementar dashboard com gráficos
4. Adicionar sistema de notificações em tempo real
5. Implementar histórico de alterações
6. Adicionar backup automático
7. Implementar relatórios personalizados
8. Melhorar performance com lazy loading
9. Otimizar carregamento de imagens
10. Implementar cache
11. Otimizar queries do banco
12. Implementar compressão de dados
13. Configurar CI/CD
14. Adicionar Docker
15. Configurar ambientes (dev, staging, prod)
16. Implementar monitoramento e logging 