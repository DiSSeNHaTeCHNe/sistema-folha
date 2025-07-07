# Importação de Folha de Pagamento

## Visão Geral

O sistema de folha de pagamento permite importar dados de folha através de arquivos de texto (.txt) com layout de posições fixas. Esta funcionalidade está disponível através do menu "Importação" no frontend.

## Formato do Arquivo

O arquivo deve seguir o formato específico do sistema de folha de pagamento, contendo as seguintes seções:

### 1. Cabeçalho
```
TECHNE ENGENHARIA E SISTEMAS LTDA                                      Folha de Pagamento                                     Página
50.737.766/0001-21                                                                                                                 1
------------------------------------------------------------------------------------------------------------------------------------
Seleção Geral:                                                                            Referente: MENSAL
                                                                                        Competência:        01/10/2023 a 31/10/2023
```

### 2. Período de Competência
A linha deve conter o período de competência no formato:
```
Competência:        01/10/2023 a 31/10/2023
```

### 3. Dados do Funcionário
Cada funcionário é identificado por uma linha no formato:
```
258             SERVICOS - EDU                      273  RENATO AMANCIO DA SILVA
```

Onde:
- `258` = Código do centro de custo
- `SERVICOS - EDU` = Nome do centro de custo
- `273` = ID externo do funcionário
- `RENATO AMANCIO DA SILVA` = Nome do funcionário

### 4. Rubricas
As rubricas são identificadas por linhas no formato:
```
0010 Salário Base           200,00         0,00        13.250,54+
5560 INSS                    14,00         0,00           876,95-
```

Onde:
- `0010` = Código da rubrica
- `Salário Base` = Descrição da rubrica
- `200,00` = Quantidade
- `0,00` = Base de cálculo
- `13.250,54` = Valor
- `+` = Tipo (provento) ou `-` = Tipo (desconto)

## Como Usar

### 1. Acessar a Página de Importação
1. Faça login no sistema
2. No menu lateral, clique em "Importação"
3. Você verá duas seções: "Importação de Folha de Pagamento" e "Importação de Benefícios"

### 2. Importar Arquivo de Folha
1. Na seção "Importação de Folha de Pagamento":
   - Clique em "Selecionar Arquivo (.txt)"
   - Escolha o arquivo de folha de pagamento (.txt)
   - Clique em "Importar Folha"

### 3. Acompanhar o Processo
- Durante a importação, você verá um indicador de progresso
- Após a conclusão, serão exibidas as informações:
  - Nome do arquivo
  - Tamanho do arquivo
  - Número de registros processados
  - Erros encontrados (se houver)

## Pré-requisitos

### Funcionários
Antes de importar a folha, os funcionários devem estar cadastrados no sistema com:
- ID externo correspondente ao arquivo
- Nome completo
- Cargo
- Centro de custo

### Rubricas
As rubricas serão criadas automaticamente durante a importação, mas você pode pré-cadastrar as mais comuns:
- Código da rubrica
- Descrição
- Tipo (Provento/Desconto/Informativo)

## Tratamento de Erros

O sistema trata os seguintes tipos de erro:

### 1. Funcionário Não Encontrado
Se um funcionário não estiver cadastrado no sistema, a importação será interrompida com a mensagem:
```
Funcionário não encontrado: [ID] - [Nome]
```

### 2. Formato de Arquivo Inválido
- Apenas arquivos .txt são aceitos
- Arquivos vazios são rejeitados

### 3. Erros de Parsing
- Valores numéricos inválidos
- Datas em formato incorreto
- Linhas mal formatadas

## Logs e Auditoria

O sistema registra logs detalhados da importação:
- Número de registros processados
- Número de funcionários encontrados
- Número de rubricas criadas
- Erros específicos por linha

## Exemplo de Arquivo

Veja o arquivo `exemplo-folha-pagamento.txt` para um exemplo completo do formato esperado.

## Dicas Importantes

1. **Backup**: Sempre faça backup dos dados antes de importar
2. **Teste**: Teste com um arquivo pequeno antes de importar arquivos grandes
3. **Validação**: Verifique se os funcionários estão cadastrados antes da importação
4. **Formato**: Certifique-se de que o arquivo está no formato correto
5. **Codificação**: Use codificação UTF-8 para caracteres especiais

## Suporte

Em caso de problemas:
1. Verifique os logs do sistema
2. Confirme se o formato do arquivo está correto
3. Verifique se os funcionários estão cadastrados
4. Entre em contato com o suporte técnico 