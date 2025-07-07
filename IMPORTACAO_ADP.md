# Importação de Folha de Pagamento ADP

## Visão Geral

O sistema agora suporta importação de arquivos de folha de pagamento com layout específico do ADP (Automatic Data Processing). Esta funcionalidade está disponível através do menu "Importação" no frontend, na seção "Importação de Folha ADP".

## Formato do Arquivo ADP

O arquivo deve seguir o layout específico do ADP com posições fixas:

### Estrutura do Arquivo

1. **Cabeçalho do Funcionário** (linha com "Admiss")
   - Posições 52-55: Código do funcionário
   - Posições 57-96: Nome do funcionário
   - Posições 96-102: Palavra "Admiss"
   - Posições 0-3: Código do centro de custo
   - Posições 4-50: Nome do centro de custo

2. **Rubricas** (linhas com códigos de 4 dígitos)
   - Posições 0-31: Primeira rubrica
   - Posições 47-64: Valores da primeira rubrica
   - Posições 66-97: Segunda rubrica (se existir)
   - Posições 113-130: Valores da segunda rubrica

3. **Total de Pagamentos** (linha com "Tot.Pagamentos:")
   - Indica o fim do processamento de um funcionário

4. **Salário Base** (linha com "Sal")
   - Posições 65-75: Valor do salário base

## Como Usar

### 1. Acessar a Página de Importação
1. Faça login no sistema
2. No menu lateral, clique em "Importação"
3. Você verá três seções: "Importação de Folha de Pagamento", "Importação de Folha ADP" e "Importação de Benefícios"

### 2. Importar Arquivo ADP
1. Na seção "Importação de Folha ADP":
   - Clique em "Selecionar Arquivo (.txt)"
   - Escolha o arquivo de folha ADP (.txt)
   - Clique em "Importar Folha ADP"

### 3. Acompanhar o Processo
- Durante a importação, você verá um indicador de progresso
- Após a conclusão, serão exibidas as informações:
  - Nome do arquivo
  - Tamanho do arquivo
  - Número de registros processados
  - Erros encontrados (se houver)

## Processamento do Arquivo

O sistema processa o arquivo ADP da seguinte forma:

### 1. Leitura com Codificação Específica
- O arquivo é lido com codificação WINDOWS-1252
- Suporte a caracteres especiais do português

### 2. Processamento por Posições Fixas
- Identificação de funcionários por padrão "Admiss"
- Extração de rubricas por posições específicas
- Montagem de cabeçalhos formatados

### 3. Substituições Automáticas
- "VENDAS E PRE VENDAS-CRONA" → "VENDAS E PRE VENDAS-CRONAPP"
- Rubricas ignoradas são marcadas com "--"

### 4. Mapeamento de Centros de Custo
- Código 258: "Filial    0065  TECHNE - EDUCACAO"
- Código 149: "Filial    0065  TECHNE - EDUCACAO"
- Código 245: "Filial    0065  TECHNE - EDUCACAO"

## Exemplo de Arquivo ADP

```
258             SERVICOS - EDU                      273  RENATO AMANCIO DA SILVA                Admissão: 01/01/2011
         Sexo: M      Tipo de Salário: M (220,00 Hrs)   Salário: 132.505,40               Função: ANALISTA DE ERP SENIOR
     Dep.IRRF:  2         Dep.Sal.Fam:  0               Vínculo: Trabalhador CLT

0010 Salário Base           200,00         0,00        13.250,54+ 5560 INSS                    14,00         0,00           876,95-
3027 Ajuda de Custo Tele      0,00         0,00            80,00+ 5610 Vale Refeição            0,00         0,00             1,00-
3524 Assi Medic Dep-GNDI      0,00         0,00         1.326,56- 9920 FGTS                     8,00         0,00         1.060,04
5500 IR Retido                4,00         0,00         2.413,50-
Tot.Pagamentos: 13.330,54          Tot.Descontos: 4.618,01           Líquido: 8.712,53
```

## Pré-requisitos

### Funcionários
Antes de importar a folha ADP, os funcionários devem estar cadastrados no sistema com:
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
Se um funcionário não estiver cadastrado no sistema, a importação continuará mas usará o nome do arquivo.

### 2. Formato de Arquivo Inválido
- Apenas arquivos .txt são aceitos
- Arquivos vazios são rejeitados

### 3. Erros de Parsing
- Valores numéricos inválidos
- Linhas mal formatadas
- Codificação incorreta

## Logs e Auditoria

O sistema registra logs detalhados da importação:
- Número de registros processados
- Número de funcionários encontrados
- Número de rubricas criadas
- Erros específicos por linha

## Diferências da Importação ADP

### Comparação com Importação Padrão

| Aspecto | Importação Padrão | Importação ADP |
|---------|-------------------|----------------|
| **Layout** | Posições fixas simples | Layout específico ADP |
| **Codificação** | UTF-8 | WINDOWS-1252 |
| **Processamento** | Regex patterns | Posições fixas |
| **Funcionários** | Busca por ID externo | Busca por nome |
| **Rubricas** | Extração direta | Processamento em duas colunas |
| **Substituições** | Nenhuma | Automáticas |

### Vantagens da Importação ADP

1. **Compatibilidade**: Suporte completo ao formato ADP
2. **Codificação**: Suporte a caracteres especiais
3. **Flexibilidade**: Processamento de múltiplas rubricas por linha
4. **Automação**: Substituições automáticas
5. **Mapeamento**: Centros de custo pré-configurados

## Suporte

Em caso de problemas:
1. Verifique os logs do sistema
2. Confirme se o formato do arquivo está correto
3. Verifique se os funcionários estão cadastrados
4. Entre em contato com o suporte técnico

## API

### Endpoint
```
POST /api/importacao/folha-adp
```

### Parâmetros
- `arquivo`: MultipartFile (.txt)

### Resposta
```json
{
  "success": true,
  "message": "Arquivo ADP importado com sucesso",
  "arquivo": "folha-adp.txt",
  "tamanho": 1024
}
```

### Códigos de Erro
- `400`: Arquivo vazio ou formato inválido
- `500`: Erro interno do servidor 