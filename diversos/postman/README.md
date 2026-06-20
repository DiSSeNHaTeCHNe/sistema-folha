# Collections do Postman - Sistema de Folha de Pagamento

Este diretório contém as collections e ambiente do Postman para testar a API do Sistema de Folha de Pagamento.

## Arquivos

- `sistema-folha.postman_collection.json` - Collection principal com todos os endpoints da API
- `sistema-folha.postman_environment.json` - Ambiente com variáveis configuradas
- `README.md` - Este arquivo de documentação

## Como Importar

### 1. Importar a Collection
1. Abra o Postman
2. Clique em "Import"
3. Selecione o arquivo `sistema-folha.postman_collection.json`
4. Clique em "Import"

### 2. Importar o Ambiente
1. No Postman, clique em "Import"
2. Selecione o arquivo `sistema-folha.postman_environment.json`
3. Clique em "Import"
4. Selecione o ambiente "Sistema Folha de Pagamento" no dropdown superior direito

## Configuração Inicial

### 1. Login
1. Execute a requisição "Login" na pasta "Autenticação"
2. O token JWT será automaticamente configurado no ambiente
3. Todas as outras requisições usarão este token automaticamente

### 2. Verificar Configuração
- Base URL: `http://localhost:8083`
- Token: Será preenchido automaticamente após o login
- Outras variáveis: Configuradas com valores padrão

## Estrutura da Collection

### Autenticação
- **Login**: Realiza login e obtém token JWT

### Usuários
- **Listar Usuários**: GET `/api/usuarios`
- **Buscar por ID**: GET `/api/usuarios/{id}`
- **Buscar por Login**: GET `/api/usuarios/login/{login}`
- **Cadastrar**: POST `/api/usuarios`
- **Atualizar**: PUT `/api/usuarios/{id}`
- **Remover**: DELETE `/api/usuarios/{id}`
- **Alterar Senha**: POST `/api/usuarios/{id}/alterar-senha`

### Funcionários
- **Listar**: GET `/funcionarios`
- **Buscar por ID**: GET `/funcionarios/{id}`
- **Cadastrar**: POST `/funcionarios`
- **Atualizar**: PUT `/funcionarios/{id}`
- **Remover**: DELETE `/funcionarios/{id}`

### Cargos
- **Listar**: GET `/cargos`
- **Buscar por ID**: GET `/cargos/{id}`
- **Cadastrar**: POST `/cargos`
- **Atualizar**: PUT `/cargos/{id}`
- **Remover**: DELETE `/cargos/{id}`

### Centros de Custo
- **Listar**: GET `/centros-custo`
- **Buscar por ID**: GET `/centros-custo/{id}`
- **Cadastrar**: POST `/centros-custo`
- **Atualizar**: PUT `/centros-custo/{id}`
- **Remover**: DELETE `/centros-custo/{id}`

### Linhas de Negócio
- **Listar**: GET `/linhas-negocio`
- **Buscar por ID**: GET `/linhas-negocio/{id}`
- **Cadastrar**: POST `/linhas-negocio`
- **Atualizar**: PUT `/linhas-negocio/{id}`
- **Remover**: DELETE `/linhas-negocio/{id}`

### Rubricas
- **Listar**: GET `/rubricas`
- **Buscar por ID**: GET `/rubricas/{id}`
- **Cadastrar**: POST `/rubricas`
- **Atualizar**: PUT `/rubricas/{id}`
- **Remover**: DELETE `/rubricas/{id}`

### Folha de Pagamento
- **Listar**: GET `/folha-pagamento`
- **Consultar por Funcionário**: GET `/folha-pagamento/funcionario/{id}`
- **Consultar por Centro de Custo**: GET `/folha-pagamento/centro-custo/{id}`
- **Consultar por Período**: GET `/folha-pagamento`
- **Remover Registro**: DELETE `/folha-pagamento/{id}`

### Benefícios
- **Consultar por Funcionário**: GET `/beneficios/funcionario/{id}`
- **Consultar por Centro de Custo**: GET `/beneficios/centro-custo/{id}`
- **Remover**: DELETE `/beneficios/{id}`

### Resumo Folha de Pagamento
- **Listar Todos**: GET `/resumo-folha-pagamento`
- **Consultar por Período**: GET `/resumo-folha-pagamento/periodo`
- **Consultar por Competência**: GET `/resumo-folha-pagamento/competencia`
- **Listar Mais Recentes**: GET `/resumo-folha-pagamento/latest`

### Importação
- **Importar Folha**: POST `/importacao/folha`
- **Importar Folha ADP**: POST `/importacao/folha-adp`
- **Importar Benefícios**: POST `/importacao/beneficios`

## Variáveis de Ambiente

### Configuração
- `baseUrl`: URL base da API (padrão: http://localhost:8083)
- `token`: Token JWT (preenchido automaticamente após login)

### IDs de Referência
- `userId`: ID do usuário para testes
- `funcionarioId`: ID do funcionário para testes
- `cargoId`: ID do cargo para testes
- `centroCustoId`: ID do centro de custo para testes
- `linhaNegocioId`: ID da linha de negócio para testes
- `rubricaId`: ID da rubrica para testes

### Datas
- `dataInicio`: Data de início para consultas (padrão: 2024-01-01)
- `dataFim`: Data de fim para consultas (padrão: 2024-01-31)
- `dataReferencia`: Data de referência para consultas (padrão: 2024-01-15)

## Como Usar

### 1. Sequência de Testes Recomendada
1. **Login** - Obter token de autenticação
2. **Cadastrar Cargo** - Criar um cargo para funcionários
3. **Cadastrar Centro de Custo** - Criar um centro de custo
4. **Cadastrar Linha de Negócio** - Criar uma linha de negócio
5. **Cadastrar Rubrica** - Criar uma rubrica
6. **Cadastrar Funcionário** - Criar um funcionário
7. **Testar Consultas** - Usar as consultas de folha e benefícios
8. **Testar Importações** - Importar arquivos de dados

### 2. Dados de Exemplo
As requisições já contêm dados de exemplo que podem ser usados para testes. Você pode modificar esses dados conforme necessário.

### 3. Parâmetros de Consulta
Para consultas que requerem datas, você pode:
- Usar as variáveis de ambiente: `{{dataInicio}}`, `{{dataFim}}`, `{{dataReferencia}}`
- Ou inserir datas diretamente no formato ISO: `2024-01-01`

### 4. Upload de Arquivos
Para as requisições de importação:
1. Clique em "Select Files" na seção Body
2. Escolha um arquivo .txt válido
3. Execute a requisição

## Troubleshooting

### Problemas Comuns

1. **Erro 401 Unauthorized**
   - Execute novamente a requisição de Login
   - Verifique se o token foi configurado corretamente

2. **Erro 404 Not Found**
   - Verifique se o servidor está rodando na porta 8083
   - Confirme se a URL base está correta

3. **Erro 400 Bad Request**
   - Verifique se os dados enviados estão no formato correto
   - Confirme se todos os campos obrigatórios estão preenchidos

4. **Erro 500 Internal Server Error**
   - Verifique os logs do servidor
   - Confirme se o banco de dados está acessível

### Logs do Servidor
Para verificar se o servidor está funcionando:
```bash
# Verificar se a aplicação está rodando
curl http://localhost:8083/actuator/health

# Verificar logs
tail -f logs/application.log
```

## Próximos Passos

1. **Criar Dados de Teste**: Use as requisições de cadastro para criar dados de teste
2. **Testar Consultas**: Execute as consultas para verificar se os dados estão sendo retornados
3. **Testar Importações**: Use arquivos de exemplo para testar as importações
4. **Personalizar**: Modifique as requisições conforme suas necessidades específicas

## Suporte

Para dúvidas ou problemas:
1. Verifique a documentação da API (Swagger) em: `http://localhost:8083/swagger-ui.html`
2. Consulte os logs da aplicação
3. Verifique se todas as dependências estão configuradas corretamente 